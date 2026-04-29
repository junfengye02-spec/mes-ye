package com.mes.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 状态码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ==================== 通用 ====================
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "请求资源不存在"),

    // ==================== 数据操作 ====================
    DATA_NOT_EXIST(1001, "数据不存在"),
    DATA_ALREADY_EXIST(1002, "数据已存在"),
    DATA_REFERENCED(1003, "数据被引用，无法删除"),
    DATA_STATUS_ERROR(1004, "数据状态不正确"),

    // ==================== 导入导出 ====================
    IMPORT_EMPTY(2001, "导入数据为空"),
    IMPORT_ERROR(2002, "导入数据存在错误"),
    EXPORT_ERROR(2003, "导出失败"),

    // ==================== APS 集成 ====================
    APS_CONNECT_FAIL(3001, "APS服务连接失败"),
    APS_TIMEOUT(3002, "APS服务响应超时"),
    APS_AUTH_FAIL(3003, "APS认证失败"),
    SYNC_DATA_EMPTY(3004, "同步数据为空"),
    SYNC_VALIDATE_FAIL(3005, "同步数据校验失败"),
    SYNC_MAPPING_NOT_FOUND(3006, "数据映射不存在"),
    SYNC_CONFLICT(3007, "同步冲突"),

    // ==================== 认证与会话（P1-14 / P1-22） ====================
    /** 用于返回 HTTP 423 Locked：账户在 15 分钟滑动窗口内连续失败 ≥ 5 次 */
    ACCOUNT_LOCKED(423, "账号已锁定，请 15 分钟后再试或联系管理员解锁"),
    /** 需要验证码（连续失败 3 次后触发） */
    CAPTCHA_REQUIRED(1101, "请输入图形验证码"),
    /** 验证码错误或已过期 */
    CAPTCHA_INVALID(1102, "验证码错误或已过期"),
    /** 刷新令牌重放：同一 refresh token 被二次使用，视为令牌被盗 */
    REFRESH_TOKEN_REPLAYED(1103, "刷新令牌被重用，已强制登出所有会话"),
    /** Access / Refresh token 已被加入黑名单（登出 / 强制下线） */
    TOKEN_REVOKED(1104, "令牌已失效，请重新登录");

    private final int code;
    private final String message;
}
