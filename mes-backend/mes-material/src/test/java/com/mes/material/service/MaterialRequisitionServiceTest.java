package com.mes.material.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.common.exception.BusinessException;
import com.mes.material.domain.dto.MaterialRequisitionDTO;
import com.mes.material.domain.dto.MaterialRequisitionItemDTO;
import com.mes.material.domain.entity.MaterialRequisition;
import com.mes.material.domain.entity.MaterialRequisitionItem;
import com.mes.material.domain.entity.StorageInventory;
import com.mes.material.mapper.MaterialRequisitionItemMapper;
import com.mes.material.mapper.MaterialRequisitionMapper;
import com.mes.material.service.impl.MaterialRequisitionServiceImpl;
import com.mes.workorder.domain.entity.WorkOrder;
import com.mes.workorder.domain.entity.WorkOrderInputMaterial;
import com.mes.workorder.enums.WorkOrderStatus;
import com.mes.workorder.mapper.WorkOrderInputMaterialMapper;
import com.mes.workorder.mapper.WorkOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaterialRequisitionServiceImpl")
class MaterialRequisitionServiceTest {

    @Mock
    private MaterialRequisitionMapper requisitionMapper;

    @Mock
    private MaterialRequisitionItemMapper itemMapper;

    @Mock
    private IStorageInventoryService storageInventoryService;

    @Mock
    private WorkOrderMapper workOrderMapper;

    @Mock
    private WorkOrderInputMaterialMapper workOrderInputMaterialMapper;

    private MaterialRequisitionServiceImpl materialRequisitionService;

    @BeforeEach
    void setUp() {
        materialRequisitionService = new MaterialRequisitionServiceImpl(
                itemMapper,
                storageInventoryService,
                workOrderMapper,
                workOrderInputMaterialMapper);
        ReflectionTestUtils.setField(materialRequisitionService, "baseMapper", requisitionMapper);
    }

    @Test
    @DisplayName("创建领料单 - 非 RELEASED/IN_PROGRESS 工单拒绝")
    void create_rejectsWorkOrderWithInvalidStatus() {
        MaterialRequisitionDTO dto = buildDto(new BigDecimal("2"));
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(10L);
        workOrder.setStatus(WorkOrderStatus.CREATED.getCode());
        when(workOrderMapper.selectById(10L)).thenReturn(workOrder);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> materialRequisitionService.create(dto));

