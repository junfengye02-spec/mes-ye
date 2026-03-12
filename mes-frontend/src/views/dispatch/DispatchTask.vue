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
            <span>派工任务列表</span>
          </template>
          <el-form :model="query" inline class="search-form" @submit.prevent="handleSearch">
            <el-form-item label="任务号">
              <el-input v-model="query.taskNo" placeholder="请输入" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="工单号">
              <el-input v-model="query.workOrderNo" placeholder="请输入" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.status" placeholder="请选择" clearable style="width: 120px">
                <el-option label="待派工" value="PENDING" />
                <el-option label="已派工" value="ASSIGNED" />
                <el-option label="进行中" value="IN_PROGRESS" />
                <el-option label="已完成" value="COMPLETED" />
              </el-select>
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

          <el-table v-loading="loading" :data="taskList" border stripe>
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="taskNo" label="任务号" min-width="120" />
            <el-table-column prop="workOrderNo" label="工单号" min-width="120" />
            <el-table-column prop="taskName" label="任务名称" min-width="120" />
            <el-table-column prop="processName" label="工序名称" min-width="100" />
            <el-table-column prop="planQty" label="计划数量" width="100" align="right" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="assignedPersonName" label="派工人员" width="100" />
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

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="query.pageNum"
              v-model:page-size="query.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadList"
              @current-change="loadList"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="assignmentsVisible" title="派工明细" width="600px">
      <el-table :data="assignments" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="assignmentType" label="派工类型" width="100" />
        <el-table-column prop="resourceName" label="资源名称" min-width="120" />
        <el-table-column prop="resourceCode" label="资源编码" width="120" />
        <el-table-column prop="status" label="状态" width="80" />
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleRevoke(row)">
              撤销
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="assignPersonVisible" title="派人员" width="400px">
      <el-form label-width="80px">
        <el-form-item label="选择人员">
          <el-select v-model="assignPersonForm.personId" placeholder="请选择人员" style="width: 100%">
            <el-option
              v-for="p in personnelList"
              :key="p.id"
              :label="p.name"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignPersonVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="handleAssignPersonSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDeviceVisible" title="派设备" width="400px">
      <el-form label-width="80px">
        <el-form-item label="选择设备">
          <el-select v-model="assignDeviceForm.deviceId" placeholder="请选择设备" style="width: 100%">
            <el-option
              v-for="d in deviceList"
              :key="d.id"
              :label="d.name"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDeviceVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="handleAssignDeviceSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignTeamVisible" title="派班组" width="400px">
      <el-form label-width="80px">
        <el-form-item label="选择班组">
          <el-select v-model="assignTeamForm.teamId" placeholder="请选择班组" style="width: 100%">
            <el-option
              v-for="t in teamList"
              :key="t.id"
              :label="t.name"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignTeamVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="handleAssignTeamSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import type { DispatchTaskVO, DispatchTaskQuery, DispatchAssignmentVO } from '@/types/dispatch'
import { dispatchTaskApi } from '@/api/dispatch/dispatchTask'

const loading = ref(false)
const taskList = ref<DispatchTaskVO[]>([])
const total = ref(0)
const query = reactive<DispatchTaskQuery>({
  taskNo: '',
  workOrderNo: '',
  status: '',
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

const assignPersonVisible = ref(false)
const assignDeviceVisible = ref(false)
const assignTeamVisible = ref(false)
const assignLoading = ref(false)
const assignPersonForm = reactive({ personId: undefined as number | undefined })
const assignDeviceForm = reactive({ deviceId: undefined as number | undefined })
const assignTeamForm = reactive({ teamId: undefined as number | undefined })
let assignTargetTaskId: number | null = null

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
  query.taskNo = ''
  query.workOrderNo = ''
  query.status = ''
  query.pageNum = 1
  loadList()
}

function handleAssignPerson(row: DispatchTaskVO) {
  assignTargetTaskId = row.id
  assignPersonForm.personId = personnelList.value[0]?.id
  assignPersonVisible.value = true
}

function handleAssignDevice(row: DispatchTaskVO) {
  assignTargetTaskId = row.id
  assignDeviceForm.deviceId = deviceList.value[0]?.id
  assignDeviceVisible.value = true
}

function handleAssignTeam(row: DispatchTaskVO) {
  assignTargetTaskId = row.id
  assignTeamForm.teamId = teamList.value[0]?.id
  assignTeamVisible.value = true
}

async function handleAssignPersonSubmit() {
  if (!assignTargetTaskId || !assignPersonForm.personId) {
    ElMessage.warning('请选择人员')
    return
  }
  assignLoading.value = true
  try {
    const person = personnelList.value.find(p => p.id === assignPersonForm.personId)
    await dispatchTaskApi.assignPerson(assignTargetTaskId, {
      personId: assignPersonForm.personId,
      personName: person?.name,
    })
    ElMessage.success('派工成功')
    assignPersonVisible.value = false
    loadList()
  } finally {
    assignLoading.value = false
  }
}

async function handleAssignDeviceSubmit() {
  if (!assignTargetTaskId || !assignDeviceForm.deviceId) {
    ElMessage.warning('请选择设备')
    return
  }
  assignLoading.value = true
  try {
    const device = deviceList.value.find(d => d.id === assignDeviceForm.deviceId)
    await dispatchTaskApi.assignDevice(assignTargetTaskId, {
      deviceId: assignDeviceForm.deviceId,
      deviceName: device?.name,
    })
    ElMessage.success('派工成功')
    assignDeviceVisible.value = false
    loadList()
  } finally {
    assignLoading.value = false
  }
}

async function handleAssignTeamSubmit() {
  if (!assignTargetTaskId || !assignTeamForm.teamId) {
    ElMessage.warning('请选择班组')
    return
  }
  assignLoading.value = true
  try {
    const team = teamList.value.find(t => t.id === assignTeamForm.teamId)
    await dispatchTaskApi.assignTeam(assignTargetTaskId, {
      teamId: assignTeamForm.teamId,
      teamName: team?.name,
    })
    ElMessage.success('派工成功')
    assignTeamVisible.value = false
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
