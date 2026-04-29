# ============================================================
#  csp-headers.inc  ——  M9-P3-08 nonce-based CSP
#  ------------------------------------------------------------
#  为什么单独一个 include 文件：
#    nginx add_header 有「子 location 出现任何 add_header 即
#    丢弃父级全部 add_header」的继承规则，很容易踩坑。
#    把全部安全头打包成 include，在每个需要的 location 里一行
#    `include /etc/nginx/conf.d/csp-headers.inc;` 搞定。
#
#  使用方式（各 nginx.conf 统一）：
#    1. server 块内顶部先 `set $csp_nonce $request_id;`
#    2. server 级 include 一次（确保默认响应带安全头）
#    3. 任何需要额外 add_header（如 Cache-Control）的 location
#       必须重新 include 一次，避免头被丢
#
#  三份 nginx 配置（nginx.conf / nginx.ha.conf /
#  nginx.microservice.conf / nginx.maintenance.conf）通过 include
#  同一份保持 CSP 指令完全一致，CI 可 diff 这个文件即可。
# ============================================================

# 基础安全头
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;

# HSTS：启用 TLS 后生效
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

# ---- nonce-based CSP（P3-08 核心）----
# script-src/style-src  仅允许同源 + 本次请求的 nonce，彻底关掉 unsafe-inline/eval
# style-src-attr        为 HTML style="" 属性放行（Element Plus / Vue 大量使用；
#                       Google CSP Evaluator 认为可接受，不影响 A+ 评分）
# connect-src           允许同源 + wss/https（WebSocket / 第三方 API 走 HTTPS）
# frame-ancestors 'none' 禁止被任何 iframe 嵌入，防点击劫持
# object-src 'none'      禁用 <object> / <embed>，阻断 Flash/PDF 注入
# upgrade-insecure-requests  自动把 http:// 子资源升 https://
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'nonce-$csp_nonce'; script-src-attr 'none'; style-src 'self' 'nonce-$csp_nonce'; style-src-attr 'unsafe-inline'; img-src 'self' data: blob:; connect-src 'self' wss: https:; font-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'; object-src 'none'; upgrade-insecure-requests" always;
