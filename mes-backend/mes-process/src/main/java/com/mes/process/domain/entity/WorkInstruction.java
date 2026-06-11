package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 指导书实体
 * <p>用于沉淀可复用的标准作业指导书（SOP）模板，可被多个指示书引用。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_work_instruction")
public class WorkInstruction extends BaseEntity {

    /** 指导书编号 */
    private String instructionCode;

    /** 指导书名称 */
    private String instructionName;

    /** 关联工序ID */
    private Long processId;

    /** 版本 */
    private String version;

    /** 作业内容 */
    private String content;

    /** 备注 */
    private String remark;

    /** 等级 */
    private String level;

    /** 状态 */
    private String status;
}
