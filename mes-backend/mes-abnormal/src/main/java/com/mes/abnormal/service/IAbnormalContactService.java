package com.mes.abnormal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.abnormal.domain.dto.AbnormalContactAttachmentDTO;
import com.mes.abnormal.domain.dto.AbnormalContactDTO;
import com.mes.abnormal.domain.entity.AbnormalContact;
import com.mes.abnormal.domain.query.AbnormalContactQuery;
import com.mes.abnormal.domain.vo.AbnormalContactAttachmentVO;
import com.mes.abnormal.domain.vo.AbnormalContactVO;
import com.mes.common.core.PageResult;

import java.util.List;

/**
 * 异常联络单 Service 接口
 */
public interface IAbnormalContactService extends IService<AbnormalContact> {

    /** 分页查询 */
    PageResult<AbnormalContactVO> page(AbnormalContactQuery query);

    /** 获取详情（含附件和日志） */
    AbnormalContactVO getDetail(Long id);

    /** 新增（草稿） */
    Long create(AbnormalContactDTO dto);

    /** 修改（仅草稿状态） */
    void update(Long id, AbnormalContactDTO dto);

    /** 删除（仅草稿状态） */
    void delete(Long id);

    /** 提交 */
    void submit(Long id);

    /** 开始处理 */
    void process(Long id);

    /** 关闭 */
    void close(Long id);

    /** 新增附件 */
    Long addAttachment(Long contactId, AbnormalContactAttachmentDTO dto);

    /** 删除附件 */
    void deleteAttachment(Long attachmentId);

    /** 查询附件列表 */
    List<AbnormalContactAttachmentVO> listAttachments(Long contactId);

    /** 签署附件 */
    void signAttachment(Long attachmentId);
}
