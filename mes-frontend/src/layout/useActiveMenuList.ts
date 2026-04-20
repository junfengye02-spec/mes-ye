import { computed } from 'vue'
import { menuList as staticMenuList } from './menuConfig'
import type { MenuItem } from './menuConfig'
import { usePermissionStore } from '@/stores/permission'
import { useAuthStore } from '@/stores/auth'

function filterPlatformOnly(items: MenuItem[], isPlatform: boolean): MenuItem[] {
  return items
    .filter((m) => (m.platformOnly ? isPlatform : true))
    .map((m) => ({
      ...m,
      children: m.children ? filterPlatformOnly(m.children, isPlatform) : undefined,
    }))
}

export function useActiveMenuList() {
  const permissionStore = usePermissionStore()
  const authStore = useAuthStore()

  return computed<MenuItem[]>(() => {
    const isPlatform = authStore.userInfo?.tenantId === 0
    const source =
      permissionStore.loaded && permissionStore.dynamicMenus.length > 0
        ? permissionStore.dynamicMenus
        : staticMenuList
    return filterPlatformOnly(source, isPlatform)
  })
}
