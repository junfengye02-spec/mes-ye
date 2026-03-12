package com.mes.dispatch.controller;

import com.mes.common.result.R;
import com.mes.dispatch.domain.dto.DispatchAssignDTO;
import com.mes.dispatch.domain.vo.DispatchAssignmentVO;
import com.mes.dispatch.service.IDispatchAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 派工分配 Controller
 */
@Tag(name = "派工分配", description = "派工分配管理接口")
@RestController
@RequestMapping("/dispatch/assignment")
@RequiredArgsConstructor
public class DispatchAssignmentController {

    private final IDispatchAssignmentService assignmentService;

    @Operation(summary = "人员分派")
    @PostMapping("/person/{taskId}")
    public R<Void> assignPerson(
            @Parameter(description = "派工任务ID") @PathVariable Long taskId,
            @Valid @RequestBody DispatchAssignDTO dto) {
        assignmentService.assignPerson(taskId, dto);
        return R.ok();
    }

    @Operation(summary = "设备分派")
    @PostMapping("/device/{taskId}")
    public R<Void> assignDevice(
            @Parameter(description = "派工任务ID") @PathVariable Long taskId,
            @Valid @RequestBody DispatchAssignDTO dto) {
        assignmentService.assignDevice(taskId, dto);
        return R.ok();
    }

    @Operation(summary = "班组分派")
    @PostMapping("/team/{taskId}")
    public R<Void> assignTeam(
            @Parameter(description = "派工任务ID") @PathVariable Long taskId,
            @Valid @RequestBody DispatchAssignDTO dto) {
        assignmentService.assignTeam(taskId, dto);
        return R.ok();
    }

    @Operation(summary = "撤销分派")
    @PostMapping("/revoke/{assignmentId}")
    public R<Void> revoke(
            @Parameter(description = "分配记录ID") @PathVariable Long assignmentId,
            @Parameter(description = "撤销原因") @RequestParam String reason) {
        assignmentService.revoke(assignmentId, reason);
        return R.ok();
    }

    @Operation(summary = "查询任务分配记录")
    @GetMapping("/list/{taskId}")
    public R<List<DispatchAssignmentVO>> listByTaskId(
            @Parameter(description = "派工任务ID") @PathVariable Long taskId) {
        return R.ok(assignmentService.listByTaskId(taskId));
    }
}
