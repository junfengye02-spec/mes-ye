package com.mes.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mes.admin.domain.dto.TenantRegisterDTO;
import com.mes.admin.domain.entity.SysTenant;
import com.mes.admin.mapper.SysTenantMapper;
import com.mes.admin.service.ITenantProvisioner;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.R;
import com.mes.framework.tenant.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 平台级租户管理接口。
 *
 * <p>仅允许平台超管（{@code tenant_id = 0}）访问：
 * - 生产环境建议配合 {@code @PreAuthorize("hasAuthority('platform:tenant:manage')")} 做细粒度权限；
 * - 本 Controller 额外在请求进入时判断 {@link TenantContextHolder#isPlatform()}，非平台上下文直接 403。</p>
 */
@Tag(name = "平台租户管理", description = "平台超管可用的租户注册 / 审批 / 停用 / 配额调整")
@RestController
@RequestMapping("/platform/tenants")
@RequiredArgsConstructor
public class PlatformTenantController {

    private final SysTenantMapper tenantMapper;
    private final ITenantProvisioner tenantProvisioner;

    @Operation(summary = "自助注册新租户（公开入口可以做网关白名单暴露给官网；平台超管也可调此接口）")
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody TenantRegisterDTO dto) {
        tenantProvisioner.register(
                dto.getTenantCode(), dto.getTenantName(),
                dto.getContactName(), dto.getContactEmail(),
                dto.getInitialAdminUsername(),
                StringUtils.hasText(dto.getInitialAdminPassword())
                        ? dto.getInitialAdminPassword() : "Change@Me-" + dto.getTenantCode());
        return R.ok();
    }

    @Operation(summary = "分页列出所有租户（超管）")
    @GetMapping
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public R<PageResult<TenantVO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                        @RequestParam(defaultValue = "20") long pageSize,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Integer status) {
        assertPlatform();
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<SysTenant>()
                .like(StringUtils.hasText(keyword), SysTenant::getTenantCode, keyword)
                .or(StringUtils.hasText(keyword), w -> w.like(SysTenant::getTenantName, keyword))
                .eq(status != null, SysTenant::getStatus, status)
                .orderByDesc(SysTenant::getCreatedTime);
        Page<SysTenant> page = tenantMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<TenantVO> items = page.getRecords().stream().map(TenantVO::from).collect(Collectors.toList());
        return R.ok(PageResult.of(items, page.getTotal()));
    }

    @Operation(summary = "查看单个租户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('platform:tenant:detail')")
    public R<TenantVO> detail(@PathVariable Long id) {
        assertPlatform();
        SysTenant t = tenantMapper.selectById(id);
        if (t == null) throw new BusinessException("租户不存在");
        return R.ok(TenantVO.from(t));
    }

    @Operation(summary = "暂停租户（登录 / 接口都会被网关拦下）")
    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('platform:tenant:suspend')")
    public R<Void> suspend(@PathVariable Long id) {
        assertPlatform();
        return updateStatus(id, 3);
    }

    @Operation(summary = "恢复租户")
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('platform:tenant:suspend')")
    public R<Void> resume(@PathVariable Long id) {
        assertPlatform();
        return updateStatus(id, 1);
    }

    @Operation(summary = "归档租户（只读）")
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('platform:tenant:archive')")
    public R<Void> archive(@PathVariable Long id) {
        assertPlatform();
        return updateStatus(id, 4);
    }

    @Operation(summary = "重新运行初始化（遇到 Provision 失败可重试）")
    @PostMapping("/{id}/reprovision")
    @PreAuthorize("hasAuthority('platform:tenant:reprovision')")
    public R<Void> reprovision(@PathVariable Long id) {
        assertPlatform();
        SysTenant t = tenantMapper.selectById(id);
        if (t == null) throw new BusinessException("租户不存在");
        tenantProvisioner.provisionAsync(id, "admin", "Change@Me-" + t.getTenantCode());
        return R.ok();
    }

    private R<Void> updateStatus(Long id, int newStatus) {
        SysTenant t = tenantMapper.selectById(id);
        if (t == null) throw new BusinessException("租户不存在");
        t.setStatus(newStatus);
        t.setUpdatedTime(LocalDateTime.now());
        tenantMapper.updateById(t);
        return R.ok();
    }

    private void assertPlatform() {
        if (!TenantContextHolder.isPlatform()) {
            throw new BusinessException("仅限平台超管访问");
        }
    }

    /** 输出 VO（隐藏 security_policy_json 等敏感字段原始值，保留常用字段） */
    @lombok.Data
    public static class TenantVO {
        private Long id;
        private String tenantCode;
        private String tenantName;
        private Integer status;
        private String schemaMode;
        private String dataRegion;
        private Integer quotaUsers;
        private Long quotaStorageMb;
        private Integer quotaQps;
        private LocalDateTime expireAt;
        private String contactName;
        private String contactEmail;
        private LocalDateTime createdTime;
        private LocalDateTime updatedTime;

        static TenantVO from(SysTenant t) {
            TenantVO vo = new TenantVO();
            vo.setId(t.getId());
            vo.setTenantCode(t.getTenantCode());
            vo.setTenantName(t.getTenantName());
            vo.setStatus(t.getStatus());
            vo.setSchemaMode(t.getSchemaMode());
            vo.setDataRegion(t.getDataRegion());
            vo.setQuotaUsers(t.getQuotaUsers());
            vo.setQuotaStorageMb(t.getQuotaStorageMb());
            vo.setQuotaQps(t.getQuotaQps());
            vo.setExpireAt(t.getExpireAt());
            vo.setContactName(t.getContactName());
            vo.setContactEmail(t.getContactEmail());
            vo.setCreatedTime(t.getCreatedTime());
            vo.setUpdatedTime(t.getUpdatedTime());
            return vo;
        }
    }
}