        assertEquals("仅已下达/执行中的工单可以领料", ex.getMessage());
        verify(requisitionMapper, never()).insert(any(MaterialRequisition.class));
    }

    @Test
    @DisplayName("创建领料单 - 明细为空时拒绝")
    void create_rejectsEmptyItems() {
        MaterialRequisitionDTO dto = new MaterialRequisitionDTO();
        dto.setWorkOrderId(10L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> materialRequisitionService.create(dto));

        assertEquals("领料明细不能为空", ex.getMessage());
        verify(workOrderMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("创建领料单 - 物料为空时拒绝")
    void create_rejectsNullMaterialId() {
        MaterialRequisitionDTO dto = buildDto(new BigDecimal("2"));
        dto.getItems().get(0).setMaterialId(null);
        when(workOrderMapper.selectById(10L)).thenReturn(releasedWorkOrder());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> materialRequisitionService.create(dto));

        assertEquals("领料明细物料不能为空", ex.getMessage());
        verify(itemMapper, never()).insert(any(MaterialRequisitionItem.class));
    }

    @Test
    @DisplayName("创建领料单 - 超出工单剩余需求时拒绝")
    void create_rejectsIssueQtyBeyondRemainingRequirement() {
        MaterialRequisitionDTO dto = buildDto(new BigDecimal("9"));
        when(workOrderMapper.selectById(10L)).thenReturn(releasedWorkOrder());
        when(workOrderInputMaterialMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(requirement(new BigDecimal("10"), new BigDecimal("3")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> materialRequisitionService.create(dto));

        assertEquals("领料数量超过工单剩余需求", ex.getMessage());
        verify(storageInventoryService, never()).deductStock(any(), any());
    }

    @Test
    @DisplayName("创建领料单 - 成功时更新工单物料已发数量并扣减库存")
    void create_updatesIssuedQtyAndDeductsInventory() {
        MaterialRequisitionDTO dto = buildDto(new BigDecimal("4"));
        when(workOrderMapper.selectById(10L)).thenReturn(releasedWorkOrder());
        when(workOrderInputMaterialMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(requirement(new BigDecimal("10"), new BigDecimal("3")));
        when(requisitionMapper.insert(any(MaterialRequisition.class))).thenAnswer(invocation -> {
            MaterialRequisition requisition = invocation.getArgument(0);
            requisition.setId(200L);
            return 1;
        });
        when(itemMapper.insert(any(MaterialRequisitionItem.class))).thenReturn(1);
        when(workOrderInputMaterialMapper.updateById(any(WorkOrderInputMaterial.class))).thenReturn(1);
        when(storageInventoryService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(inventory(99L)));

        Long requisitionId = materialRequisitionService.create(dto);

        assertEquals(200L, requisitionId);
        ArgumentCaptor<WorkOrderInputMaterial> requirementCaptor = ArgumentCaptor.forClass(WorkOrderInputMaterial.class);
        verify(workOrderInputMaterialMapper).updateById(requirementCaptor.capture());
        assertEquals(new BigDecimal("7"), requirementCaptor.getValue().getIssuedQty());
        verify(storageInventoryService).deductStock(99L, new BigDecimal("4"));
    }

    @Test
    @DisplayName("APS MRP 生成领料申请 - 仅建申请和明细，不扣库存也不回写已发数量")
    void createFromMrp_createsPlannedRequisitionWithoutInventorySideEffects() {
        MaterialRequisitionDTO dto = buildDto(new BigDecimal("4"));
        dto.setProductCode("PROD-001");
        dto.setProductName("测试产品");
        when(workOrderMapper.selectById(10L)).thenReturn(releasedWorkOrder());
        when(requisitionMapper.insert(any(MaterialRequisition.class))).thenAnswer(invocation -> {
            MaterialRequisition requisition = invocation.getArgument(0);
            requisition.setId(201L);
            return 1;
        });
        when(itemMapper.insert(any(MaterialRequisitionItem.class))).thenReturn(1);

        Long requisitionId = materialRequisitionService.createFromMrp(dto);

        assertEquals(201L, requisitionId);
        ArgumentCaptor<MaterialRequisition> requisitionCaptor = ArgumentCaptor.forClass(MaterialRequisition.class);
        verify(requisitionMapper).insert(requisitionCaptor.capture());
        assertEquals("CREATED", requisitionCaptor.getValue().getStatus());
        assertNotNull(requisitionCaptor.getValue().getRequisitionNo());

        ArgumentCaptor<MaterialRequisitionItem> itemCaptor = ArgumentCaptor.forClass(MaterialRequisitionItem.class);
        verify(itemMapper).insert(itemCaptor.capture());
        assertEquals(new BigDecimal("4"), itemCaptor.getValue().getDemandQty());
        assertEquals(new BigDecimal("4"), itemCaptor.getValue().getPendingQty());
        assertEquals(BigDecimal.ZERO, itemCaptor.getValue().getIssueQty());

        verify(storageInventoryService, never()).deductStock(any(), any());
        verify(workOrderInputMaterialMapper, never()).updateById(any(WorkOrderInputMaterial.class));
    }

    @Test
    @DisplayName("创建领料单 - 扩展头字段可通过正常 DTO 写入")
    void create_persistsExtendedHeaderFieldsFromDto() {
        MaterialRequisitionDTO dto = buildDto(new BigDecimal("3"));
        LocalDateTime actualStartTime = LocalDateTime.of(2026, 5, 28, 8, 30);
        LocalDateTime actualEndTime = LocalDateTime.of(2026, 5, 28, 10, 15);
        dto.setActualQty(new BigDecimal("2.5"));
        dto.setQualifiedQty(new BigDecimal("2.0"));
        dto.setActualStartTime(actualStartTime);
        dto.setActualEndTime(actualEndTime);
        dto.setSalesOrderLine("SO-001-10");

        when(workOrderMapper.selectById(10L)).thenReturn(releasedWorkOrder());
        when(workOrderInputMaterialMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(requirement(new BigDecimal("10"), BigDecimal.ZERO));
        when(requisitionMapper.insert(any(MaterialRequisition.class))).thenAnswer(invocation -> {
            MaterialRequisition requisition = invocation.getArgument(0);
            requisition.setId(203L);
            return 1;
        });
        when(itemMapper.insert(any(MaterialRequisitionItem.class))).thenReturn(1);
        when(workOrderInputMaterialMapper.updateById(any(WorkOrderInputMaterial.class))).thenReturn(1);
        when(storageInventoryService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(inventory(99L)));

        materialRequisitionService.create(dto);

        ArgumentCaptor<MaterialRequisition> requisitionCaptor = ArgumentCaptor.forClass(MaterialRequisition.class);
        verify(requisitionMapper).insert(requisitionCaptor.capture());
        MaterialRequisition saved = requisitionCaptor.getValue();
        assertEquals(new BigDecimal("2.5"), saved.getActualQty());
        assertEquals(new BigDecimal("2.0"), saved.getQualifiedQty());
        assertEquals(actualStartTime, saved.getActualStartTime());
        assertEquals(actualEndTime, saved.getActualEndTime());
        assertEquals("SO-001-10", saved.getSalesOrderLine());
    }

    @Test
    @DisplayName("修改领料单 - 先冲销旧库存和旧已发数量，再应用新明细")
    void update_revertsOldStateBeforeApplyingNewItems() {
        MaterialRequisition existing = new MaterialRequisition();
        existing.setId(200L);
        existing.setRequisitionNo("LL-200");
        when(requisitionMapper.selectById(200L)).thenReturn(existing);
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(savedItem(200L, 10L, 100L, new BigDecimal("4"))));
        when(storageInventoryService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(inventory(99L)));
        when(workOrderMapper.selectById(10L)).thenReturn(inProgressWorkOrder());
        when(workOrderInputMaterialMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(
                        requirement(new BigDecimal("10"), new BigDecimal("6")),
                        requirement(new BigDecimal("10"), new BigDecimal("2")));
        when(requisitionMapper.updateById(any(MaterialRequisition.class))).thenReturn(1);
        when(itemMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(itemMapper.insert(any(MaterialRequisitionItem.class))).thenReturn(1);
        when(workOrderInputMaterialMapper.updateById(any(WorkOrderInputMaterial.class))).thenReturn(1);

        materialRequisitionService.update(200L, buildDto(new BigDecimal("2")));

        verify(storageInventoryService).addStock(99L, new BigDecimal("4"));
        verify(storageInventoryService).deductStock(99L, new BigDecimal("2"));
        ArgumentCaptor<WorkOrderInputMaterial> requirementCaptor = ArgumentCaptor.forClass(WorkOrderInputMaterial.class);
        verify(workOrderInputMaterialMapper, org.mockito.Mockito.times(2)).updateById(requirementCaptor.capture());
        List<WorkOrderInputMaterial> updates = requirementCaptor.getAllValues();
        assertEquals(new BigDecimal("2"), updates.get(0).getIssuedQty());
        assertEquals(new BigDecimal("4"), updates.get(1).getIssuedQty());
    }

    @Test
    @DisplayName("获取领料详情 - 返回扩展头字段")
    void getDetail_returnsExtendedHeaderFields() {
        MaterialRequisition entity = new MaterialRequisition();
        entity.setId(204L);
        entity.setRequisitionNo("LL-204");
        entity.setActualQty(new BigDecimal("6"));
        entity.setQualifiedQty(new BigDecimal("5"));
        entity.setActualStartTime(LocalDateTime.of(2026, 5, 28, 7, 0));
        entity.setActualEndTime(LocalDateTime.of(2026, 5, 28, 9, 0));
        entity.setSalesOrderLine("SO-204-20");
        entity.setWbsElement("WBS-204");

        when(requisitionMapper.selectById(204L)).thenReturn(entity);
        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        var detail = materialRequisitionService.getDetail(204L);

        assertEquals(new BigDecimal("6"), detail.getActualQty());
        assertEquals(new BigDecimal("5"), detail.getQualifiedQty());
        assertEquals(LocalDateTime.of(2026, 5, 28, 7, 0), detail.getActualStartTime());
        assertEquals(LocalDateTime.of(2026, 5, 28, 9, 0), detail.getActualEndTime());
        assertEquals("SO-204-20", detail.getSalesOrderLine());
        assertEquals("WBS-204", detail.getWbsElement());
    }

    @Test
    @DisplayName("创建领料单 - 多库位时优先命中有库存的记录而不是随机首条")
    void create_prefersInventoryWithEnoughStockAcrossLocations() {
        MaterialRequisitionDTO dto = buildDto(new BigDecimal("4"));
        when(workOrderMapper.selectById(10L)).thenReturn(releasedWorkOrder());
        when(workOrderInputMaterialMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(requirement(new BigDecimal("10"), new BigDecimal("1")));
        when(requisitionMapper.insert(any(MaterialRequisition.class))).thenAnswer(invocation -> {
            MaterialRequisition requisition = invocation.getArgument(0);
            requisition.setId(202L);
            return 1;
        });
        when(itemMapper.insert(any(MaterialRequisitionItem.class))).thenReturn(1);
        when(workOrderInputMaterialMapper.updateById(any(WorkOrderInputMaterial.class))).thenReturn(1);
        when(storageInventoryService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                inventory(10L, "LOC-A", new BigDecimal("1")),
                inventory(20L, "LOC-B", new BigDecimal("8"))));

        materialRequisitionService.create(dto);

        verify(storageInventoryService).deductStock(20L, new BigDecimal("4"));
    }

    private MaterialRequisitionDTO buildDto(BigDecimal qty) {
        MaterialRequisitionItemDTO item = new MaterialRequisitionItemDTO();
        item.setMaterialId(100L);
        item.setMaterialCode("MAT-100");
        item.setMaterialName("测试物料");
        item.setDemandQty(qty);
        item.setUnit("PCS");

        MaterialRequisitionDTO dto = new MaterialRequisitionDTO();
        dto.setWorkOrderId(10L);
        dto.setWorkOrderNo("WO-10");
        dto.setItems(List.of(item));
        return dto;
    }

    private WorkOrder releasedWorkOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(10L);
        workOrder.setStatus(WorkOrderStatus.RELEASED.getCode());
        return workOrder;
    }

    private WorkOrder inProgressWorkOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(10L);
        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS.getCode());
        return workOrder;
    }

    private WorkOrderInputMaterial requirement(BigDecimal requiredQty, BigDecimal issuedQty) {
        WorkOrderInputMaterial requirement = new WorkOrderInputMaterial();
        requirement.setId(300L);
        requirement.setWorkOrderId(10L);
        requirement.setMaterialId(100L);
        requirement.setRequiredQty(requiredQty);
        requirement.setIssuedQty(issuedQty);
        return requirement;
    }

    private StorageInventory inventory(Long id) {
        StorageInventory inventory = new StorageInventory();
        inventory.setId(id);
        inventory.setMaterialId(100L);
        inventory.setUnrestrictedStock(new BigDecimal("10"));
        return inventory;
    }

    private StorageInventory inventory(Long id, String location, BigDecimal unrestrictedStock) {
        StorageInventory inventory = new StorageInventory();
        inventory.setId(id);
        inventory.setMaterialId(100L);
        inventory.setStorageLocation(location);
        inventory.setUnrestrictedStock(unrestrictedStock);
        return inventory;
    }

    private MaterialRequisitionItem savedItem(Long requisitionId, Long workOrderId, Long materialId, BigDecimal qty) {
        MaterialRequisitionItem item = new MaterialRequisitionItem();
        item.setRequisitionId(requisitionId);
        item.setWorkOrderId(workOrderId);
        item.setMaterialId(materialId);
        item.setDemandQty(qty);
        item.setIssueQty(qty);
        item.setPendingQty(BigDecimal.ZERO);
        return item;
    }
}
