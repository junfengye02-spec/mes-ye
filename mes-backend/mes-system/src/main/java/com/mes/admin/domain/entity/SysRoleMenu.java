package com.mes.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色-菜单关联（RBAC 交叉表）。
 *
 * <p><b>复合主键：</b>{@code (tenant_id, role_id, menu_id)}——由 DDL 层
 * {@code V2.02__tenantize_rbac.sql} 在加列 {@code tenant_id} 之后重建 PRIMARY KEY 保证。</p>
 *
 * <p><b>为什么不使用 {@code @TableId}？</b></p>
 * <ul>
 *   <li>MyBatis-Plus {@code @TableId} 仅支持单列主键。复合主键场景下若强行在任一字段标注
 *       {@code @TableId}，{@code updateById} / {@code deleteById} 等方法会只按单列匹配，
 *       存在跨租户误删/误改风险。</li>
 *   <li>本实体走 {@link com.baomidou.mybatisplus.core.mapper.BaseMapper#insert} 的全字段 INSERT，
 *       不需要 MP 识别主键；精确查询走 {@code LambdaQueryWrapper}
 *       显式 where (tenant_id = ?, role_id = ?, menu_id = ?)。</li>
 * </ul>
 *
 * <p><b>租户隔离语义：</b></p>
 * <ul>
 *   <li>{@code tenantId} 为冗余列（与所挂 {@code sys_role.tenant_id} 一致），
 *       防止"租户 A 的角色"被挂到"租户 B 的菜单"上。</li>
 *   <li>{@code tenantId = 0} 保留给"平台模板角色" 的菜单绑定，新租户 Provisioning
 *       时按此克隆到租户命名空间。</li>
 *   <li>该表登记在 {@link com.mes.framework.mybatis.MybatisPlusConfig} 的
 *       {@code DEFAULT_IGNORE_TABLES} 白名单，MP 不会自动在 WHERE 里加 tenant_id 条件；
 *       <b>业务代码必须显式</b>通过构造器 {@code new SysRoleMenu(tenantId, roleId, menuId)}
 *       或 setter 赋值 tenantId。</li>
 *   <li>兜底：若 {@code tenantId} 为 null，{@code MetaObjectHandler.insertFill}
 *       会从 {@code TenantContextHolder} 填入；取不到则直接抛 {@code IllegalStateException}
 *       （fail-closed），拒绝写入防止跨租户串数据。</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role_menu")
public class SysRoleMenu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 租户 ID（复合主键之一，冗余列）。DDL: {@code tenant_id BIGINT NOT NULL}。 */
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    /** 角色 ID（复合主键之一）。DDL: {@code role_id BIGINT NOT NULL}。 */
    @TableField("role_id")
    private Long roleId;

    /** 菜单 ID（复合主键之一）。DDL: {@code menu_id BIGINT NOT NULL}。 */
    @TableField("menu_id")
    private Long menuId;
}
