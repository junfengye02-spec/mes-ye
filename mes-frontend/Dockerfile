FROM node:20-alpine AS builder
WORKDIR /build
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
ARG NGINX_CONF=nginx.conf
COPY --from=builder /build/dist /usr/share/nginx/html
COPY ${NGINX_CONF} /etc/nginx/conf.d/default.conf

ENV TZ=Asia/Shanghai

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
