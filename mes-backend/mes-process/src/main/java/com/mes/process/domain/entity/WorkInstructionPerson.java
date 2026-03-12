package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 指导书人员实体
 */
@Data
@TableName("mes_work_instruction_person")
public class WorkInstructionPerson implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 指导书ID */
    private Long instructionId;

    /** 人员编号 */
    private String personCode;

    /** 姓名 */
    private String personName;

    /** 人员分类 */
    private String personCategory;

    /** 性别 */
    private String gender;

    /** 出生日期 */
    private LocalDate birthDate;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;
}
