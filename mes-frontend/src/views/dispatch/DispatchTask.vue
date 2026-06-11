<template>
  <div class="dispatch-task">
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card shadow="never" class="resource-card">
          <template #header>
            <span>资源选择</span>
          </template>
          <el-tabs v-model="resourceTab">
            <el-tab-pane label="人员" name="personnel">
              <el-table
                ref="personnelTableRef"
                :data="personnelList"
                border
                stripe
                max-height="400"
                @selection-change="(val: any[]) => (selectedPersonnel = val)"
              >
                <el-table-column type="selection" width="50" align="center" />
                <el-table-column prop="code" label="工号" width="100" />
                <el-table-column prop="name" label="姓名" min-width="100" />
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="设备" name="device">
              <el-table
                ref="deviceTableRef"
                :data="deviceList"
                border
                stripe
                max-height="400"
                @selection-change="(val: any[]) => (selectedDevices = val)"
              >
                <el-table-column type="selection" width="50" align="center" />
                <el-table-column prop="code" label="设备编码" width="120" />
                <el-table-column prop="name" label="设备名称" min-width="120" />
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="班组" name="team">
              <el-table
                ref="teamTableRef"
                :data="teamList"
                border
                stripe
                max-height="400"
                @selection-change="(val: any[]) => (selectedTeams = val)"
              >
                <el-table-column type="selection" width="50" align="center" />
                <el-table-column prop="code" label="班组编码" width="120" />
                <el-table-column prop="name" label="班组名称" min-width="120" />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="table-header">
              <span>{{ t('dispatch.listTitle') }}</span>
              <el-button v-auth="['dispatch:task:create']" type="primary" @click="handleAddTask">
                新增派工
              </el-button>
            </div>
          </template>
          <el-form
            :model="query"
            inline
            class="search-form"
            role="search"
            aria-label="派工任务查询条件"
            @submit.prevent="handleSearch"
          >
            <el-form-item label="订单编号">
              <el-input v-model="query.orderNo" aria-label="订单编号" placeholder="请输入" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="工序号">
              <el-input v-model="query.processNo" aria-label="工序号" placeholder="请输入" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.dispatchStatus" aria-label="状态" placeholder="请选择" clearable style="width: 120px">
                <el-option
                  v-for="item in getDictList('dispatchStatus')"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">
                <el-icon aria-hidden="true"><Search /></el-icon> 查询
              </el-button>
              <el-button @click="handleReset">
                <el-icon aria-hidden="true"><Refresh /></el-icon> 重置
              </el-button>
            </el-form-item>
          </el-form>

          <el-table
            v-loading="loading"
            :data="taskList"
            border
            stripe
            role="region"
            aria-label="派工任务列表"
          >
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="orderNo" label="订单编号" min-width="120" />
            <el-table-column prop="processNo" label="工序号" min-width="100" />
            <el-table-column prop="workName" label="工作名称" min-width="120" />
            <el-table-column prop="projectName" label="项目" min-width="100" />
            <el-table-column prop="planQty" label="计划数量" width="100" align="right" />
            <el-table-column prop="dispatchStatus" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getDictType('dispatchStatus', row.dispatchStatus)">
                  {{ getDictLabel('dispatchStatus', row.dispatchStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="520" fixed="right" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="canEditTask(row)"
                  v-auth="['dispatch:task:update']"
                  type="primary"
                  link
                  size="small"
                  @click="handleEditTask(row)"
                >
                  编辑
                </el-button>
                <el-button
                  v-if="canAssignTask(row)"
                  v-auth="['dispatch:task:assign']"
                  type="primary"
                  link
                  size="small"
                  @click="handleAssignPerson(row)"
                >
                  派人员
                </el-button>
                <el-button
                  v-if="canAssignTask(row)"
                  v-auth="['dispatch:task:assign']"
                  type="primary"
                  link
                  size="small"
                  @click="handleAssignDevice(row)"
                >
                  派设备
                </el-button>
                <el-button
                  v-if="canAssignTask(row)"
                  v-auth="['dispatch:task:assign']"
                  type="primary"
                  link
                  size="small"
                  @click="handleAssignTeam(row)"
                >
                  派班组
                </el-button>
                <el-button
                  v-if="row.dispatchStatus === 'ASSIGNED'"
                  v-auth="['dispatch:task:start']"
                  type="success"
                  link
                  size="small"
                  @click="handleStartTask(row)"
                >
                  开工
                </el-button>
                <el-button
                  v-if="row.dispatchStatus === 'IN_PROGRESS'"
                  v-auth="['dispatch:task:complete']"
                  type="success"
                  link
                  size="small"
                  @click="handleOpenCompleteDialog(row)"
                >
                  完工
                </el-button>
                <el-button
                  v-if="canCancelTask(row)"
                  v-auth="['dispatch:task:cancel']"
                  type="danger"
                  link
                  size="small"
                  @click="handleCancelTask(row)"
                >
                  撤销任务
                </el-button>
                <el-button type="primary" link size="small" @click="handleViewAssignments(row)">
                  查看派工
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <nav class="pagination-wrapper" aria-label="派工任务分页">
            <el-pagination
              v-model:current-page="query.pageNum"
              v-model:page-size="query.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadList"
              @current-change="loadList"
            />
          </nav>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="assignmentsVisible" title="派工明细" width="700px">
      <el-table :data="assignments" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="assignType" label="派工类型" width="100" />
        <el-table-column prop="assigneeName" label="资源名称" min-width="120" />
        <el-table-column prop="assigneeCode" label="资源编码" width="120" />
        <el-table-column prop="assignedQty" label="分派数量" width="100" align="right" />
        <el-table-column prop="status" label="状态" width="80" />
        <el-table-column prop="assignedBy" label="派工人" width="100" />
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'ACTIVE'"
              type="danger"
              link
              size="small"
              @click="handleRevoke(row)"
            >
              撤销
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="assignDialogVisible" :title="assignDialogTitle" width="400px">
          <el-form label-width="80px">
            <el-form-item label="选择资源">
              <el-select v-model="assignForm.assigneeId" placeholder="请选择" style="width: 100%" @change="onAssigneeChange">
                <el-option
                  v-for="item in currentResourceList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="分派数量">
              <el-input-number v-model="assignForm.assignedQty" :min="0" style="width: 100%" />
            </el-form-item>
            <el-form-item label="单位">
              <el-input v-model="assignForm.qtyUnit" placeholder="请输入单位" />
            </el-form-item>
          </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="handleAssignSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="taskDialogVisible" :title="taskDialogTitle" width="720px" destroy-on-close @close="handleTaskDialogClose">
      <el-form ref="taskFormRef" :model="taskForm" :rules="taskRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工单ID" prop="workOrderId">
              <el-input-number v-model="taskForm.workOrderId" :min="1" :disabled="isTaskEditing" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工作清单ID">
              <el-input-number v-model="taskForm.workOrderTaskId" :min="1" :disabled="isTaskEditing" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订单编号" prop="orderNo">
              <el-input v-model="taskForm.orderNo" placeholder="请输入订单编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工序号" prop="processNo">
              <el-input v-model="taskForm.processNo" placeholder="请输入工序号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工作名称" prop="workName">
              <el-input v-model="taskForm.workName" placeholder="请输入工作名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目">
              <el-input v-model="taskForm.projectName" placeholder="请输入项目" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划工作中心">
              <el-input-number v-model="taskForm.planWorkCenterId" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="序列号">
              <el-input v-model="taskForm.serialNo" placeholder="请输入序列号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划数量" prop="planQty">
              <el-input-number v-model="taskForm.planQty" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="qtyUnit">
              <el-input v-model="taskForm.qtyUnit" placeholder="请输入单位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划开始时间">
              <el-date-picker
                v-model="taskForm.planStartTime"
                type="datetime"
                placeholder="选择时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划结束时间">
              <el-date-picker
                v-model="taskForm.planEndTime"
                type="datetime"
                placeholder="选择时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="taskSubmitLoading" @click="handleTaskSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="completeDialogVisible" title="派工完工" width="520px" destroy-on-close @close="handleCompleteDialogClose">
      <el-form ref="completeFormRef" :model="completeForm" :rules="completeRules" label-width="100px">
        <el-form-item label="实际开工时间">
          <el-date-picker
            v-model="completeForm.actualStartTime"
            type="datetime"
            placeholder="选择时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="实际完工时间" prop="actualEndTime">
          <el-date-picker
            v-model="completeForm.actualEndTime"
            type="datetime"
            placeholder="选择时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="实际数量" prop="actualQty">
          <el-input-number v-model="completeForm.actualQty" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="质量结果" prop="qualityResult">
          <el-select v-model="completeForm.qualityResult" placeholder="请选择" style="width: 100%">
            <el-option label="合格" value="PASS" />
            <el-option label="不合格" value="FAIL" />
            <el-option label="不适用" value="NA" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="completeForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="completeSubmitting" @click="handleCompleteSubmit">
          确定完工
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { getDictList, getDictLabel, getDictType } from '@/utils/dict'
import type {
  DispatchTaskVO,
  DispatchTaskQuery,
  DispatchAssignmentVO,
  DispatchTaskCreateDTO,
  DispatchTaskUpdateDTO,
  DispatchTaskCompleteDTO,
} from '@/types/dispatch'
import { dispatchTaskApi } from '@/api/dispatch/dispatchTask'

const { t } = useI18n()
const loading = ref(false)
const taskList = ref<DispatchTaskVO[]>([])
const total = ref(0)
const query = reactive<DispatchTaskQuery>({
  orderNo: '',
  processNo: '',
  dispatchStatus: undefined,
  pageNum: 1,
  pageSize: 20,
})

const resourceTab = ref('personnel')
const personnelTableRef = ref()
const deviceTableRef = ref()
const teamTableRef = ref()

const personnelList = ref([
  { id: 1, code: 'P001', name: '张三' },
  { id: 2, code: 'P002', name: '李四' },
  { id: 3, code: 'P003', name: '王五' },
  { id: 4, code: 'P004', name: '赵六' },
])
const deviceList = ref([
  { id: 1, code: 'DEV001', name: '数控机床A' },
  { id: 2, code: 'DEV002', name: '数控机床B' },
  { id: 3, code: 'DEV003', name: '冲压机' },
  { id: 4, code: 'DEV004', name: '焊接机器人' },
])
const teamList = ref([
  { id: 1, code: 'T001', name: '一班' },
  { id: 2, code: 'T002', name: '二班' },
  { id: 3, code: 'T003', name: '三班' },
])

const selectedPersonnel = ref<any[]>([])
const selectedDevices = ref<any[]>([])
const selectedTeams = ref<any[]>([])

const assignmentsVisible = ref(false)
const assignments = ref<DispatchAssignmentVO[]>([])
let currentTaskId: number | null = null

const assignDialogVisible = ref(false)
const assignDialogTitle = ref('')
const assignLoading = ref(false)
let assignType: 'person' | 'device' | 'team' = 'person'
let assignTargetTaskId: number | null = null
const assignForm = reactive({
  assigneeId: 0,
  assigneeCode: '',
  assigneeName: '',
  assignedQty: undefined as number | undefined,
  qtyUnit: 'PCS',
})

const taskDialogVisible = ref(false)
const taskDialogTitle = ref('新增派工')
const taskFormRef = ref<FormInstance>()
const taskSubmitLoading = ref(false)
const taskEditingId = ref<number | null>(null)
const taskForm = reactive<DispatchTaskCreateDTO & { id?: number }>({
  workOrderId: 0,
  workOrderTaskId: undefined,
  orderNo: '',
  processNo: '',
  workName: '',
  planWorkCenterId: undefined,
  serialNo: '',
  projectName: '',
  planQty: 1,
  qtyUnit: 'PCS',
  planStartTime: '',
  planEndTime: '',
})
const taskRules: FormRules = {
  workOrderId: [{ required: true, message: '请输入工单ID', trigger: 'blur' }],
  orderNo: [{ required: true, message: '请输入订单编号', trigger: 'blur' }],
  processNo: [{ required: true, message: '请输入工序号', trigger: 'blur' }],
  workName: [{ required: true, message: '请输入工作名称', trigger: 'blur' }],
  planQty: [{ required: true, message: '请输入计划数量', trigger: 'change' }],
  qtyUnit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
}

const completeDialogVisible = ref(false)
const completeFormRef = ref<FormInstance>()
const completeSubmitting = ref(false)
const completingTask = ref<DispatchTaskVO | null>(null)
const completeForm = reactive<DispatchTaskCompleteDTO>({
  actualStartTime: '',
  actualEndTime: '',
  actualQty: 0,
  qualityResult: 'PASS',
  remark: '',
})
const completeRules: FormRules = {
  actualEndTime: [{ required: true, message: '请选择实际完工时间', trigger: 'change' }],
  actualQty: [{ required: true, message: '请输入实际数量', trigger: 'change' }],
  qualityResult: [{ required: true, message: '请选择质量结果', trigger: 'change' }],
}

const isTaskEditing = computed(() => taskEditingId.value !== null)

const currentResourceList = computed(() => {
  if (assignType === 'person') return personnelList.value
  if (assignType === 'device') return deviceList.value
  return teamList.value
})

function onAssigneeChange(id: number) {
  const list = currentResourceList.value
  const item = list.find(r => r.id === id)
  if (item) {
    assignForm.assigneeCode = item.code
    assignForm.assigneeName = item.name
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await dispatchTaskApi.page(query)
    taskList.value = res?.list ?? []
    total.value = res?.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  query.orderNo = ''
  query.processNo = ''
  query.dispatchStatus = undefined
  query.pageNum = 1
  loadList()
}

function openAssignDialog(row: DispatchTaskVO, type: 'person' | 'device' | 'team', title: string) {
  assignTargetTaskId = row.id
  assignType = type
  assignDialogTitle.value = title
  assignForm.assigneeId = 0
  assignForm.assigneeCode = ''
  assignForm.assigneeName = ''
  assignForm.assignedQty = row.planQty
  assignForm.qtyUnit = row.qtyUnit || 'PCS'
  const list = type === 'person' ? personnelList.value : type === 'device' ? deviceList.value : teamList.value
  const first = list[0]
  if (first) {
    assignForm.assigneeId = first.id
    assignForm.assigneeCode = first.code
    assignForm.assigneeName = first.name
  }
  assignDialogVisible.value = true
}

function handleAssignPerson(row: DispatchTaskVO) { openAssignDialog(row, 'person', '派人员') }
function handleAssignDevice(row: DispatchTaskVO) { openAssignDialog(row, 'device', '派设备') }
function handleAssignTeam(row: DispatchTaskVO) { openAssignDialog(row, 'team', '派班组') }

async function handleAssignSubmit() {
  if (!assignTargetTaskId || !assignForm.assigneeId) {
    ElMessage.warning('请选择资源')
    return
  }
  assignLoading.value = true
  try {
    await dispatchTaskApi.assign({
      taskId: assignTargetTaskId,
      assignType: assignType === 'person' ? 'PERSON' : assignType === 'device' ? 'DEVICE' : 'TEAM',
      assigneeIds: [assignForm.assigneeId],
      assigneeCodes: [assignForm.assigneeCode],
      assigneeNames: assignForm.assigneeName ? [assignForm.assigneeName] : undefined,
      assignedQty: assignForm.assignedQty,
      qtyUnit: assignForm.qtyUnit,
    })
    ElMessage.success('派工成功')
    assignDialogVisible.value = false
    loadList()
  } finally {
    assignLoading.value = false
  }
}

async function handleViewAssignments(row: DispatchTaskVO) {
  currentTaskId = row.id
  try {
    const res = await dispatchTaskApi.getAssignments(row.id)
    assignments.value = Array.isArray(res) ? res : []
    assignmentsVisible.value = true
  } catch {
    assignments.value = []
    assignmentsVisible.value = true
  }
}

function canAssignTask(row: DispatchTaskVO) {
  return row.dispatchStatus === 'UNASSIGNED' || row.dispatchStatus === 'REVOKED'
}

function canEditTask(row: DispatchTaskVO) {
  return row.dispatchStatus === 'UNASSIGNED' || row.dispatchStatus === 'ASSIGNED'
}

function canCancelTask(row: DispatchTaskVO) {
  return row.dispatchStatus !== 'COMPLETED' && row.dispatchStatus !== 'CANCELLED'
}

function resetTaskForm() {
  Object.assign(taskForm, {
    workOrderId: 0,
    workOrderTaskId: undefined,
    orderNo: '',
    processNo: '',
    workName: '',
    planWorkCenterId: undefined,
    serialNo: '',
    projectName: '',
    planQty: 1,
    qtyUnit: 'PCS',
    planStartTime: '',
    planEndTime: '',
  })
}

function handleAddTask() {
  taskDialogTitle.value = '新增派工'
  taskEditingId.value = null
  resetTaskForm()
  taskDialogVisible.value = true
}

function handleEditTask(row: DispatchTaskVO) {
  taskDialogTitle.value = '编辑派工'
  taskEditingId.value = row.id
  resetTaskForm()
  Object.assign(taskForm, {
    id: row.id,
    workOrderId: row.workOrderId || 0,
    workOrderTaskId: row.workOrderTaskId,
    orderNo: row.orderNo || '',
    processNo: row.processNo || '',
    workName: row.workName || '',
    planWorkCenterId: row.planWorkCenterId,
    serialNo: row.serialNo || '',
    projectName: row.projectName || '',
    planQty: row.planQty || 1,
    qtyUnit: row.qtyUnit || 'PCS',
    planStartTime: row.planStartTime || '',
    planEndTime: row.planEndTime || '',
  })
  taskDialogVisible.value = true
}

function handleTaskDialogClose() {
  taskFormRef.value?.resetFields()
}

async function handleTaskSubmit() {
  await taskFormRef.value?.validate()
  taskSubmitLoading.value = true
  try {
    if (taskEditingId.value) {
      const payload: DispatchTaskUpdateDTO = {
        id: taskEditingId.value,
        orderNo: taskForm.orderNo,
        processNo: taskForm.processNo,
        workName: taskForm.workName,
        planWorkCenterId: taskForm.planWorkCenterId,
        serialNo: taskForm.serialNo,
        projectName: taskForm.projectName,
        planQty: taskForm.planQty,
        qtyUnit: taskForm.qtyUnit,
        planStartTime: taskForm.planStartTime,
        planEndTime: taskForm.planEndTime,
      }
      await dispatchTaskApi.update(payload)
      ElMessage.success('编辑成功')
    } else {
      await dispatchTaskApi.create({
        workOrderId: taskForm.workOrderId,
        workOrderTaskId: taskForm.workOrderTaskId,
        orderNo: taskForm.orderNo,
        processNo: taskForm.processNo,
        workName: taskForm.workName,
        planWorkCenterId: taskForm.planWorkCenterId,
        serialNo: taskForm.serialNo,
        projectName: taskForm.projectName,
        planQty: taskForm.planQty,
        qtyUnit: taskForm.qtyUnit,
        planStartTime: taskForm.planStartTime,
        planEndTime: taskForm.planEndTime,
      })
      ElMessage.success('新增成功')
    }
    taskDialogVisible.value = false
    loadList()
  } finally {
    taskSubmitLoading.value = false
  }
}

function handleStartTask(row: DispatchTaskVO) {
  ElMessageBox.confirm('确定要开工该派工任务吗？', '提示', {
    type: 'info',
  }).then(async () => {
    await dispatchTaskApi.start(row.id)
    ElMessage.success('开工成功')
    loadList()
  }).catch(() => {})
}

function handleOpenCompleteDialog(row: DispatchTaskVO) {
  completingTask.value = row
  completeForm.actualStartTime = row.actualStartTime || row.planStartTime || ''
  completeForm.actualEndTime = row.actualEndTime || ''
  completeForm.actualQty = row.planQty || 0
  completeForm.qualityResult = (row.qualityResult as 'PASS' | 'FAIL' | 'NA') || 'PASS'
  completeForm.remark = ''
  completeDialogVisible.value = true
}

function handleCompleteDialogClose() {
  completeFormRef.value?.resetFields()
  completingTask.value = null
}

async function handleCompleteSubmit() {
  await completeFormRef.value?.validate()
  if (!completingTask.value) return
  completeSubmitting.value = true
  try {
    await dispatchTaskApi.complete(completingTask.value.id, {
      actualStartTime: completeForm.actualStartTime || undefined,
      actualEndTime: completeForm.actualEndTime,
      actualQty: completeForm.actualQty,
      qualityResult: completeForm.qualityResult,
      remark: completeForm.remark,
    })
    ElMessage.success('完工成功')
    completeDialogVisible.value = false
    loadList()
  } finally {
    completeSubmitting.value = false
  }
}

function handleCancelTask(row: DispatchTaskVO) {
  ElMessageBox.prompt('请输入撤销原因', '撤销任务', {
    confirmButtonText: '确认撤销',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '撤销原因不能为空',
    type: 'warning',
  }).then(async ({ value }) => {
    await dispatchTaskApi.cancel(row.id, value)
    ElMessage.success('撤销成功')
    loadList()
  }).catch(() => {})
}

function handleRevoke(row: DispatchAssignmentVO) {
  ElMessageBox.prompt('请输入撤销原因', '撤销派工', {
    confirmButtonText: '确认撤销',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '撤销原因不能为空',
    type: 'warning',
  }).then(async ({ value: reason }) => {
    await dispatchTaskApi.unassign(row.id, reason)
    ElMessage.success('撤销成功')
    if (currentTaskId) {
      const res = await dispatchTaskApi.getAssignments(currentTaskId)
      assignments.value = Array.isArray(res) ? res : []
    }
    loadList()
  }).catch(() => {})
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.dispatch-task {
  padding: 16px;
}
.resource-card {
  margin-bottom: 16px;
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-form {
  margin-bottom: 16px;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
