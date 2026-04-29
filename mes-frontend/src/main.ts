import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
// 引入 Element Plus 官方深色 CSS 变量，配合 <html class="dark"> 生效
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { i18n } from './locales'
import { useLocaleStore } from './stores/locale'
import { useThemeStore } from './stores/theme'
import { setupAuthDirective } from './directives/auth'
import { installCSPNonce } from './utils/csp-nonce'
// 业务主题变量 + html.dark 覆盖层，必须在 element-plus/dist/index.css
// 和 element-plus/theme-chalk/dark/css-vars.css 之后引入，才能覆盖默认值
import './styles/theme.css'
// 无障碍（a11y）全局样式：focus-visible / sr-only / skip-link
// 放在 theme.css 之后以保证覆盖优先级（例如 --el-color-primary 对聚焦描边生效）
import './styles/a11y.css'

// M9-P3-08: 必须在任何可能触发 CSS-in-JS / 动态脚本加载之前完成 nonce 注入，
// 这样 Vite 运行时懒加载 chunk（vue-vendor / views/**）读 window.__webpack_nonce__
// 才能拿到正确的 nonce，避免 CSP violation。
installCSPNonce()

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(i18n)

// 注册全局 v-auth 按钮级权限指令（依赖 Pinia 的 useAuthStore，需在 app.use(pinia) 之后）
setupAuthDirective(app)

// ElementPlus 默认用 zh-cn 先初始化，真正的动态 locale 由 App.vue 中的
// <el-config-provider :locale="localeStore.elLocale" /> 接管
app.use(ElementPlus, { locale: zhCn })

// 在挂载前先初始化主题，避免首屏先出现白底再切暗（FOUC）
const themeStore = useThemeStore(pinia)
themeStore.initTheme()

// 初始化 locale（异步加载 Element Plus 语言包），不阻塞挂载
const localeStore = useLocaleStore(pinia)
localeStore.init()

app.mount('#app')
