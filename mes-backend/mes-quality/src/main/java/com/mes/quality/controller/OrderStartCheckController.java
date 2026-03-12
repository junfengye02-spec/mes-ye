package com.mes.quality.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.quality.domain.dto.OrderStartCheckDTO;
import com.mes.quality.domain.query.OrderStartCheckQuery;
import com.mes.quality.domain.vo.OrderStartCheckVO;
import com.mes.quality.service.IOrderStartCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "工单开工检查", description = "生产工单开工检查管理接口")
@RestController
@RequestMapping("/quality/order-start-check")
@RequiredArgsConstructor
public class OrderStartCheckController {

    private final IOrderStartCheckService orderStartCheckService;

    @Operation(summary = "分页查询工单开工检查")
    @GetMapping("/page")
    public R<PageResult<OrderStartCheckVO>> page(OrderStartCheckQuery query) {
        return R.ok(orderStartCheckService.page(query));
    }

    @Operation(summary = "获取工单开工检查详情")
    @GetMapping("/{id}")
    public R<OrderStartCheckVO> getDetail(@Parameter(description = "ID") @PathVariable Long id) {
        return R.ok(orderStartCheckService.getDetail(id));
    }

    @Operation(summary = "新增工单开工检查")
    @PostMapping
    public R<Long> create(@Valid @RequestBody OrderStartCheckDTO dto) {
        return R.ok("新增成功", orderStartCheckService.create(dto));
    }

    @Operation(summary = "修改工单开工检查")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "ID") @PathVariable Long id,
                          @Valid @RequestBody OrderStartCheckDTO dto) {
        orderStartCheckService.update(id, dto);
        return R.ok();
    }
}
