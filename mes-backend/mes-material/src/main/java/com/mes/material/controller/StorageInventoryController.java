package com.mes.material.controller;

import com.mes.common.core.PageResult;
import com.mes.common.result.R;
import com.mes.material.domain.dto.StorageInventoryDTO;
import com.mes.material.domain.query.StorageInventoryQuery;
import com.mes.material.domain.vo.StorageInventoryVO;
import com.mes.material.service.IStorageInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 存储地点库存 Controller
 */
@Tag(name = "存储地点库存", description = "库存查询管理接口")
@RestController
@RequestMapping("/material/inventory")
@RequiredArgsConstructor
public class StorageInventoryController {

    private final IStorageInventoryService storageInventoryService;

    @Operation(summary = "分页查询库存")
    @GetMapping("/page")
    public R<PageResult<StorageInventoryVO>> page(StorageInventoryQuery query) {
        return R.ok(storageInventoryService.page(query));
    }

    @Operation(summary = "获取库存详情")
    @GetMapping("/{id}")
    public R<StorageInventoryVO> getDetail(
            @Parameter(description = "库存ID") @PathVariable Long id) {
        return R.ok(storageInventoryService.getDetail(id));
    }

    @Operation(summary = "新增库存")
    @PostMapping
    public R<Long> create(@Valid @RequestBody StorageInventoryDTO dto) {
        return R.ok("新增成功", storageInventoryService.create(dto));
    }

    @Operation(summary = "修改库存")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "库存ID") @PathVariable Long id,
            @Valid @RequestBody StorageInventoryDTO dto) {
        storageInventoryService.update(id, dto);
        return R.ok();
    }
}
