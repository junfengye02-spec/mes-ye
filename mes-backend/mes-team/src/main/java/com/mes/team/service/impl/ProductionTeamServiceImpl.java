package com.mes.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.team.domain.dto.ProductionTeamDTO;
import com.mes.team.domain.entity.ProductionTeam;
import com.mes.team.domain.query.ProductionTeamQuery;
import com.mes.team.domain.vo.ProductionTeamVO;
import com.mes.team.mapper.ProductionTeamMapper;
import com.mes.team.service.IProductionTeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 生产班组 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionTeamServiceImpl extends ServiceImpl<ProductionTeamMapper, ProductionTeam>
        implements IProductionTeamService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public PageResult<ProductionTeamVO> page(ProductionTeamQuery query) {
        // 构建查询条件
        LambdaQueryWrapper<ProductionTeam> wrapper = new LambdaQueryWrapper<ProductionTeam>()
                .like(StringUtils.hasText(query.getTeamCode()),
                        ProductionTeam::getTeamCode, query.getTeamCode())
                .like(StringUtils.hasText(query.getTeamName()),
                        ProductionTeam::getTeamName, query.getTeamName())
                .eq(query.getEnabled() != null,
                        ProductionTeam::getEnabled, query.getEnabled())
                .orderByDesc(ProductionTeam::getCreatedTime);

        // 分页查询
        Page<ProductionTeam> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        // 转换为 VO
        List<ProductionTeamVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public ProductionTeamVO getDetail(Long id) {
        ProductionTeam team = getById(id);
        AssertUtil.notNull(team, ResultCode.DATA_NOT_EXIST);
        return toVO(team);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductionTeamDTO dto) {
        // 校验编码唯一性
        checkCodeUnique(dto.getTeamCode(), null);

        ProductionTeam team = new ProductionTeam();
        BeanUtils.copyProperties(dto, team);
        team.setEnabled(1); // 默认启用
        save(team);

        log.info("新增班组: {} - {}", team.getTeamCode(), team.getTeamName());
        return team.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductionTeamDTO dto) {
        ProductionTeam existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);

        // 校验编码唯一性（排除自身）
        checkCodeUnique(dto.getTeamCode(), id);

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);

        log.info("修改班组: {} - {}", existing.getTeamCode(), existing.getTeamName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProductionTeam team = getById(id);
        AssertUtil.notNull(team, ResultCode.DATA_NOT_EXIST);

        assertNotReferenced("mes_process_info", "team_id", id, "工序");
        assertNotReferenced("mes_storage_inventory", "team_id", id, "库存台账");
        assertNotReferenced("mes_work_order_task_segment", "assigned_team_id", id, "APS任务段");
        assertNotReferenced("mes_shift_handover", "handover_team_id", id, "交班记录");
        assertNotReferenced("mes_shift_handover", "takeover_team_id", id, "接班记录");
        assertNoActiveDispatchReference(id);

        removeById(id);
        log.info("删除班组: {} - {}", team.getTeamCode(), team.getTeamName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleEnabled(Long id) {
        ProductionTeam team = getById(id);
        AssertUtil.notNull(team, ResultCode.DATA_NOT_EXIST);

        int newStatus = (team.getEnabled() != null && team.getEnabled() == 1) ? 0 : 1;
        team.setEnabled(newStatus);
        updateById(team);

        log.info("班组 {} 状态切换为: {}", team.getTeamCode(), newStatus == 1 ? "启用" : "停用");
    }

    // ==================== 私有方法 ====================

    /**
     * 校验班组编码唯一性
     */
    private void checkCodeUnique(String teamCode, Long excludeId) {
        LambdaQueryWrapper<ProductionTeam> wrapper = new LambdaQueryWrapper<ProductionTeam>()
                .eq(ProductionTeam::getTeamCode, teamCode)
                .ne(excludeId != null, ProductionTeam::getId, excludeId);
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "班组编码已存在: " + teamCode);
        }
    }

    /**
     * Entity → VO 转换
     */
    private ProductionTeamVO toVO(ProductionTeam entity) {
        ProductionTeamVO vo = new ProductionTeamVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private void assertNotReferenced(String table, String column, Long id, String label) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM " + table + " WHERE " + column + " = ?",
                Long.class,
                id);
        AssertUtil.isFalse(count != null && count > 0, "该班组已被" + label + "引用，无法删除");
    }

    private void assertNoActiveDispatchReference(Long id) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM mes_dispatch_assignment WHERE assign_type = 'TEAM' AND assignee_id = ?",
                Long.class,
                id);
        AssertUtil.isFalse(count != null && count > 0, "该班组已被派工分配引用，无法删除");
    }
}
