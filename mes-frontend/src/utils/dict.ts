export interface DictItem {
  label: string
  value: string | number
  type?: string
}

const dictMap: Record<string, DictItem[]> = {
  workOrderStatus: [
    { label: '已创建', value: 'CREATED', type: 'info' },
    { label: '已下发', value: 'RELEASED', type: 'primary' },
    { label: '执行中', value: 'IN_PROGRESS', type: 'warning' },
    { label: '已完工', value: 'COMPLETED', type: 'success' },
    { label: '强制完工', value: 'FORCE_COMPLETED', type: 'danger' },
    { label: '已关闭', value: 'CLOSED', type: 'info' },
  ],
  orderPlanStatus: [
    { label: '已创建', value: 'CREATED', type: 'info' },
    { label: '已下达', value: 'RELEASED', type: 'primary' },
    { label: '已完成', value: 'COMPLETED', type: 'success' },
    { label: '已终止', value: 'TERMINATED', type: 'danger' },
  ],
  productionPlanStatus: [
    { label: '已创建', value: 'CREATED', type: 'info' },
    { label: '已下达', value: 'RELEASED', type: 'primary' },
  ],
  abnormalStatus: [
    { label: '草稿', value: 'DRAFT', type: 'info' },
    { label: '已提交', value: 'SUBMITTED', type: 'primary' },
    { label: '处理中', value: 'PROCESSING', type: 'warning' },
    { label: '已关闭', value: 'CLOSED', type: 'success' },
  ],
  syncStatus: [
    { label: '待处理', value: 'PENDING', type: 'info' },
    { label: '处理中', value: 'PROCESSING', type: 'warning' },
    { label: '已同步', value: 'SYNCED', type: 'success' },
    { label: '失败', value: 'FAILED', type: 'danger' },
  ],
  syncType: [
    { label: '工单', value: 'WORKORDER' },
    { label: '库存', value: 'INVENTORY' },
    { label: '质量', value: 'QUALITY' },
    { label: '异常', value: 'ABNORMAL' },
    { label: '外协', value: 'OUTSOURCE' },
    { label: '转厂', value: 'TRANSFER' },
  ],
  instructionStatus: [
    { label: '草稿', value: 'DRAFT', type: 'info' },
    { label: '已发布', value: 'PUBLISHED', type: 'success' },
    { label: '已停用', value: 'DISABLED', type: 'danger' },
  ],
  bomStatus: [
    { label: '草稿', value: 'DRAFT', type: 'info' },
    { label: '已发布', value: 'PUBLISHED', type: 'success' },
    { label: '已停用', value: 'DISABLED', type: 'danger' },
  ],
  yesNo: [
    { label: '是', value: 1, type: 'success' },
    { label: '否', value: 0, type: 'info' },
  ],
  requestType: [
    { label: '新制品', value: 'NEW', type: 'success' },
    { label: '维修品', value: 'REPAIR', type: 'warning' },
    { label: '不可维修品', value: 'UNREPAIRABLE', type: 'danger' },
  ],
  receiptType: [
    { label: '新制品', value: 'NEW', type: 'success' },
    { label: '维修品', value: 'REPAIR', type: 'warning' },
    { label: '不可维修品', value: 'UNREPAIRABLE', type: 'danger' },
  ],
  deliverySignStatus: [
    { label: '待签收', value: 'PENDING', type: 'warning' },
    { label: '已签收', value: 'SIGNED', type: 'success' },
  ],
  syncDirection: [
    { label: '上行', value: 'UPSTREAM', type: 'primary' },
    { label: '下行', value: 'DOWNSTREAM', type: 'success' },
  ],
  requisitionOrderStatus: [
    { label: '待领料', value: 'PENDING', type: 'info' },
    { label: '部分领料', value: 'PARTIAL', type: 'warning' },
    { label: '已领料', value: 'ISSUED', type: 'success' },
  ],
  requisitionStatus: [
    { label: '待领料', value: 'PENDING', type: 'info' },
    { label: '部分领料', value: 'PARTIAL', type: 'warning' },
    { label: '已领料', value: 'ISSUED', type: 'success' },
    { label: '已关闭', value: 'CLOSED', type: 'info' },
  ],
  receiptRequestStatus: [
    { label: '待处理', value: 'PENDING', type: 'info' },
    { label: '已入库', value: 'RECEIVED', type: 'success' },
    { label: '已关闭', value: 'CLOSED', type: 'info' },
  ],
  receiptStatus: [
    { label: '待入库', value: 'PENDING', type: 'info' },
    { label: '已入库', value: 'RECEIVED', type: 'success' },
  ],
  materialReturnStatus: [
    { label: '待退料', value: 'PENDING', type: 'info' },
    { label: '已退料', value: 'RETURNED', type: 'success' },
  ],
}

export function getDictList(dictType: string): DictItem[] {
  return dictMap[dictType] || []
}

export function getDictLabel(dictType: string, value: string | number | undefined): string {
  if (value === undefined) return ''
  const items = dictMap[dictType] || []
  return items.find(i => i.value === value)?.label || String(value)
}

export function getDictType(dictType: string, value: string | number | undefined): 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  if (value === undefined) return 'info'
  const items = dictMap[dictType] || []
  return (items.find(i => i.value === value)?.type || 'info') as 'success' | 'warning' | 'info' | 'danger' | 'primary'
}
