import { defineStore } from 'pinia'
import { ref, shallowRef } from 'vue'
import { i18n, loadElementPlusLocale, persistLocale, resolveInitialLocale } from '@/locales'
import type { LocaleKey } from '@/locales'

/**
 * 语言 Store：
 * - current    : 当前语言
 * - elLocale   : 供 Element Plus `<el-config-provider :locale="..">` 使用的 locale 对象
 * - setLocale  : 切换语言，会同步：vue-i18n / Element Plus / localStorage / <html lang>
 */
export const useLocaleStore = defineStore('locale', () => {
  const current = ref<LocaleKey>(resolveInitialLocale())
  const elLocale = shallowRef<unknown>(null)

  async function setLocale(next: LocaleKey): Promise<void> {
    current.value = next
    i18n.global.locale.value = next
    persistLocale(next)
    try {
      document.documentElement.setAttribute('lang', next)
    } catch {
      /* ignore */
    }
    elLocale.value = await loadElementPlusLocale(next)
  }

  async function init(): Promise<void> {
    await setLocale(current.value)
  }

  return { current, elLocale, setLocale, init }
})
