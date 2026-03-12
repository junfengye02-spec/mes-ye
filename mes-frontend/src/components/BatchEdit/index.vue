<template>
  <el-dialog v-model="visible" title="批量编辑" width="80%" @close="handleClose">
    <el-table :data="editRows" border>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <slot />
    </el-table>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">
        保存 ({{ editRows.length }} 条)
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  rows: any[]
}>()

const visible = defineModel<boolean>('visible', { default: false })
const emit = defineEmits<{
  save: [rows: any[]]
}>()

const editRows = ref<any[]>([])
const saving = ref(false)

watch(() => props.rows, val => {
  editRows.value = val.map(r => ({ ...r }))
}, { immediate: true })

function handleSave() {
  saving.value = true
  emit('save', editRows.value)
}

function handleClose() {
  saving.value = false
}

defineExpose({ done: () => { saving.value = false; visible.value = false } })
</script>
