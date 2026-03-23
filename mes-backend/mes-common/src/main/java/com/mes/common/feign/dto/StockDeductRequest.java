package com.mes.common.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDeductRequest {
    private Long workOrderId;
    private String workOrderNo;
    private List<StockLockRequest.StockItem> items;
}
