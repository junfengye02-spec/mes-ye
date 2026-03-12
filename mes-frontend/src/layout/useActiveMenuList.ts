import { computed } from 'vue'
import { menuList as staticMenuList } from './menuConfig'
import type { MenuItem } from './menuConfig'
import { usePermissionStore } from '@/stores/permission'

export function useActiveMenuList() {
  const permissionStore = usePermissionStore()

  return computed<MenuItem[]>(() => {
    if (permissionStore.loaded && permissionStore.dynamicMenus.length > 0) {
      return permissionStore.dynamicMenus
    }
    return staticMenuList
  })
}
