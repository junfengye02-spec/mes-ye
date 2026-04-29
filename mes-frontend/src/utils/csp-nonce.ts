/**
 * CSP nonce 工具（M9-P3-08）
 *
 * 读取 <meta name="csp-nonce" content="..."> 里的 nonce 并暴露给运行时。
 * 生产环境的 nonce 来自 nginx sub_filter 替换 __CSP_NONCE__；dev 环境
 * 保持占位字符串原样（dev 不启用 CSP 严格策略，不会报错）。
 *
 * 使用场景：
 *   1. 第三方库（如 webpack 动态 import polyfill）读 window.__webpack_nonce__
 *   2. 业务代码动态 new Style / Script / link 元素时显式 setAttribute('nonce', ...)
 *   3. Element Plus / Vue 运行时动态注入的 <style>（若未来出现）
 *
 * 注意：
 *   - 浏览器会在 nonce 属性写入 DOM 后将其从 attribute 读取中遮蔽（getAttribute
 *     返回空字符串，Chrome 95+），只能通过 element.nonce 属性读取；
 *   - 因此本工具只提供"拿 nonce 字符串"一个能力，不 monkey-patch createElement，
 *     避免踩到属性遮蔽的坑。
 */

const META_NAME = 'csp-nonce'

/**
 * 读取当前页面的 CSP nonce；若未配置或占位未被替换则返回 undefined。
 */
export function getCSPNonce(): string | undefined {
  if (typeof document === 'undefined') return undefined
  const meta = document.querySelector(`meta[name="${META_NAME}"]`)
  const value = meta?.getAttribute('content')?.trim()
  if (!value) return undefined
  // dev 或 nginx 未替换时占位还是 __CSP_NONCE__ 字面量，视为未启用
  if (value === '__CSP_NONCE__') return undefined
  return value
}

/**
 * 在 App 启动前调用：
 *   1. 把 nonce 挂到 window.__CSP_NONCE__（业务代码可读）
 *   2. 同时挂到 window.__webpack_nonce__（Vite / webpack 运行时动态 chunk 会读）
 * 若未读到 nonce（dev / 未启用 CSP），不做任何事。
 */
export function installCSPNonce(): void {
  const nonce = getCSPNonce()
  if (!nonce) return
  const w = window as unknown as {
    __CSP_NONCE__?: string
    __webpack_nonce__?: string
  }
  w.__CSP_NONCE__ = nonce
  // __webpack_nonce__ 是 webpack 和 Vite 通用的运行时 nonce 读取约定
  w.__webpack_nonce__ = nonce
}
