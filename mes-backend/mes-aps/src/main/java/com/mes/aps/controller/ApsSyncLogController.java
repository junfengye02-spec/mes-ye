package com.mes.aps.controller;

import com.mes.aps.domain.query.ApsSyncLogQuery;
import com.mes.aps.domain.vo.ApsSyncLogVO;
import com.mes.aps.service.IApsSyncLogService;
import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * APS 同步日志 Controller
 */
@Tag(name = "APS同步日志", description = "APS同步日志查询接口")
@RestController
@RequestMapping("/aps/log")
@RequiredArgsConstructor
public class ApsSyncLogController {

    private final IApsSyncLogService apsSyncLogService;

    @Operation(summary = "分页查询同步日志")
    @GetMapping("/page")
    public R<PageResult<ApsSyncLogVO>> page(ApsSyncLogQuery query) {
        return R.ok(apsSyncLogService.page(query));
    }

    @Operation(summary = "获取日志详情")
    @GetMapping("/{id}")
    public R<ApsSyncLogVO> getDetail(
            @Parameter(description = "日志ID") @PathVariable Long id) {
        return R.ok(apsSyncLogService.getDetail(id));
    }
}
