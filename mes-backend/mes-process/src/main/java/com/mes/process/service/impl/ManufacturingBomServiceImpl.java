package com.mes.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.process.domain.dto.BomSubstituteDTO;
import com.mes.process.domain.dto.ManufacturingBomDTO;
import com.mes.process.domain.dto.ManufacturingBomItemDTO;
import com.mes.process.domain.entity.BomSubstitute;
import com.mes.process.domain.entity.BomVersionLog;
import com.mes.process.domain.entity.ManufacturingBom;
import com.mes.process.domain.entity.ManufacturingBomItem;
import com.mes.process.domain.query.ManufacturingBomQuery;
import com.mes.process.domain.vo.BomSubstituteVO;
import com.mes.process.domain.vo.ManufacturingBomItemVO;
import com.mes.process.domain.vo.ManufacturingBomVO;
import com.mes.process.enums.BomStatus;
import com.mes.process.mapper.BomSubstituteMapper;
import com.mes.process.mapper.BomVersionLogMapper;
import com.mes.process.mapper.ManufacturingBomItemMapper;
import com.mes.process.mapper.ManufacturingBomMapper;
import com.mes.process.service.IManufacturingBomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 制造BOM Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManufacturingBomServiceImpl extends ServiceImpl<ManufacturingBomMapper, ManufacturingBom>
        implements IManufacturingBomService {

    private final ManufacturingBomItemMapper itemMapper;
    private final BomSubstituteMapper substituteMapper;
    private final BomVersionLogMapper versionLogMapper;

    @Override
    public PageResult<ManufacturingBomVO> page(ManufacturingBomQuery query) {
        LambdaQueryWrapper<ManufacturingBom> wrapper = new LambdaQueryWrapper<ManufacturingBom>()
                .like(StringUtils.hasText(query.getBomCode()),
                        ManufacturingBom::getBomCode, query.getBomCode())
                .like(StringUtils.hasText(query.getBomName()),
                        ManufacturingBom::getBomName, query.getBomName())
                .like(StringUtils.hasText(query.getProductCode()),
                        ManufacturingBom::getProductCode, query.getProductCode())
                .eq(StringUtils.hasText(query.getProductCategory()),
                        ManufacturingBom::getProductCategory, query.getProductCategory())
                .eq(StringUtils.hasText(query.getStatus()),
                        ManufacturingBom::getStatus, query.getStatus())
                .orderByDesc(ManufacturingBom::getCreatedTime);

        Page<ManufacturingBom> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<ManufacturingBomVO> voList = page.getRecords().stream()
                .map(this::toBomVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public ManufacturingBomVO getDetail(Long id) {
        ManufacturingBom entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        ManufacturingBomVO vo = toBomVO(entity);
        vo.setItems(getItemTree(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ManufacturingBomDTO dto) {
        ManufacturingBom entity = new ManufacturingBom();
        BeanUtils.copyProperties(dto, entity);
        entity.setBomVersion("V1");
        entity.setStatus(BomStatus.DRAFT.getCode());
        save(entity);

        // 递归保存明细树
        if (!CollectionUtils.isEmpty(dto.getItems())) {
            saveItemsRecursive(entity.getId(), null, 1, dto.getItems());
        }

        // 记录版本日志
        saveBomVersionLog(entity.getId(), null, "V1", "创建", "创建BOM " + entity.getBomCode());

        log.info("新增制造BOM: {} V{}", entity.getBomCode(), entity.getBomVersion());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ManufacturingBomDTO dto) {
        ManufacturingBom existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(BomStatus.DRAFT.getCode().equals(existing.getStatus()),
                "只有草稿状态的BOM才能编辑");

        String version = existing.getBomVersion();
        String status = existing.getStatus();

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        existing.setBomVersion(version);
        existing.setStatus(status);
        updateById(existing);

        // 删除旧明细 + 替代料
        deleteItemsAndSubstitutes(id);
        // 递归保存新明细
        if (!CollectionUtils.isEmpty(dto.getItems())) {
            saveItemsRecursive(id, null, 1, dto.getItems());
        }

        log.info("修改制造BOM: {} {}", existing.getBomCode(), version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ManufacturingBom entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        // 级联删除
        deleteItemsAndSubstitutes(id);
        removeById(id);

        log.info("删除制造BOM: {} {}", entity.getBomCode(), entity.getBomVersion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upgrade(Long id) {
        ManufacturingBom source = getById(id);
        AssertUtil.notNull(source, ResultCode.DATA_NOT_EXIST);

        String newVersion = incrementVersion(source.getBomVersion());

        // 检查新版本是否已存在
        long existCount = count(new LambdaQueryWrapper<ManufacturingBom>()
                .eq(ManufacturingBom::getBomCode, source.getBomCode())
                .eq(ManufacturingBom::getBomVersion, newVersion));
        AssertUtil.isFalse(existCount > 0, "版本 " + newVersion + " 已存在");

        // 深拷贝主表
        ManufacturingBom newBom = new ManufacturingBom();
        BeanUtils.copyProperties(source, newBom);
        newBom.setId(null);
        newBom.setBomVersion(newVersion);
        newBom.setStatus(BomStatus.DRAFT.getCode());
        newBom.setUpgradeFromId(source.getId());
        newBom.setCreatedBy(null);
        newBom.setCreatedTime(null);
        newBom.setUpdatedBy(null);
        newBom.setUpdatedTime(null);
        newBom.setDeleted(null);
        save(newBom);

        // 深拷贝明细树 + 替代料（需维护 parentItemId 映射）
        copyItemsRecursive(id, newBom.getId());

        // 记录版本日志
        saveBomVersionLog(newBom.getId(), source.getBomVersion(), newVersion,
                "升级", "从 " + source.getBomVersion() + " 升级到 " + newVersion);

        log.info("BOM版本升级: {} {} -> {}", source.getBomCode(), source.getBomVersion(), newVersion);
        return newBom.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        ManufacturingBom entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(BomStatus.DRAFT.getCode().equals(entity.getStatus()),
                "只有草稿状态才能发布");

        // 检查同编码下是否已有 PUBLISHED 版本，若有则自动停用
        List<ManufacturingBom> publishedList = list(new LambdaQueryWrapper<ManufacturingBom>()
                .eq(ManufacturingBom::getBomCode, entity.getBomCode())
                .eq(ManufacturingBom::getStatus, BomStatus.PUBLISHED.getCode())
                .ne(ManufacturingBom::getId, id));
        for (ManufacturingBom published : publishedList) {
            published.setStatus(BomStatus.DISABLED.getCode());
            updateById(published);
            saveBomVersionLog(published.getId(), null, null, "自动停用",
                    "因版本 " + entity.getBomVersion() + " 发布而自动停用");
        }

        entity.setStatus(BomStatus.PUBLISHED.getCode());
        updateById(entity);

        saveBomVersionLog(id, null, entity.getBomVersion(), "发布",
                "发布BOM " + entity.getBomCode() + " " + entity.getBomVersion());

        log.info("发布制造BOM: {} {}", entity.getBomCode(), entity.getBomVersion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        ManufacturingBom entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(BomStatus.PUBLISHED.getCode().equals(entity.getStatus()),
                "只有已发布状态才能停用");

        entity.setStatus(BomStatus.DISABLED.getCode());
        updateById(entity);

        saveBomVersionLog(id, null, entity.getBomVersion(), "停用",
                "停用BOM " + entity.getBomCode() + " " + entity.getBomVersion());

        log.info("停用制造BOM: {} {}", entity.getBomCode(), entity.getBomVersion());
    }

    @Override
    public List<ManufacturingBomItemVO> getItemTree(Long bomId) {
        // 查全量明细
        List<ManufacturingBomItem> allItems = itemMapper.selectList(
                new LambdaQueryWrapper<ManufacturingBomItem>()
                        .eq(ManufacturingBomItem::getBomId, bomId)
                        .orderByAsc(ManufacturingBomItem::getSequenceNo));

        // 查全量替代料
        List<Long> itemIds = allItems.stream().map(ManufacturingBomItem::getId).toList();
        Map<Long, List<BomSubstituteVO>> substituteMap = new HashMap<>();
        if (!itemIds.isEmpty()) {
            List<BomSubstitute> allSubstitutes = substituteMapper.selectList(
                    new LambdaQueryWrapper<BomSubstitute>()
                            .in(BomSubstitute::getBomItemId, itemIds));
            substituteMap = allSubstitutes.stream()
                    .map(this::toSubstituteVO)
                    .collect(Collectors.groupingBy(BomSubstituteVO::getBomItemId));
        }

        // 转换为 VO 并设置替代料
        Map<Long, List<BomSubstituteVO>> finalSubstituteMap = substituteMap;
        List<ManufacturingBomItemVO> allVOs = allItems.stream()
                .map(item -> {
                    ManufacturingBomItemVO vo = toItemVO(item);
                    vo.setSubstitutes(finalSubstituteMap.getOrDefault(item.getId(), new ArrayList<>()));
                    return vo;
                })
                .toList();

        // 按 parentItemId 分组，构建树
        Map<Long, List<ManufacturingBomItemVO>> childrenMap = allVOs.stream()
                .filter(vo -> vo.getParentItemId() != null)
                .collect(Collectors.groupingBy(ManufacturingBomItemVO::getParentItemId));

        List<ManufacturingBomItemVO> roots = new ArrayList<>();
        for (ManufacturingBomItemVO vo : allVOs) {
            vo.setChildren(childrenMap.getOrDefault(vo.getId(), new ArrayList<>()));
            if (vo.getParentItemId() == null) {
                roots.add(vo);
            }
        }

        return roots;
    }

    // ==================== 私有方法 ====================

    private String incrementVersion(String currentVersion) {
        if (currentVersion != null && currentVersion.startsWith("V")) {
            try {
                int num = Integer.parseInt(currentVersion.substring(1));
                return "V" + (num + 1);
            } catch (NumberFormatException e) {
                // fallback
            }
        }
        return currentVersion + ".1";
    }

    /**
     * 递归保存明细树
     */
    private void saveItemsRecursive(Long bomId, Long parentItemId, int level,
                                     List<ManufacturingBomItemDTO> itemDTOs) {
        if (CollectionUtils.isEmpty(itemDTOs)) {
            return;
        }
        for (ManufacturingBomItemDTO dto : itemDTOs) {
            ManufacturingBomItem item = new ManufacturingBomItem();
            BeanUtils.copyProperties(dto, item);
            item.setBomId(bomId);
            item.setParentItemId(parentItemId);
            item.setLevel(level);
            item.setCreatedTime(LocalDateTime.now());
            item.setUpdatedTime(LocalDateTime.now());
            itemMapper.insert(item);

            // 保存替代料
            if (!CollectionUtils.isEmpty(dto.getSubstitutes())) {
                for (BomSubstituteDTO subDTO : dto.getSubstitutes()) {
                    BomSubstitute substitute = new BomSubstitute();
                    BeanUtils.copyProperties(subDTO, substitute);
                    substitute.setBomItemId(item.getId());
                    substitute.setCreatedTime(LocalDateTime.now());
                    substitute.setUpdatedTime(LocalDateTime.now());
                    substituteMapper.insert(substitute);
                }
            }

            // 递归保存子级
            if (!CollectionUtils.isEmpty(dto.getChildren())) {
                saveItemsRecursive(bomId, item.getId(), level + 1, dto.getChildren());
            }
        }
    }

    /**
     * 深拷贝明细树 + 替代料（需维护 parentItemId 映射）
     */
    private void copyItemsRecursive(Long sourceBomId, Long targetBomId) {
        List<ManufacturingBomItem> sourceItems = itemMapper.selectList(
                new LambdaQueryWrapper<ManufacturingBomItem>()
                        .eq(ManufacturingBomItem::getBomId, sourceBomId)
                        .orderByAsc(ManufacturingBomItem::getLevel)
                        .orderByAsc(ManufacturingBomItem::getSequenceNo));

        // oldId -> newId 映射
        Map<Long, Long> idMapping = new HashMap<>();

        for (ManufacturingBomItem source : sourceItems) {
            ManufacturingBomItem newItem = new ManufacturingBomItem();
            BeanUtils.copyProperties(source, newItem);
            newItem.setId(null);
            newItem.setBomId(targetBomId);
            // 映射父级 ID
            if (source.getParentItemId() != null) {
                newItem.setParentItemId(idMapping.get(source.getParentItemId()));
            }
            newItem.setCreatedTime(LocalDateTime.now());
            newItem.setUpdatedTime(LocalDateTime.now());
            itemMapper.insert(newItem);

            idMapping.put(source.getId(), newItem.getId());

            // 深拷贝替代料
            List<BomSubstitute> substitutes = substituteMapper.selectList(
                    new LambdaQueryWrapper<BomSubstitute>()
                            .eq(BomSubstitute::getBomItemId, source.getId()));
            for (BomSubstitute sub : substitutes) {
                BomSubstitute newSub = new BomSubstitute();
                BeanUtils.copyProperties(sub, newSub);
                newSub.setId(null);
                newSub.setBomItemId(newItem.getId());
                newSub.setCreatedTime(LocalDateTime.now());
                newSub.setUpdatedTime(LocalDateTime.now());
                substituteMapper.insert(newSub);
            }
        }
    }

    /**
     * 删除 BOM 的全部明细和替代料
     */
    private void deleteItemsAndSubstitutes(Long bomId) {
        List<ManufacturingBomItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<ManufacturingBomItem>()
                        .eq(ManufacturingBomItem::getBomId, bomId));
        List<Long> itemIds = items.stream().map(ManufacturingBomItem::getId).toList();

        if (!itemIds.isEmpty()) {
            substituteMapper.delete(new LambdaQueryWrapper<BomSubstitute>()
                    .in(BomSubstitute::getBomItemId, itemIds));
        }
        itemMapper.delete(new LambdaQueryWrapper<ManufacturingBomItem>()
                .eq(ManufacturingBomItem::getBomId, bomId));
    }

    private void saveBomVersionLog(Long bomId, String fromVersion, String toVersion,
                                    String action, String changeSummary) {
        BomVersionLog versionLog = new BomVersionLog();
        versionLog.setBomId(bomId);
        versionLog.setFromVersion(fromVersion);
        versionLog.setToVersion(toVersion);
        versionLog.setAction(action);
        versionLog.setOperator("system"); // TODO: 从 SecurityContext 获取当前用户
        versionLog.setOperatedTime(LocalDateTime.now());
        versionLog.setChangeSummary(changeSummary);
        versionLogMapper.insert(versionLog);
    }

    private ManufacturingBomVO toBomVO(ManufacturingBom entity) {
        ManufacturingBomVO vo = new ManufacturingBomVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private ManufacturingBomItemVO toItemVO(ManufacturingBomItem entity) {
        ManufacturingBomItemVO vo = new ManufacturingBomItemVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private BomSubstituteVO toSubstituteVO(BomSubstitute entity) {
        BomSubstituteVO vo = new BomSubstituteVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
