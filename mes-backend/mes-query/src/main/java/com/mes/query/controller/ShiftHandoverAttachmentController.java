package com.mes.query.controller;

import com.mes.common.result.R;
import com.mes.query.domain.dto.ShiftHandoverAttachmentDTO;
import com.mes.query.domain.vo.ShiftHandoverAttachmentVO;
import com.mes.query.service.IShiftHandoverAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "交班记录附件", description = "交班记录附件管理接口")
@RestController
@RequestMapping("/query/shift-handover-attachment")
@RequiredArgsConstructor
public class ShiftHandoverAttachmentController {

    private final IShiftHandoverAttachmentService attachmentService;

    @Operation(summary = "查询交班附件列表")
    @GetMapping("/list/{handoverId}")
    public R<List<ShiftHandoverAttachmentVO>> listByHandoverId(
            @Parameter(description = "交班记录ID") @PathVariable Long handoverId) {
        return R.ok(attachmentService.listByHandoverId(handoverId));
    }

    @Operation(summary = "新增交班附件")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ShiftHandoverAttachmentDTO dto) {
        return R.ok("新增成功", attachmentService.create(dto));
    }

    @Operation(summary = "删除交班附件")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "附件ID") @PathVariable Long id) {
        attachmentService.delete(id);
        return R.ok();
    }
}
