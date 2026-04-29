import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'

/**
 * 主题模式类型
 * - light: 浅色模式
 * - dark: 深色模式
 * - auto: 跟随系统
 */
export type ThemeMode = 'light' | 'dark' | 'auto'

/** localStorage 持久化 key */
const STORAGE_KEY = 'mes-theme-mode'

/** HTML 根节点上用于标记深色的 class 名 */
const DARK_CLASS = 'dark'

/** 系统深色媒体查询 */
const PREFERS_DARK_QUERY = '(prefers-color-scheme: dark)'

/**
 * 从 localStorage 读取持久化的主题模式，非法值兜底为 auto
 */
function readStoredMode(): ThemeMode {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw === 'light' || raw === 'dark' || raw === 'auto') {
      return raw
    }
  } catch {
    // localStorage 不可用时（SSR 或受限环境）回退到 auto
  }
  return 'auto'
}

/**
 * 尝试写入 localStorage，失败时忽略（例如隐私模式）
 */
function writeStoredMode(mode: ThemeMode): void {
  try {
    localStorage.setItem(STORAGE_KEY, mode)
  } catch {
    // 忽略写入失败
  }
}

/**
 * 读取系统当前是否偏好深色
 */
function getSystemPrefersDark(): boolean {
  try {
    return window.matchMedia(PREFERS_DARK_QUERY).matches
  } catch {
    return false
  }
}

/**
 * 主题 Store：管理 Element Plus 深色模式 + 业务自定义主题变量
 *
 * - mode        ：用户选择的模式（light / dark / auto）
 * - isDark      ：当前实际生效的是否深色（auto 模式下根据系统计算）
 * - setMode     ：显式切换到指定模式
 * - toggleMode  ：在 light -> dark -> auto -> light 之间循环
 * - initTheme   ：应用启动时调用，应用持久化的模式并监听系统主题变化
 */
export const useThemeStore = defineStore('theme', () => {
  // 初始化时直接从 localStorage 读取，避免首屏白底闪烁
  const mode = ref<ThemeMode>(readStoredMode())
  // 系统当前偏好（仅在 auto 模式下影响 isDark）
  const systemDark = ref<boolean>(getSystemPrefersDark())
  // 系统主题监听句柄（MediaQueryList），仅在 initTheme 里创建一次
  let mediaQueryList: MediaQueryList | null = null

  /**
   * 当前是否实际处于深色模式：
   * - mode === 'dark' 时必为 true
   * - mode === 'light' 时必为 false
   * - mode === 'auto' 时跟随系统
   */
  const isDark = computed<boolean>(() => {
    if (mode.value === 'dark') return true
    if (mode.value === 'light') return false
    return systemDark.value
  })

  /**
   * 把当前 isDark 状态同步到 <html> 的 class 上
   * Element Plus 深色 CSS（css-vars.css）依赖 `html.dark` 选择器
   */
  function applyDomClass(dark: boolean): void {
    try {
      const root = document.documentElement
      if (dark) {
        root.classList.add(DARK_CLASS)
      } else {
        root.classList.remove(DARK_CLASS)
      }
      // 同时设置 color-scheme 让原生控件（滚动条、表单）也跟随
      root.style.colorScheme = dark ? 'dark' : 'light'
    } catch {
      // SSR 或没有 document 时忽略
    }
  }

  /**
   * 切换到指定模式
   *
   * @param next 目标模式
   */
  function setMode(next: ThemeMode): void {
    mode.value = next
    writeStoredMode(next)
  }

  /**
   * 三态循环：light -> dark -> auto -> light
   */
  function toggleMode(): void {
    if (mode.value === 'light') {
      setMode('dark')
    } else if (mode.value === 'dark') {
      setMode('auto')
    } else {
      setMode('light')
    }
  }

  /**
   * 系统主题变化时的回调（仅在 auto 模式下生效）
   */
  function handleSystemChange(e: MediaQueryListEvent): void {
    systemDark.value = e.matches
  }

  /**
   * 应用启动时调用：
   * 1) 注册系统主题监听（若浏览器支持）
   * 2) 立即应用当前 isDark 到 <html>
   */
  function initTheme(): void {
    // 注册媒体查询监听，保证用户在 auto 模式下能实时跟随系统切换
    try {
      if (!mediaQueryList) {
        mediaQueryList = window.matchMedia(PREFERS_DARK_QUERY)
        // addEventListener 在较老 Safari 上可能不存在，这里做兜底
        if (typeof mediaQueryList.addEventListener === 'function') {
          mediaQueryList.addEventListener('change', handleSystemChange)
        } else if (typeof (mediaQueryList as unknown as {
          addListener?: (cb: (e: MediaQueryListEvent) => void) => void
        }).addListener === 'function') {
          (mediaQueryList as unknown as {
            addListener: (cb: (e: MediaQueryListEvent) => void) => void
          }).addListener(handleSystemChange)
        }
      }
    } catch {
      // 浏览器不支持 matchMedia 时不做处理
    }
    // 立即应用一次
    applyDomClass(isDark.value)
  }

  // isDark 变化时自动把 class 写到 <html>
  watch(isDark, dark => {
    applyDomClass(dark)
  })

  return {
    mode,
    isDark,
    setMode,
    toggleMode,
    initTheme,
  }
})
