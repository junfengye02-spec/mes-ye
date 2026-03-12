FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY mes-common/pom.xml mes-common/pom.xml
COPY mes-framework/pom.xml mes-framework/pom.xml
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
RUN mvn dependency:go-offline -B

COPY . .
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/mes-admin/target/*.jar app.jar

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

EXPOSE 9090

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
