package com.mes.abnormal.controller;

import com.mes.abnormal.domain.dto.AbnormalContactAttachmentDTO;
import com.mes.abnormal.domain.dto.AbnormalContactDTO;
import com.mes.abnormal.domain.query.AbnormalContactQuery;
import com.mes.abnormal.domain.vo.AbnormalContactAttachmentVO;
import com.mes.abnormal.domain.vo.AbnormalContactVO;
import com.mes.abnormal.service.IAbnormalContactService;
import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 异常联络单 Controller
 */
@Tag(name = "异常联络单", description = "异常联络单管理接口")
@RestController
@RequestMapping("/abnormal/contact")
@RequiredArgsConstructor
public class AbnormalContactController {

    private final IAbnormalContactService abnormalContactService;

    @Operation(summary = "分页查询异常联络单")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('abnormal:contact:list')")
    public R<PageResult<AbnormalContactVO>> page(AbnormalContactQuery query) {
        return R.ok(abnormalContactService.page(query));
    }

    @Operation(summary = "获取异常联络单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('abnormal:contact:detail')")
    public R<AbnormalContactVO> getDetail(
            @Parameter(description = "异常联络单ID") @PathVariable Long id) {
        return R.ok(abnormalContactService.getDetail(id));
    }

    @Operation(summary = "新增异常联络单（草稿）")
    @PostMapping
    @PreAuthorize("hasAuthority('abnormal:contact:create')")
    public R<Long> create(@Valid @RequestBody AbnormalContactDTO dto) {
        return R.ok("新增成功", abnormalContactService.create(dto));
    }

    @Operation(summary = "修改异常联络单")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('abnormal:contact:update')")
    public R<Void> update(
            @Parameter(description = "异常联络单ID") @PathVariable Long id,
            @Valid @RequestBody AbnormalContactDTO dto) {
        abnormalContactService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除异常联络单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('abnormal:contact:delete')")
    public R<Void> delete(
            @Parameter(description = "异常联络单ID") @PathVariable Long id) {
        abnormalContactService.delete(id);
        return R.ok();
    }

    @Operation(summary = "提交异常联络单")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('abnormal:contact:submit')")
    public R<Void> submit(
            @Parameter(description = "异常联络单ID") @PathVariable Long id) {
        abnormalContactService.submit(id);
        return R.ok();
    }

    @Operation(summary = "开始处理")
    @PostMapping("/{id}/process")
    @PreAuthorize("hasAuthority('abnormal:contact:process')")
    public R<Void> process(
            @Parameter(description = "异常联络单ID") @PathVariable Long id) {
        abnormalContactService.process(id);
        return R.ok();
    }

    @Operation(summary = "关闭异常联络单")
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('abnormal:contact:close')")
    public R<Void> close(
            @Parameter(description = "异常联络单ID") @PathVariable Long id) {
        abnormalContactService.close(id);
        return R.ok();
    }

    // ==================== 附件管理 ====================

    @Operation(summary = "查询附件列表")
    @GetMapping("/{contactId}/attachments")
    @PreAuthorize("hasAuthority('abnormal:contact:detail')")
    public R<List<AbnormalContactAttachmentVO>> listAttachments(
            @Parameter(description = "异常联络单ID") @PathVariable Long contactId) {
        return R.ok(abnormalContactService.listAttachments(contactId));
    }

    @Operation(summary = "新增附件")
    @PostMapping("/{contactId}/attachments")
    @PreAuthorize("hasAuthority('abnormal:contact:uploadAttachment')")
    public R<Long> addAttachment(
            @Parameter(description = "异常联络单ID") @PathVariable Long contactId,
            @Valid @RequestBody AbnormalContactAttachmentDTO dto) {
        return R.ok("新增成功", abnormalContactService.addAttachment(contactId, dto));
    }

    @Operation(summary = "删除附件")
    @DeleteMapping("/attachments/{attachmentId}")
    @PreAuthorize("hasAuthority('abnormal:contact:deleteAttachment')")
    public R<Void> deleteAttachment(
            @Parameter(description = "附件ID") @PathVariable Long attachmentId) {
        abnormalContactService.deleteAttachment(attachmentId);
        return R.ok();
    }

    @Operation(summary = "签署附件")
    @PostMapping("/attachments/{attachmentId}/sign")
    @PreAuthorize("hasAuthority('abnormal:contact:signAttachment')")
    public R<Void> signAttachment(
            @Parameter(description = "附件ID") @PathVariable Long attachmentId) {
        abnormalContactService.signAttachment(attachmentId);
        return R.ok();
    }
}
