FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY mes-common/pom.xml mes-common/pom.xml
COPY mes-framework/pom.xml mes-framework/pom.xml
COPY mes-system/pom.xml mes-system/pom.xml
COPY mes-basic/pom.xml mes-basic/pom.xml
COPY mes-team/pom.xml mes-team/pom.xml
COPY mes-process/pom.xml mes-process/pom.xml
COPY mes-plan/pom.xml mes-plan/pom.xml
COPY mes-workorder/pom.xml mes-workorder/pom.xml
COPY mes-dispatch/pom.xml mes-dispatch/pom.xml
COPY mes-abnormal/pom.xml mes-abnormal/pom.xml
COPY mes-quality/pom.xml mes-quality/pom.xml
COPY mes-query/pom.xml mes-query/pom.xml
COPY mes-material/pom.xml mes-material/pom.xml
COPY mes-aps/pom.xml mes-aps/pom.xml
COPY mes-admin/pom.xml mes-admin/pom.xml
COPY mes-gateway/pom.xml mes-gateway/pom.xml
COPY mes-system-service/pom.xml mes-system-service/pom.xml
COPY mes-master-data-service/pom.xml mes-master-data-service/pom.xml
COPY mes-production-service/pom.xml mes-production-service/pom.xml
COPY mes-quality-service/pom.xml mes-quality-service/pom.xml
COPY mes-material-service/pom.xml mes-material-service/pom.xml
COPY mes-integration-service/pom.xml mes-integration-service/pom.xml
COPY mes-query-service/pom.xml mes-query-service/pom.xml
RUN mvn dependency:go-offline -B || true

COPY . .
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-jammy AS base
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms256m -Xmx512m"
# 非 root 运行容器；curl 供 healthcheck 使用
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r mes && useradd -r -g mes -u 10001 -m -d /home/mes -s /sbin/nologin mes

FROM base AS mes-system-service
WORKDIR /app
COPY --from=builder /build/mes-system-service/target/*.jar app.jar
RUN chown -R mes:mes /app
USER mes
EXPOSE 9001
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=6 \
  CMD curl -fsS http://localhost:9001/api/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]

FROM base AS mes-master-data-service
WORKDIR /app
COPY --from=builder /build/mes-master-data-service/target/*.jar app.jar
RUN chown -R mes:mes /app
USER mes
EXPOSE 9002
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=6 \
  CMD curl -fsS http://localhost:9002/api/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]

FROM base AS mes-production-service
WORKDIR /app
COPY --from=builder /build/mes-production-service/target/*.jar app.jar
RUN chown -R mes:mes /app
USER mes
EXPOSE 9003
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=6 \
  CMD curl -fsS http://localhost:9003/api/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]

FROM base AS mes-quality-service
WORKDIR /app
COPY --from=builder /build/mes-quality-service/target/*.jar app.jar
RUN chown -R mes:mes /app
USER mes
EXPOSE 9004
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=6 \
  CMD curl -fsS http://localhost:9004/api/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]

FROM base AS mes-material-service
WORKDIR /app
COPY --from=builder /build/mes-material-service/target/*.jar app.jar
RUN chown -R mes:mes /app
USER mes
EXPOSE 9005
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=6 \
  CMD curl -fsS http://localhost:9005/api/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]

FROM base AS mes-integration-service
WORKDIR /app
COPY --from=builder /build/mes-integration-service/target/*.jar app.jar
RUN chown -R mes:mes /app
USER mes
EXPOSE 9006
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=6 \
  CMD curl -fsS http://localhost:9006/api/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]

FROM base AS mes-query-service
WORKDIR /app
COPY --from=builder /build/mes-query-service/target/*.jar app.jar
RUN chown -R mes:mes /app
USER mes
EXPOSE 9007
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=6 \
  CMD curl -fsS http://localhost:9007/api/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]

FROM base AS mes-gateway
WORKDIR /app
COPY --from=builder /build/mes-gateway/target/*.jar app.jar
RUN chown -R mes:mes /app
USER mes
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=6 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]

FROM base AS mes-admin
WORKDIR /app
COPY --from=builder /build/mes-admin/target/*.jar app.jar
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
RUN chown -R mes:mes /app /home/mes \
    && mkdir -p /app/uploads && chown -R mes:mes /app/uploads
USER mes
EXPOSE 9090
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=10 \
  CMD curl -fsS http://localhost:9090/api/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
