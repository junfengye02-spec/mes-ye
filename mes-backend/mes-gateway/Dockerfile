FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY mes-common/pom.xml mes-common/pom.xml
COPY mes-framework/pom.xml mes-framework/pom.xml
COPY mes-gateway/pom.xml mes-gateway/pom.xml
RUN mvn dependency:go-offline -B -pl mes-gateway -am || true

COPY . .
RUN mvn clean package -DskipTests -pl mes-gateway -am -B

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/mes-gateway/target/*.jar app.jar

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
