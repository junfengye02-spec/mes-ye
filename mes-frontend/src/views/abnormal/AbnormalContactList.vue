<template>
  <div class="abnormal-contact-list">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="联系单号">
          <el-input v-model="query.contactNo" placeholder="联系单号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="主题">
          <el-input v-model="query.subject" placeholder="主题" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="事件类别">
          <el-input v-model="query.eventCategory" placeholder="事件类别" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="发现日期">
          <el-date-picker
            v-model="query.discoveryDate"
            type="date"
            placeholder="发现日期"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="table-header">
          <span class="table-title">异常联系单列表</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon> 新增
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="contactNo" label="联系单号" min-width="120" />
        <el-table-column prop="subject" label="主题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="occurStage" label="发生阶段" min-width="100" />
        <el-table-column prop="eventCategory" label="事件类别" min-width="100" />
        <el-table-column prop="productName" label="产品名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="qty" label="数量" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="(getDictType('abnormalStatus', row.status) || undefined) as any">
              {{ getDictLabel('abnormalStatus', row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="discoveryDate" label="发现日期" width="110" />
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'DRAFT'"
              link
              type="primary"
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              link
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              link
              type="primary"
              size="small"
              @click="handleSubmit(row)"
            >
              提交
            </el-button>
            <el-button
              v-if="row.status === 'SUBMITTED'"
              link
              type="primary"
              size="small"
              @click="handleProcess(row)"
            >
              处理
            </el-button>
            <el-button
              v-if="row.status === 'PROCESSING'"
              link
              type="primary"
              size="small"
              @click="handleClose(row)"
            >
              关闭
            </el-button>
            <el-button link type="primary" size="small" @click="handleViewDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑异常联系单' : '新增异常联系单'"
      width="700px"
      destroy-on-close
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <el-form-item label="主题" prop="subject">
          <el-input v-model="form.subject" placeholder="主题" />
        </el-form-item>
        <el-form-item label="发生阶段" prop="occurStage">
          <el-input v-model="form.occurStage" placeholder="发生阶段" />
        </el-form-item>
        <el-form-item label="事件类别" prop="eventCategory">
          <el-input v-model="form.eventCategory" placeholder="事件类别" />
        </el-form-item>
        <el-form-item label="产品事业部" prop="productDivision">
          <el-input v-model="form.productDivision" placeholder="产品事业部" />
        </el-form-item>
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="订单号" />
        </el-form-item>
        <el-form-item label="客户项目" prop="customerProject">
          <el-input v-model="form.customerProject" placeholder="客户项目" />
        </el-form-item>
        <el-form-item label="发起部门" prop="initiateDept">
          <el-input v-model="form.initiateDept" placeholder="发起部门" />
        </el-form-item>
        <el-form-item label="产品型号" prop="productModel">
          <el-input v-model="form.productModel" placeholder="产品型号" />
        </el-form-item>
        <el-form-item label="产品类型" prop="productType">
          <el-input v-model="form.productType" placeholder="产品类型" />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="产品名称" />
        </el-form-item>
        <el-form-item label="发起工序" prop="initiateProcess">
          <el-input v-model="form.initiateProcess" placeholder="发起工序" />
        </el-form-item>
        <el-form-item label="数量" prop="qty">
          <el-input-number v-model="form.qty" :min="0" placeholder="数量" style="width: 100%" />
        </el-form-item>
        <el-form-item label="存储位置" prop="storageLocation">
          <el-input v-model="form.storageLocation" placeholder="存储位置" />
        </el-form-item>
        <el-form-item label="发现日期" prop="discoveryDate">
          <el-date-picker
            v-model="form.discoveryDate"
            type="date"
            placeholder="发现日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="异常描述" prop="abnormalDesc">
          <el-input v-model="form.abnormalDesc" type="textarea" :rows="4" placeholder="异常描述" />
        </el-form-item>
        <el-form-item label="影响排程" prop="affectSchedule">
          <el-switch v-model="form.affectSchedule" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmitForm">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- Detail Drawer -->
    <el-drawer
      v-model="drawerVisible"
      title="异常联系单详情"
      size="50%"
      destroy-on-close
      @close="detailData = null"
    >
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="联系单号">{{ detailData.contactNo }}</el-descriptions-item>
          <el-descriptions-item label="主题">{{ detailData.subject }}</el-descriptions-item>
          <el-descriptions-item label="发生阶段">{{ detailData.occurStage }}</el-descriptions-item>
          <el-descriptions-item label="事件类别">{{ detailData.eventCategory }}</el-descriptions-item>
          <el-descriptions-item label="产品名称">{{ detailData.productName }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ detailData.qty }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="(getDictType('abnormalStatus', detailData.status) || undefined) as any">
              {{ getDictLabel('abnormalStatus', detailData.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发现日期">{{ detailData.discoveryDate }}</el-descriptions-item>
          <el-descriptions-item label="异常描述" :span="2">{{ detailData.abnormalDesc }}</el-descriptions-item>
        </el-descriptions>

        <div class="attachment-section">
          <h4>附件列表</h4>
          <el-table :data="detailData.attachments || []" border size="small">
            <el-table-column prop="fileNo" label="文件编号" width="100" />
            <el-table-column prop="fileName" label="文件名" min-width="150" show-overflow-tooltip />
            <el-table-column prop="responsiblePerson" label="责任人" width="100" />
            <el-table-column prop="team" label="班组" width="100" />
            <el-table-column prop="publishTime" label="发布时间" width="170" />
            <el-table-column prop="signed" label="签署状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.signed ? 'success' : 'info'">
                  {{ row.signed ? '已签署' : '未签署' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="!row.signed"
                  link
                  type="primary"
                  size="small"
                  @click="handleSignAttachment(row)"
                >
                  签署
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getDictLabel, getDictType, getDictList } from '@/utils/dict'
import type { AbnormalContactVO, AbnormalContactDTO } from '@/types/abnormal'
import { abnormalContactApi } from '@/api/abnormal/abnormalContact'

const statusOptions = getDictList('abnormalStatus')

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<AbnormalContactVO[]>([])
const total = ref(0)
const query = reactive({
  contactNo: '',
  subject: '',
  status: '',
  eventCategory: '',
  discoveryDate: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<AbnormalContactDTO & { affectSchedule?: number }>({
  subject: '',
  occurStage: '',
  eventCategory: '',
  productDivision: '',
  orderNo: '',
  customerProject: '',
  initiateDept: '',
  productModel: '',
  productType: '',
  productName: '',
  initiateProcess: '',
  qty: undefined,
  storageLocation: '',
  discoveryDate: '',
  abnormalDesc: '',
  affectSchedule: 0,
})
const formRules: FormRules = {
  subject: [{ required: true, message: '请输入主题', trigger: 'blur' }],
}

const drawerVisible = ref(false)
const detailData = ref<AbnormalContactVO | null>(null)

async function fetchList() {
  loading.value = true
  try {
    const res = await abnormalContactApi.page(query)
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchList()
}

function handleReset() {
  query.contactNo = ''
  query.subject = ''
  query.status = ''
  query.eventCategory = ''
  query.discoveryDate = ''
  query.pageNum = 1
  fetchList()
}

function handleCreate() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: AbnormalContactVO) {
  if (row.status !== 'DRAFT') return
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    subject: row.subject,
    occurStage: row.occurStage,
    eventCategory: row.eventCategory,
    productDivision: row.productDivision,
    orderNo: row.orderNo,
    customerProject: row.customerProject,
    initiateDept: row.initiateDept,
    productModel: row.productModel,
    productType: row.productType,
    productName: row.productName,
    initiateProcess: row.initiateProcess,
    qty: row.qty,
    storageLocation: row.storageLocation,
    discoveryDate: row.discoveryDate,
    abnormalDesc: row.abnormalDesc,
    affectSchedule: row.affectSchedule ?? 0,
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    subject: '',
    occurStage: '',
    eventCategory: '',
    productDivision: '',
    orderNo: '',
    customerProject: '',
    initiateDept: '',
    productModel: '',
    productType: '',
    productName: '',
    initiateProcess: '',
    qty: undefined,
    storageLocation: '',
    discoveryDate: '',
    abnormalDesc: '',
    affectSchedule: 0,
  })
}

async function handleSubmitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value && editId.value) {
        await abnormalContactApi.update(editId.value, form)
        ElMessage.success('更新成功')
      } else {
        await abnormalContactApi.create(form)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchList()
    } finally {
      submitLoading.value = false
    }
  })
}

async function handleDelete(row: AbnormalContactVO) {
  if (row.status !== 'DRAFT') return
  await ElMessageBox.confirm('确定要删除该异常联系单吗？', '提示', {
    type: 'warning',
  })
  await abnormalContactApi.delete(row.id)
  ElMessage.success('删除成功')
  fetchList()
}

async function handleSubmit(row: AbnormalContactVO) {
  await abnormalContactApi.submit(row.id)
  ElMessage.success('提交成功')
  fetchList()
}

async function handleProcess(row: AbnormalContactVO) {
  await abnormalContactApi.process(row.id)
  ElMessage.success('已开始处理')
  fetchList()
}

async function handleClose(row: AbnormalContactVO) {
  await abnormalContactApi.close(row.id)
  ElMessage.success('已关闭')
  fetchList()
}

async function handleViewDetail(row: AbnormalContactVO) {
  const res = await abnormalContactApi.getDetail(row.id)
  detailData.value = res
  if (!detailData.value?.attachments?.length) {
    const attachRes = await abnormalContactApi.getAttachments(row.id)
    if (detailData.value && attachRes) {
      detailData.value.attachments = Array.isArray(attachRes) ? attachRes : (attachRes as any)
    }
  }
  drawerVisible.value = true
}

async function handleSignAttachment(attachment: { id: number }) {
  await abnormalContactApi.signAttachment(attachment.id)
  ElMessage.success('签署成功')
  if (detailData.value) {
    const res = await abnormalContactApi.getDetail(detailData.value.id)
    detailData.value = res
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.abnormal-contact-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.search-card :deep(.el-card__body) {
  padding-bottom: 0;
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.table-title {
  font-weight: 600;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.attachment-section {
  margin-top: 24px;
}
.attachment-section h4 {
  margin-bottom: 12px;
  font-size: 14px;
}
</style>
