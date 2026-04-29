package com.mes.framework.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 生产环境弱口令巡检器（P0-06 安全整改）。
 *
 * <p>问题背景：</p>
 * <ul>
 *   <li>{@code sql/R__seed_test_data.sql} 与 {@code V1.11__auth_rbac.sql} 在 sys_user 表里
 *       写入了 5 个共享 BCrypt(admin123) 密文的账号（admin + 4 个测试账号）。</li>
 *   <li>若客户安装时未换密即上线，等于给平台开了公网弱口令爆破入口，触发等保三级红线。</li>
 * </ul>
 *
 * <p>本组件在 prod profile 启动时：</p>
 * <ol>
 *   <li>扫描 sys_user 全表；</li>
 *   <li>先用精确 hash 匹配（{@link #WEAK_PASSWORD_HASHES}）O(n) 快速定位命中；</li>
 *   <li>若开关 {@code mes.security.weak-pwd-audit.deep-scan=true}，还会用 {@link PasswordEncoder#matches(CharSequence, String)}
 *       对其余 {@code $2a$...} 风格的密文做兜底 BCrypt 比对，命中同样算弱口令；</li>
 *   <li>命中账号 UPDATE {@code sys_user.must_change_password=1}，登录响应会回带 mustChangePwd=true，
 *       前端弹出强制改密对话框。</li>
 * </ol>
 *
 * <p>容错策略：任何 SQL 异常（含 V1.20 未执行导致的 must_change_password 列缺失）
 * 都只打 WARN 不抛出，避免审计逻辑阻塞应用启动。</p>
 *
 * @author mcp24
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class WeakPasswordAuditor {

    /**
     * 已知的 BCrypt(admin123) 密文常量列表。
     *
     * <p>来源：</p>
     * <ul>
     *   <li>{@code sql/V1.11__auth_rbac.sql} 里 admin 账号的初始 hash；</li>
     *   <li>{@code sql/R__seed_test_data.sql} 里 4 个测试账号共用的 hash（与 admin 一致）。</li>
     * </ul>
     *
     * <p>若后续发现新的 BCrypt(admin123) 变体，追加到此处即可；强烈建议**不要**直接
     * 把明文 "admin123" 写入代码。</p>
     */
    private static final Set<String> WEAK_PASSWORD_HASHES = Set.of(
            "$2a$10$e2nbvCXt4JOvHpdJAqvIweP8fRNID1OUSVBmbxg4PLiVGdKonzRXy"
    );

    /**
     * 另外列一个已知明文白名单，仅在 deepScan=true 时启用 BCrypt 逐行比对。
     *
     * <p>⚠️ 这里列出弱口令明文是为了在运行时让 passwordEncoder 做 matches 对比；
     * 常量仅本类可见，不对外暴露。</p>
     */
    private static final List<String> KNOWN_WEAK_PLAINTEXTS = List.of(
            "admin123", "123456", "12345678", "password", "admin", "admin888"
    );

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    /**
     * 是否启用深度 BCrypt 扫描。
     *
     * <p>关闭（默认）：O(n) 扫 hash 常量，毫秒级完成；
     * 开启：O(n × m) 对每条记录调 passwordEncoder.matches()，单条 ~30ms，大表会显著拖慢启动，
     * 仅在确认有非标 hash 时开启。可用 {@code mes.security.weak-pwd-audit.deep-scan=true} 显式开启。</p>
     */
    @Value("${mes.security.weak-pwd-audit.deep-scan:false}")
    private boolean deepScan;

    /**
     * 是否启用该审计。关闭时整体跳过（适合演练环境临时关闭）。
     */
    @Value("${mes.security.weak-pwd-audit.enabled:true}")
    private boolean enabled;

    /**
     * 应用启动后立即执行一次弱口令巡检。
     *
     * <p>非致命——任何异常都只写 WARN 日志、不抛出，保证应用继续启动。</p>
     */
    @PostConstruct
    public void auditOnStartup() {
        if (!enabled) {
            log.info("[P0-06] 弱口令巡检已通过 mes.security.weak-pwd-audit.enabled=false 关闭");
            return;
        }
        try {
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                    "SELECT id, username, password FROM sys_user WHERE deleted = 0");
            if (users.isEmpty()) {
                log.info("[P0-06] sys_user 表为空，弱口令巡检跳过");
                return;
            }

            List<Long> weakUserIds = new ArrayList<>();
            List<String> weakUsernames = new ArrayList<>();

            for (Map<String, Object> row : users) {
                Long id = ((Number) row.get("id")).longValue();
                String username = asString(row.get("username"));
                String hash = asString(row.get("password"));
                if (hash == null || hash.isBlank()) {
                    continue;
                }
                if (isWeak(hash)) {
                    weakUserIds.add(id);
                    weakUsernames.add(username);
                }
            }

            if (weakUserIds.isEmpty()) {
                log.info("[P0-06] 弱口令巡检完成：扫描 {} 个账号，未发现弱口令", users.size());
                return;
            }

            markMustChangePassword(weakUserIds);
            log.warn("[P0-06] 弱口令巡检命中 {} 个账号，已强制 must_change_password=1："
                            + " usernames={} userIds={}；登录后必须先通过 /system/user/change-my-password 改密",
                    weakUserIds.size(), weakUsernames, weakUserIds);
        } catch (Exception e) {
            log.warn("[P0-06] 弱口令巡检执行失败（忽略，不阻塞启动）: {}", e.getMessage());
        }
    }

    /**
     * 判定某个 BCrypt 密文是否为已知弱口令。
     *
     * @param hash sys_user.password 原文 hash
     * @return true=命中弱口令
     */
    private boolean isWeak(String hash) {
        if (WEAK_PASSWORD_HASHES.contains(hash)) {
            return true;
        }
        if (!deepScan) {
            return false;
        }
        // 只对 $2a/$2b/$2y 格式的 BCrypt 密文做深度比对；其它 format 跳过，避免浪费 CPU
        if (!(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"))) {
            return false;
        }
        for (String plain : KNOWN_WEAK_PLAINTEXTS) {
            try {
                if (passwordEncoder.matches(plain, hash)) {
                    return true;
                }
            } catch (Exception ignore) {
                // 个别密文格式非法时 matches 可能抛异常，忽略继续
            }
        }
        return false;
    }

    /**
     * 批量把弱口令账号置为 must_change_password=1。
     *
     * <p>采用逐条 UPDATE 而非批量的原因：</p>
     * <ul>
     *   <li>弱口令账号通常很少（一般 &lt; 10 个），批量意义不大；</li>
     *   <li>逐条 UPDATE 便于单条失败时隔离并继续（例如 V1.20 未执行时字段缺失）。</li>
     * </ul>
     *
     * @param userIds 命中弱口令的 user id 列表
     */
    private void markMustChangePassword(List<Long> userIds) {
        String sql = "UPDATE sys_user SET must_change_password = 1 WHERE id = ?";
        int updated = 0;
        for (Long id : userIds) {
            try {
                updated += jdbcTemplate.update(sql, id);
            } catch (Exception e) {
                log.warn("[P0-06] 更新 must_change_password 失败（可能 V1.20 未执行）: userId={}, err={}",
                        id, e.getMessage());
                // 单条失败就不再尝试剩余账号——必定是 schema 层面问题
                return;
            }
        }
        log.info("[P0-06] 已写入 must_change_password=1 共 {} 条记录", updated);
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }
}
