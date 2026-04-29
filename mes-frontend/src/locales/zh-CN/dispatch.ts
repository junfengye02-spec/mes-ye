export default {
  title: '生产派工',
  listTitle: '派工任务列表',
  fields: {
    taskNo: '任务号',
    workOrderNo: '工单号',
    processCode: '工序编码',
    assignee: '派工对象',
    assignTime: '派工时间',
    status: '派工状态',
  },
  statuses: {
    PENDING: '待派工',
    ASSIGNED: '已派工',
    IN_PROGRESS: '进行中',
    FINISHED: '已完成',
    CANCELLED: '已撤销',
  },
  actions: {
    assign: '派工',
    revoke: '撤销',
  },
}
