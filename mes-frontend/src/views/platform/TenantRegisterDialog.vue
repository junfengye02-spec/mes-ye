<template>
  <el-dialog
    :model-value="modelValue"
    title="新建租户"
    width="560px"
    destroy-on-close
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="租户编码" prop="tenantCode">
        <el-input
          v-model="form.tenantCode"
          placeholder="小写字母开头，只允许字母/数字/短横线"
          maxlength="64"
        />
        <div class="field-tip">登录子域使用，注册后不可修改。</div>
      </el-form-item>
      <el-form-item label="租户名称" prop="tenantName">
        <el-input v-model="form.tenantName" placeholder="企业全称" maxlength="100" />
      </el-form-item>
      <el-form-item label="管理员用户名" prop="initialAdminUsername">
        <el-input v-model="form.initialAdminUsername" placeholder="默认 admin" maxlength="50" />
      </el-form-item>
      <el-form-item label="管理员密码" prop="initialAdminPassword">
        <el-input
          v-model="form.initialAdminPassword"
          type="password"
          show-password
          placeholder="留空则按 Change@Me-{编码} 生成，至少 8 位"
          maxlength="128"
        />
      </el-form-item>
      <el-form-item label="联系人" prop="contactName">
        <el-input v-model="form.contactName" maxlength="100" />
      </el-form-item>
      <el-form-item label="联系邮箱" prop="contactEmail">
        <el-input v-model="form.contactEmail" maxlength="128" placeholder="name@company.com" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { platformTenantApi } from '@/api/platform/tenant'
import type { TenantRegisterDTO } from '@/api/platform/tenant'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'success'): void
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const createDefault = (): TenantRegisterDTO => ({
  tenantCode: '',
  tenantName: '',
  contactName: '',
  contactEmail: '',
  initialAdminUsername: 'admin',
  initialAdminPassword: '',
})

const form = reactive<TenantRegisterDTO>(createDefault())

const rules: FormRules = {
  tenantCode: [
    { required: true, message: '请输入租户编码', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9-]{1,63}$/,
      message: '须小写字母开头，仅允许字母/数字/短横线，长度 2~64',
      trigger: 'blur',
    },
  ],
  tenantName: [
    { required: true, message: '请输入租户名称', trigger: 'blur' },
    { max: 100, message: '长度不超过 100', trigger: 'blur' },
  ],
  contactEmail: [
    { type: 'email', message: '邮箱格式不合法', trigger: 'blur' },
    { max: 128, message: '长度不超过 128', trigger: 'blur' },
  ],
  initialAdminPassword: [
    { min: 8, max: 128, message: '密码至少 8 位', trigger: 'blur' },
  ],
}

watch(
  () => props.modelValue,
  (v) => {
    if (v) resetForm()
  },
)

function resetForm() {
  Object.assign(form, createDefault())
  formRef.value?.clearValidate()
}

function handleCancel() {
  emit('update:modelValue', false)
}

function handleClose() {
  emit('update:modelValue', false)
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: TenantRegisterDTO = { ...form }
    if (!payload.initialAdminPassword) delete payload.initialAdminPassword
    if (!payload.initialAdminUsername) delete payload.initialAdminUsername
    if (!payload.contactName) delete payload.contactName
    if (!payload.contactEmail) delete payload.contactEmail
    await platformTenantApi.register(payload)
    ElMessage.success('租户注册成功，后台正在异步 provisioning')
    emit('success')
    emit('update:modelValue', false)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.field-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
  margin-top: 4px;
}
</style>
