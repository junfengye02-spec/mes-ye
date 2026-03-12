<template>
  <el-dialog v-model="visible" title="数据导入" width="520px" @close="handleClose">
    <el-upload
      ref="uploadRef"
      drag
      :auto-upload="false"
      :limit="1"
      accept=".xlsx,.xls,.csv"
      :on-change="handleFileChange"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">将文件拖到此处，或 <em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip">
          支持 xlsx / xls / csv 格式文件
          <el-button v-if="templateUrl" type="primary" link @click="downloadTemplate">
            下载导入模板
          </el-button>
        </div>
      </template>
    </el-upload>

    <div v-if="errors.length" class="import-errors">
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
