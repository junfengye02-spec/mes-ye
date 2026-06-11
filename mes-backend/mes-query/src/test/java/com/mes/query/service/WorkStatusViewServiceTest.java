package com.mes.query.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mes.common.core.PageResult;
import com.mes.query.domain.entity.WorkStatusView;
import com.mes.query.domain.query.WorkStatusViewQuery;
import com.mes.query.domain.vo.WorkStatusViewVO;
import com.mes.query.mapper.WorkStatusViewMapper;
import com.mes.query.service.impl.WorkStatusViewServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WorkStatusViewServiceImpl")
class WorkStatusViewServiceTest {

    @Mock
    private WorkStatusViewMapper workStatusViewMapper;

    @InjectMocks
    private WorkStatusViewServiceImpl workStatusViewService;

    @Test
    @DisplayName("page - 返回通用资源组编码字段")
    void page_returnsGenericResourceGroupCode() {
        WorkStatusView entity = new WorkStatusView();
        entity.setId(1L);
        entity.setWorkNo("WSV-001");
        entity.setWorkName("热处理");
        entity.setResourceGroupCode("RG-HEAT-01");

        Page<WorkStatusView> page = new Page<>(1, 20);
        page.setRecords(List.of(entity));
        page.setTotal(1L);

        when(workStatusViewMapper.selectPage(any(Page.class), any())).thenReturn(page);

        WorkStatusViewQuery query = new WorkStatusViewQuery();
        query.setPageNum(1);
        query.setPageSize(20);

        PageResult<WorkStatusViewVO> result = workStatusViewService.page(query);

        assertEquals(1L, result.getTotal());
        assertEquals("RG-HEAT-01", result.getList().get(0).getResourceGroupCode());
    }
}
