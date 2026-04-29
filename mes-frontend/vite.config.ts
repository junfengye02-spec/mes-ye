import { defineConfig, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'

/**
 * M9-P3-08: CSP nonce 注入插件
 *
 * Vite 构建产物的 index.html 里会出现：
 *   - <script type="module" crossorigin src="/assets/index-xxx.js"></script>
 *   - <link rel="stylesheet" href="/assets/index-xxx.css">
 *   - <link rel="modulepreload" href="/assets/vue-vendor-xxx.js">
 *
 * nginx CSP 指令仅放行 'self' + 'nonce-xxx'，因此必须给每一个 <script>、
 * <style>、<link rel="modulepreload"|"preload"|"stylesheet"> 注入
 * nonce="__CSP_NONCE__" 占位，运行时由 nginx sub_filter 替换成真实 $request_id。
 *
 * 如果业务/第三方将来塞入 inline <script> 或 <style>，这里也会一并打上 nonce。
 */
function cspNoncePlugin(): Plugin {
  const NONCE_PLACEHOLDER = '__CSP_NONCE__'

  const ensureAttr = (tag: string, attrs: string, name: string, value: string): string => {
    if (new RegExp(`\\b${name}\\s*=`).test(attrs)) return `<${tag}${attrs}>`
    return `<${tag} ${name}="${value}"${attrs}>`
  }

  return {
    name: 'mes-csp-nonce',
    enforce: 'post',
    transformIndexHtml: {
      order: 'post',
      handler(html) {
        // <script ...> —— 覆盖 type=module / inline / 带 src 所有形态
        html = html.replace(/<script(\s[^>]*?)?>/g, (_m, attrs: string | undefined) => {
          return ensureAttr('script', attrs ?? '', 'nonce', NONCE_PLACEHOLDER)
        })
        // <style ...>  —— inline CSS 必须带 nonce 才能通过 style-src
        html = html.replace(/<style(\s[^>]*?)?>/g, (_m, attrs: string | undefined) => {
          return ensureAttr('style', attrs ?? '', 'nonce', NONCE_PLACEHOLDER)
        })
        // <link rel="stylesheet|modulepreload|preload">  —— Level 3 CSP 支持 link nonce
        // 即使浏览器不强制校验 link 上的 nonce，保留占位能避免 Chromium 未来收紧时踩雷
        html = html.replace(/<link(\s[^>]*?)\s*\/?>/g, (m, attrs: string) => {
          if (!/rel=["']?(stylesheet|modulepreload|preload)["']?/i.test(attrs)) return m
          if (/\bnonce\s*=/.test(attrs)) return m
          return `<link${attrs} nonce="${NONCE_PLACEHOLDER}">`
        })
        return html
      },
    },
  }
}

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
    }),
    cspNoncePlugin(),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:9091',
        changeOrigin: true,
      },
    },
  },
  build: {
    // M9-P3-08: 关闭 modulePreload polyfill，避免 Vite 在 index.html 头部
    // 塞入一段无法控制 nonce 的内联 <script>。Chrome 66+/Firefox 115+/Safari 17+
    // 已原生支持 <link rel="modulepreload">，企业内网现代浏览器无需 polyfill。
    modulePreload: {
      polyfill: false,
    },
    // 当单个 chunk 超过阈值时给出 warning；结合 manualChunks 后主包应远低于此
    chunkSizeWarningLimit: 800,
    // cssCodeSplit = true（Vite 默认）保证每个异步 chunk 的 CSS 单独出成
    // 外链 <link>，不会被内联到 JS 里；进一步降低 inline style 的可能性
    cssCodeSplit: true,
    rollupOptions: {
      output: {
        /**
         * 按 node_modules 来源拆分 vendor chunk，缓解单一 index.js 超过 1MB 的问题：
         *   - vue-vendor     : vue / vue-router / pinia / @vue/*
         *   - element-vendor : element-plus 及其图标包
         *   - lib-vendor     : 通用运行时库（axios / dayjs / nprogress / lodash-es 等）
         *   - vendor         : 其余第三方依赖
         * 路由懒加载不受影响，各视图仍然按需生成独立 chunk。
         */
        manualChunks(id: string) {
          if (!id.includes('node_modules')) {
            return undefined
          }
          if (
            id.includes('/node_modules/vue/') ||
            id.includes('/node_modules/@vue/') ||
            id.includes('/node_modules/vue-router/') ||
            id.includes('/node_modules/pinia/')
          ) {
            return 'vue-vendor'
          }
          if (
            id.includes('/node_modules/element-plus/') ||
            id.includes('/node_modules/@element-plus/')
          ) {
            return 'element-vendor'
          }
          if (
            id.includes('/node_modules/axios/') ||
            id.includes('/node_modules/dayjs/') ||
            id.includes('/node_modules/nprogress/') ||
            id.includes('/node_modules/lodash') ||
            id.includes('/node_modules/qs/') ||
            id.includes('/node_modules/js-cookie/')
          ) {
            return 'lib-vendor'
          }
          return 'vendor'
        },
      },
    },
  },
})
