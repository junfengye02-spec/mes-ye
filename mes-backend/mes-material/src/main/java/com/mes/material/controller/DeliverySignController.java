package com.mes.material.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.material.domain.dto.DeliverySignDTO;
import com.mes.material.domain.query.DeliverySignQuery;
import com.mes.material.domain.vo.DeliverySignVO;
import com.mes.material.service.IDeliverySignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 发货签收 Controller
 */
@Tag(name = "发货签收", description = "发货签收管理接口")
@RestController
@RequestMapping("/material/delivery-sign")
@RequiredArgsConstructor
public class DeliverySignController {

    private final IDeliverySignService deliverySignService;

    @Operation(summary = "分页查询发货签收")
    @GetMapping("/page")
    public R<PageResult<DeliverySignVO>> page(DeliverySignQuery query) {
        return R.ok(deliverySignService.page(query));
    }

    @Operation(summary = "新增发货签收")
    @PostMapping
    public R<Long> create(@Valid @RequestBody DeliverySignDTO dto) {
        return R.ok("新增成功", deliverySignService.create(dto));
    }

    @Operation(summary = "确认发货签收")
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(
            @Parameter(description = "发货签收ID") @PathVariable Long id) {
        deliverySignService.confirm(id);
        return R.ok();
    }
}
