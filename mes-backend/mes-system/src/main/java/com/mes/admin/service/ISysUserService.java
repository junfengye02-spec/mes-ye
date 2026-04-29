package com.mes.admin.service;

import com.mes.admin.domain.dto.SysUserDTO;
import com.mes.admin.domain.query.SysUserQuery;
import com.mes.admin.domain.vo.SysUserVO;
import com.mes.common.core.PageResult;

public interface ISysUserService {
    PageResult<SysUserVO> page(SysUserQuery query);
    SysUserVO getDetail(Long id);
    Long create(SysUserDTO dto);
    void update(Long id, SysUserDTO dto);
    void delete(Long id);
    void resetPassword(Long id, String newPassword);

    /**
     * 重置密码并返回实际生效的密码明文（P0-07 安全整改）。
     *
     * @param id          用户 ID
     * @param newPassword 新密码明文，可为空；空时由服务端生成 12 位随机强密码
     * @return 实际生效的密码明文，一次性返回给管理员当面告知用户
     */
    String resetPasswordAndReturn(Long id, String newPassword);

    /**
     * 当前登录用户自助修改密码（P0-06 安全整改）。
     *
     * <p>用于：
     * <ul>
     *   <li>弱口令审计命中后被强制改密；</li>
     *   <li>用户主动在个人中心修改密码。</li>
     * </ul>
     * </p>
     *
     * <p>校验规则：
     * <ul>
     *   <li>currentUserId 对应的账号存在；</li>
     *   <li>oldPassword 与 DB 中 BCrypt 密文匹配；</li>
     *   <li>newPassword 长度 &ge; 8 且 &ne; oldPassword；</li>
     *   <li>newPassword 至少含大写/小写/数字/特殊字符中 3 类。</li>
     * </ul>
     * </p>
     *
     * <p>成功后副作用：重置 {@code must_change_password=0}，清除弱口令强制改密标记。</p>
     *
     * @param currentUserId 当前登录用户 ID（来自 SecurityContext，不由请求体传入以防越权）
     * @param oldPassword   原密码明文
     * @param newPassword   新密码明文
     * @throws com.mes.common.exception.BusinessException 任一校验失败
     */
    void changeMyPassword(Long currentUserId, String oldPassword, String newPassword);
}
