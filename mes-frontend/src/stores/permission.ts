import { defineStore } from 'pinia'
import { ref } from 'vue'
import { sysMenuApi } from '@/api/system/menu'
import type { SysMenuVO } from '@/api/system/menu'
import type { MenuItem } from '@/layout/menuConfig'

export const usePermissionStore = defineStore('permission', () => {
  const dynamicMenus = ref<MenuItem[]>([])
  const loaded = ref(false)

  function mapToMenuItem(menu: SysMenuVO): MenuItem {
    return {
      path: menu.path || '',
      title: menu.menuName,
      icon: menu.icon || undefined,
      children: menu.children?.filter(c => c.menuType !== 'B').map(mapToMenuItem),
    }
  }

  async function loadUserMenus() {
    try {
      const menus = await sysMenuApi.getUserTree()
      dynamicMenus.value = (menus || [])
        .filter(m => m.menuType !== 'B')
        .map(mapToMenuItem)
      loaded.value = true
    } catch {
      loaded.value = false
    }
  }

  function reset() {
    dynamicMenus.value = []
    loaded.value = false
  }

  return { dynamicMenus, loaded, loadUserMenus, reset }
})
