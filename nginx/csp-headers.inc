# ============================================================
#  nginx/csp-headers.inc  ——  canonical reference copy
#  ------------------------------------------------------------
#  本目录是 mes 项目级的 nginx 配置参考位（对齐 nginx/gateway-lb.conf）。
#  真正被 mes-frontend 镜像使用的同名文件在
#  `mes-frontend/csp-headers.inc`（Dockerfile build context = mes-frontend/）
#  两份内容必须保持一致。
#
#  同步规则：
#    如需修改 CSP 指令，务必 mes-frontend/csp-headers.inc 和本文件**同时改**，
#    CI 会在 PR 检测阶段 diff 这两份文件，不一致会失败。
#    （M9-P3-08 nonce 迁移引入）
# ============================================================

add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;

add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'nonce-$csp_nonce'; script-src-attr 'none'; style-src 'self' 'nonce-$csp_nonce'; style-src-attr 'unsafe-inline'; img-src 'self' data: blob:; connect-src 'self' wss: https:; font-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'; object-src 'none'; upgrade-insecure-requests" always;
