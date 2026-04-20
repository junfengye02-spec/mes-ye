<template>
  <div class="file-upload">
    <el-upload
      :action="uploadUrl"
      :data="{ directory: directory }"
      :headers="uploadHeaders"
      :before-upload="beforeUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      :file-list="fileList"
      :drag="drag"
      :multiple="multiple"
      :accept="accept"
      :show-file-list="showFileList"
    >
      <slot>
        <el-button type="primary">
          <el-icon><Upload /></el-icon> 上传文件
        </el-button>
      </slot>
      <template v-if="tip" #tip>
        <div class="el-upload__tip">{{ tip }}</div>
      </template>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { UploadFile, UploadRawFile } from 'element-plus'
import { ElMessage } from 'element-plus'

const props = withDefaults(defineProps<{
  directory?: string
  maxSizeMb?: number
  accept?: string
  drag?: boolean
  multiple?: boolean
  tip?: string
  showFileList?: boolean
}>(), {
  directory: 'common',
  maxSizeMb: 10,
  accept: '',
  drag: false,
  multiple: false,
  showFileList: true,
})

const emit = defineEmits<{
  success: [data: { fileUrl: string; fileName: string; fileSize: string }]
  error: [error: Error]
}>()

const fileList = ref<UploadFile[]>([])
const uploadUrl = computed(() => `${import.meta.env.VITE_API_BASE_URL}/file/upload`)
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

function beforeUpload(file: UploadRawFile) {
  if (props.maxSizeMb && file.size / 1024 / 1024 > props.maxSizeMb) {
    ElMessage.error(`文件大小不能超过 ${props.maxSizeMb}MB`)
    return false
  }
  return true
}

function handleSuccess(response: any) {
  if (response && response.code === 200) {
    emit('success', response.data)
  } else {
    const msg = response?.message || '上传失败'
    ElMessage.error(msg)
    emit('error', new Error(msg))
  }
}

function handleError(error: Error) {
  ElMessage.error('文件上传失败')
  emit('error', error)
}
</script>
