<template>
  <div class="work-order-detail">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="header-actions">
          <el-button @click="handleBack">
            <el-icon><ArrowLeft /></el-icon> 返回
          </el-button>
        </div>
      </template>

      <div v-if="detail" class="detail-content">
        <div class="header-card">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="工单号">{{ detail.workOrderNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getDictType('workOrderStatus', detail.status || '') as any">
                {{ getDictLabel('workOrderStatus', detail.status || '') }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="产品编码">{{ detail.productCode }}</el-descriptions-item>
            <el-descriptions-item label="产品名称">{{ detail.productName }}</el-descriptions-item>
            <el-descriptions-item label="计划数量">{{ detail.planQty }}</el-descriptions-item>
            <el-descriptions-item label="计划时间">
              {{ detail.planStartTime }} ~ {{ detail.planEndTime }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane label="工作清单" name="tasks">
            <el-table :data="detail.tasks || []" border stripe>
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="taskNo" label="任务编号" min-width="120" />
              <el-table-column prop="taskName" label="任务名称" min-width="150" />
              <el-table-column prop="planWorkCenterId" label="计划工作中心" width="120" />
              <el-table-column prop="planQty" label="计划数量" width="100" align="right" />
              <el-table-column prop="status" label="状态" width="100" />
              <el-table-column prop="sequenceNo" label="工序号" width="80" align="center" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="输入物料" name="inputMaterials">
            <el-table :data="detail.inputMaterials || []" border stripe>
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="materialCode" label="物料编码" min-width="120" />
              <el-table-column prop="materialName" label="物料名称" min-width="150" />
              <el-table-column prop="requiredQty" label="需求数量" width="100" align="right" />
              <el-table-column prop="issuedQty" label="已发数量" width="100" align="right" />
              <el-table-column prop="qtyUnit" label="单位" width="80" align="center" />
              <el-table-column prop="batchNo" label="批次号" width="120" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="输出物料" name="outputMaterials">
            <el-table :data="detail.outputMaterials || []" border stripe>
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="materialCode" label="物料编码" min-width="120" />
              <el-table-column prop="materialName" label="物料名称" min-width="150" />
              <el-table-column prop="outputQty" label="产出数量" width="100" align="right" />
              <el-table-column prop="qtyUnit" label="单位" width="80" align="center" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="检验项目" name="qualityItems">
            <el-table :data="detail.qualityItems || []" border stripe>
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="qualityItemCode" label="检验项编码" min-width="120" />
              <el-table-column prop="qualityItemName" label="检验项名称" min-width="150" />
              <el-table-column prop="requirement" label="要求" min-width="200" />
              <el-table-column prop="status" label="状态" width="100" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="约束关系" name="constraints">
            <el-table :data="detail.constraints || []" border stripe>
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="constraintType" label="约束类型" min-width="120" />
              <el-table-column prop="relatedWorkOrderId" label="关联工单ID" width="120" />
              <el-table-column prop="remark" label="备注" min-width="200" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="供应计划" name="supplyPlans">
            <el-table :data="detail.supplyPlans || []" border stripe>
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="demandPlanNo" label="需求计划号" min-width="120" />
              <el-table-column prop="supplyPlanNo" label="供应计划号" min-width="120" />
              <el-table-column prop="supplyQty" label="供应数量" width="100" align="right" />
              <el-table-column prop="qtyUnit" label="单位" width="80" align="center" />
              <el-table-column prop="completedQty" label="完成数量" width="100" align="right" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="文档附件" name="attachments">
            <el-table :data="detail.attachments || []" border stripe>
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="fileName" label="文件名" min-width="150" />
              <el-table-column prop="fileType" label="文件类型" width="100" />
              <el-table-column prop="fileSizeKb" label="大小(KB)" width="100" align="right" />
              <el-table-column prop="fileUrl" label="文件链接" min-width="200">
                <template #default="{ row }">
                  <el-link v-if="row.fileUrl" type="primary" :href="row.fileUrl" target="_blank">
                    {{ row.fileUrl }}
                  </el-link>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="fileModifiedTime" label="修改时间" width="170" />
              <el-table-column prop="modifiedBy" label="修改人" width="100" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getDictLabel, getDictType } from '@/utils/dict'
import type { WorkOrderVO } from '@/types/workorder'
import { workOrderApi } from '@/api/workorder/workOrder'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const loading = ref(false)
const detail = ref<WorkOrderVO | null>(null)
const activeTab = ref('tasks')

async function loadDetail() {
  if (isNaN(id) || id <= 0) {
    ElMessage.error('无效的工单ID')
    router.push('/workorder/list')
    return
  }
  loading.value = true
  try {
    const res = await workOrderApi.getDetail(id)
    detail.value = res ?? null
  } catch {
    ElMessage.error('加载工单详情失败')
    detail.value = null
  } finally {
    loading.value = false
  }
}

function handleBack() {
  router.push('/workorder/list')
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.work-order-detail {
  padding: 16px;
}
.header-actions {
  margin-bottom: 8px;
}
.header-card {
  margin-bottom: 16px;
}
.detail-tabs {
  margin-top: 16px;
}
</style>
