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
            <span>{{ t('dispatch.listTitle') }}</span>
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
              <el-input v-model="query.orderNo" placeholder="请输入" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="工序号">
              <el-input v-model="query.processNo" placeholder="请输入" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.dispatchStatus" placeholder="请选择" clearable style="width: 120px">
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
            <el-table-column label="操作" width="320" fixed="right" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleAssignPerson(row)">
                  派人员
                </el-button>
                <el-button type="primary" link size="small" @click="handleAssignDevice(row)">
                  派设备
                </el-button>
                <el-button type="primary" link size="small" @click="handleAssignTeam(row)">
                  派班组
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
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="handleAssignSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { getDictList, getDictLabel, getDictType } from '@/utils/dict'
import type { DispatchTaskVO, DispatchTaskQuery, DispatchAssignmentVO, DispatchAssignDTO } from '@/types/dispatch'
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
const assignForm = reactive<DispatchAssignDTO>({
  assigneeId: 0,
  assigneeCode: '',
  assigneeName: '',
})

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
    const dto: DispatchAssignDTO = { ...assignForm }
    if (assignType === 'person') {
      await dispatchTaskApi.assignPerson(assignTargetTaskId, dto)
    } else if (assignType === 'device') {
      await dispatchTaskApi.assignDevice(assignTargetTaskId, dto)
    } else {
      await dispatchTaskApi.assignTeam(assignTargetTaskId, dto)
    }
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

function handleRevoke(row: DispatchAssignmentVO) {
  ElMessageBox.prompt('请输入撤销原因', '撤销派工', {
    confirmButtonText: '确认撤销',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '撤销原因不能为空',
    type: 'warning',
  }).then(async ({ value: reason }) => {
    await dispatchTaskApi.revokeAssignment(row.id, reason)
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
.search-form {
  margin-bottom: 16px;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
