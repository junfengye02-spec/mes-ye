package com.mes.workorder.domain.doc;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产工单 ES 文档（P2-28）
 *
 * <p>定位：</p>
 * <ul>
 *   <li>MySQL {@code mes_work_order} 表的查询加速副本，用于百万级以上模糊检索 / 多字段聚合</li>
 *   <li>只存查询常用字段，不存完整记录；详情仍走 MySQL</li>
 *   <li>按租户分索引：{@code mes_work_order-{tenantId}}，避免跨租户数据越权</li>
 * </ul>
 *
 * <p>分片策略：建议每个租户索引 3 主分片 / 1 副本（生产环境），详见 elasticsearch-setup.md。</p>
 *
 * <p>约束：</p>
 * <ol>
 *   <li>ES 不是事实源头，同步失败不能阻塞业务；见 {@code WorkOrderEsSyncListener}</li>
 *   <li>{@link #indexName} 使用 SpEL：{@code #{@esIndexNameResolver.prefix}mes_work_order}，
 *       真实租户索引名由业务层调用 Repository 时显式传入 IndexCoordinates</li>
 * </ol>
 */
@Data
@Document(indexName = "#{@esIndexNameResolver.prefix}mes_work_order",
        createIndex = false,
        writeTypeHint = WriteTypeHint.FALSE)
public class WorkOrderDoc {

    /**
     * 工单 ID（与 MySQL 主键一致），ES 的 _id。
     */
    @Id
    private Long id;

    /**
     * 工单号，精确匹配用 keyword；支持前缀搜索，text 可选。
     */
    @Field(type = FieldType.Keyword)
    private String workOrderNo;

    /**
     * 订单编号，业务高频筛选字段。
     */
    @Field(type = FieldType.Keyword)
    private String orderNo;

    /**
     * 主产品名称，用于 wildcard / match 搜索。
     * 使用 text+keyword 双字段：模糊用 text，聚合/精确用 keyword。
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String mainProduct;

    /**
     * 产品编码。
     */
    @Field(type = FieldType.Keyword)
    private String productCode;

    /**
     * 产品名称，中文搜索用 IK 分词。
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String productName;

    /**
     * 项目名称，支持模糊搜索（如按项目报表）。
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String projectName;

    /**
     * 工单状态，高频筛选：CREATED / RELEASED / IN_PROGRESS / COMPLETED / FORCE_COMPLETED。
     */
    @Field(type = FieldType.Keyword)
    private String status;

    /**
     * 机型，聚合 / 筛选。
     */
    @Field(type = FieldType.Keyword)
    private String machineModel;

    /**
     * 计划数量，数值字段支持范围查询。
     */
    @Field(type = FieldType.Double)
    private BigDecimal planQty;

    /**
     * 租户 ID，用于索引名 + term 过滤双重隔离。
     */
    @Field(type = FieldType.Long)
    private Long tenantId;

    /**
     * 创建时间，时间范围查询用。
     * 使用 strict_date_optional_time 格式，避免跨时区问题。
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second,
            pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /**
     * 计划开始时间。
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second,
            pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planStartTime;

    /**
     * 计划结束时间。
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second,
            pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planEndTime;
}
