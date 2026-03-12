package com.mes.dispatch.service;

import com.mes.dispatch.domain.dto.DispatchAssignDTO;
import com.mes.dispatch.domain.vo.DispatchAssignmentVO;

import java.util.List;

/**
 * 派工分配 Service
 */
public interface IDispatchAssignmentService {

    /** 人员分派 */
    void assignPerson(Long taskId, DispatchAssignDTO dto);

    /** 设备分派 */
    void assignDevice(Long taskId, DispatchAssignDTO dto);

    /** 班组分派 */
    void assignTeam(Long taskId, DispatchAssignDTO dto);

    /** 撤销分派 */
    void revoke(Long assignmentId, String reason);

    /** 查询任务的所有分配记录 */
    List<DispatchAssignmentVO> listByTaskId(Long taskId);
}
