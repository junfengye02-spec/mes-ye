package com.mes.material.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.material.domain.dto.StorageInventoryDTO;
import com.mes.material.domain.entity.StorageInventory;
import com.mes.material.domain.query.StorageInventoryQuery;
import com.mes.material.domain.vo.StorageInventoryVO;
import java.math.BigDecimal;

public interface IStorageInventoryService extends IService<StorageInventory> {
    PageResult<StorageInventoryVO> page(StorageInventoryQuery query);
    StorageInventoryVO getDetail(Long id);
    Long create(StorageInventoryDTO dto);
    void update(Long id, StorageInventoryDTO dto);
    /** 原子性扣减库存，库存不足时抛出异常 */
    void deductStock(Long inventoryId, BigDecimal qty);
    /** 原子性增加库存 */
    void addStock(Long inventoryId, BigDecimal qty);
}
