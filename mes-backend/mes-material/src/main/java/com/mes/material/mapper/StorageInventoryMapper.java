package com.mes.material.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.material.domain.entity.StorageInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface StorageInventoryMapper extends BaseMapper<StorageInventory> {

    /**
     * 原子性扣减库存（领料）
     * @return 受影响行数，0 表示库存不足
     */
    @Update("UPDATE mes_storage_inventory SET unrestricted_stock = unrestricted_stock - #{qty}, " +
            "updated_time = NOW() WHERE id = #{id} AND unrestricted_stock >= #{qty}")
    int deductStock(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /**
     * 原子性增加库存（入库/退料）
     */
    @Update("UPDATE mes_storage_inventory SET unrestricted_stock = unrestricted_stock + #{qty}, " +
            "updated_time = NOW() WHERE id = #{id}")
    int addStock(@Param("id") Long id, @Param("qty") BigDecimal qty);
}
