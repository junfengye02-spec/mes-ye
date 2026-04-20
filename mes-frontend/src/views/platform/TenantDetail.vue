<template>
  <el-drawer
    :model-value="modelValue"
    title="租户详情"
    direction="rtl"
    size="560px"
    destroy-on-close
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <div v-loading="loading" class="tenant-detail">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="租户编码">
          <span class="code-text">{{ detail.tenantCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="租户名称">{{ detail.tenantName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType">{{ statusLabel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Schema 模式">{{ detail.schemaMode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数据区域">{{ detail.dataRegion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户配额">{{ detail.quotaUsers ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="存储配额（MB）">{{ detail.quotaStorageMb ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="QPS 配额">{{ detail.quotaQps ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="过期时间">{{ detail.expireAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系邮箱">{{ detail.contactEmail || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createdTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updatedTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else-if="!loading" description="暂无数据" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  platformTenantApi,
  TENANT_STATUS_LABEL,
  TENANT_STATUS_TAG_TYPE,
} from '@/api/platform/tenant'
import type { TenantVO } from '@/api/platform/tenant'

const props = defineProps<{
  modelValue: boolean
  tenantId: number | null
}>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
}>()

const loading = ref(false)
const detail = ref<TenantVO | null>(null)

const statusLabel = computed(() =>
  detail.value ? TENANT_STATUS_LABEL[detail.value.status] || String(detail.value.status) : '-',
)
const statusTagType = computed(() =>
  detail.value ? TENANT_STATUS_TAG_TYPE[detail.value.status] : undefined,
)

watch(
  () => [props.modelValue, props.tenantId] as const,
  async ([visible, id]) => {
    if (!visible || !id) {
      detail.value = null
      return
    }
    loading.value = true
    try {
      detail.value = await platformTenantApi.getDetail(id)
    } finally {
      loading.value = false
    }
  },
)
</script>

<style scoped>
.tenant-detail {
  padding: 8px 16px 16px;
  min-height: 200px;
}
.code-text {
  font-family: Consolas, 'Courier New', monospace;
  font-weight: 600;
}
</style>
