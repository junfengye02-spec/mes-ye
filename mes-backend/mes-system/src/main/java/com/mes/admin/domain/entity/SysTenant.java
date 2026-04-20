package com.mes.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户（企业）主数据。
 *
 * <p>本表在 MyBatis-Plus 租户拦截器的忽略名单中，业务代码查询时不会自动拼
 * {@code WHERE tenant_id = ?}，因此跨租户列表/详情需要由调用方显式做权限校验：
 * 通常只允许平台超管 {@code tenant_id = 0} 访问。</p>
 */
@Data
@TableName("sys_tenant")
public class SysTenant {

    private Long id;

    /** 租户编码（登录时用；子域名也以它为准） */
    private String tenantCode;

    /** 租户名称 */
    private String tenantName;

    /** 0=PENDING 1=ACTIVE 2=PROVISIONING 3=SUSPENDED 4=ARCHIVED */
    private Integer status;

    /** POOL=共享Schema；SCHEMA=独立Schema；DB=独立实例 */
    private String schemaMode;

    /** 数据归属区域 */
    private String dataRegion;

    /** 订阅计划 ID */
    private Long planId;

    /** 用户数上限 */
    private Integer quotaUsers;

    /** 文件存储上限（MB） */
    private Long quotaStorageMb;

    /** 接口 QPS 上限 */
    private Integer quotaQps;

    /** 订阅到期时间 */
    private LocalDateTime expireAt;

    private String contactName;
    private String contactPhone;
    private String contactEmail;

    /** 租户内首个管理员用户 ID */
    private Long primaryAdminUserId;

    /** JSON：{mfa:true, passwordMinLength:12, ipWhitelist:[...]} */
    private String securityPolicyJson;

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
