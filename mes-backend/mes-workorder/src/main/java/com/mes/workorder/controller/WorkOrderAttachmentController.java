package com.mes.workorder.controller;

import com.mes.common.result.R;
import com.mes.workorder.domain.dto.WorkOrderAttachmentDTO;
import com.mes.workorder.domain.vo.WorkOrderAttachmentVO;
import com.mes.workorder.service.IWorkOrderAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "工单文档附件", description = "工单文档附件管理接口")
@RestController
@RequestMapping("/workorder/attachment")
@RequiredArgsConstructor
public class WorkOrderAttachmentController {

    private final IWorkOrderAttachmentService attachmentService;

    @Operation(summary = "查询工单附件列表")
    @GetMapping("/list")
    public R<List<WorkOrderAttachmentVO>> list(
            @Parameter(description = "工单ID") @RequestParam Long workOrderId) {
        return R.ok(attachmentService.listByWorkOrderId(workOrderId));
    }

    @Operation(summary = "新增附件")
    @PostMapping
    public R<Long> create(
            @Parameter(description = "工单ID") @RequestParam Long workOrderId,
            @Valid @RequestBody WorkOrderAttachmentDTO dto) {
        return R.ok("新增成功", attachmentService.create(workOrderId, dto));
    }

    @Operation(summary = "删除附件")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "附件ID") @PathVariable Long id) {
        attachmentService.deleteAttachment(id);
        return R.ok();
    }
}
