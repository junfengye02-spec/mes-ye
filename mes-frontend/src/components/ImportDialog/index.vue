<template>
  <!-- Element Plus el-dialog 已内置 role="dialog" 及 aria-labelledby 关联 title，这里不再手工加 -->
  <el-dialog v-model="visible" title="数据导入" width="520px" @close="handleClose">
    <el-upload
      ref="uploadRef"
      drag
      :auto-upload="false"
      :limit="1"
      accept=".xlsx,.xls,.csv"
      :on-change="handleFileChange"
    >
      <el-icon class="el-icon--upload" aria-hidden="true"><UploadFilled /></el-icon>
      <div class="el-upload__text">将文件拖到此处，或 <em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip">
          支持 xlsx / xls / csv 格式文件
          <el-button
            v-if="templateUrl"
            type="primary"
            link
            aria-label="下载导入模板"
            @click="downloadTemplate"
          >
            下载导入模板
          </el-button>
        </div>
      </template>
    </el-upload>

    <!-- 导入错误区域用 role="alert" + aria-live，让错误立即被屏幕阅读器朗读 -->
    <div
      v-if="errors.length"
      class="import-errors"
      role="alert"
      aria-live="assertive"
    >
      <el-alert title="导入错误" type="error" :closable="false" show-icon>
        <ul>
          <li v-for="(err, i) in errors" :key="i">{{ err }}</li>
        </ul>
      </el-alert>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="importing" :disabled="!file" @click="handleImport">
        确认导入
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { UploadFile, UploadInstance } from 'element-plus'

const props = defineProps<{
  templateUrl?: string
}>()

const visible = defineModel<boolean>('visible', { default: false })
const emit = defineEmits<{
  import: [file: File]
}>()

const uploadRef = ref<UploadInstance>()
const file = ref<File | null>(null)
const importing = ref(false)
const errors = ref<string[]>([])

function handleFileChange(uploadFile: UploadFile) {
  file.value = uploadFile.raw || null
  errors.value = []
}

function handleImport() {
  if (!file.value) return
  importing.value = true
  emit('import', file.value)
}

function downloadTemplate() {
  if (props.templateUrl) {
    window.open(props.templateUrl, '_blank')
  }
}

function handleClose() {
  file.value = null
  errors.value = []
  importing.value = false
  uploadRef.value?.clearFiles()
}

defineExpose({ setErrors: (errs: string[]) => { errors.value = errs; importing.value = false } })
</script>

<style scoped>
.import-errors {
  margin-top: 16px;
}
.import-errors ul {
  margin: 8px 0 0;
  padding-left: 20px;
}
</style>
