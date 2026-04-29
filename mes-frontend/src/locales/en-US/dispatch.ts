export default {
  title: 'Dispatch',
  listTitle: 'Dispatch Tasks',
  fields: {
    taskNo: 'Task No.',
    workOrderNo: 'Work Order No.',
    processCode: 'Process Code',
    assignee: 'Assignee',
    assignTime: 'Assign Time',
    status: 'Status',
  },
  statuses: {
    PENDING: 'Pending',
    ASSIGNED: 'Assigned',
    IN_PROGRESS: 'In Progress',
    FINISHED: 'Finished',
    CANCELLED: 'Cancelled',
  },
  actions: {
    assign: 'Assign',
    revoke: 'Revoke',
  },
}
