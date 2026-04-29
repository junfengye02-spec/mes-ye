import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'

export type LocaleKey = 'zh-CN' | 'en-US'

export const SUPPORTED_LOCALES: { value: LocaleKey; label: string; short: string }[] = [
  { value: 'zh-CN', label: '简体中文', short: '中' },
  { value: 'en-US', label: 'English', short: 'EN' },
]

const LOCALE_STORAGE_KEY = 'mes.locale'

export function resolveInitialLocale(): LocaleKey {
  // 优先 localStorage，其次浏览器语言，最后回退到 zh-CN
  try {
    const saved = localStorage.getItem(LOCALE_STORAGE_KEY) as LocaleKey | null
    if (saved === 'zh-CN' || saved === 'en-US') return saved
  } catch {
    // SSR / 私有模式 localStorage 不可用
  }
  const lang = (typeof navigator !== 'undefined' && navigator.language) || 'zh-CN'
  return lang.toLowerCase().startsWith('en') ? 'en-US' : 'zh-CN'
}

/**
 * 把当前语言持久化到 localStorage，方便刷新/重启恢复。
 */
export function persistLocale(locale: LocaleKey): void {
  try {
    localStorage.setItem(LOCALE_STORAGE_KEY, locale)
  } catch {
    /* ignore */
  }
}

export const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: resolveInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
  missingWarn: false,
  fallbackWarn: false,
})

/**
 * 把应用的 i18n locale 与 Element Plus 自身的 locale 联动。
 * 由 `useLocaleStore` 在 setLocale 时调用。
 */
export async function loadElementPlusLocale(locale: LocaleKey): Promise<unknown> {
  if (locale === 'en-US') {
    return (await import('element-plus/es/locale/lang/en')).default
  }
  return (await import('element-plus/es/locale/lang/zh-cn')).default
}
