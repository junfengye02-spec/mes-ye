package com.mes.query.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.query.domain.query.ProductionWorkQuery;
import com.mes.query.domain.vo.ProductionWorkVO;
import com.mes.query.service.IProductionWorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "生产工作查询", description = "生产工作查询接口")
@RestController
@RequestMapping("/query/production-work")
@RequiredArgsConstructor
public class ProductionWorkController {

    private final IProductionWorkService productionWorkService;

    @Operation(summary = "分页查询生产工作")
    @GetMapping("/page")
    public R<PageResult<ProductionWorkVO>> page(ProductionWorkQuery query) {
        return R.ok(productionWorkService.page(query));
    }

    @Operation(summary = "获取生产工作详情")
    @GetMapping("/{id}")
    public R<ProductionWorkVO> getDetail(@Parameter(description = "ID") @PathVariable Long id) {
        return R.ok(productionWorkService.getDetail(id));
    }
}
