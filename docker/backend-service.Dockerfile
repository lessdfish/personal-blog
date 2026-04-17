# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-17 AS build
ARG SERVICE_MODULE
WORKDIR /workspace

COPY pom.xml ./
COPY blog-common/pom.xml blog-common/pom.xml
COPY blog-gateway/pom.xml blog-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY article-service/pom.xml article-service/pom.xml
COPY comment-service/pom.xml comment-service/pom.xml
COPY notify-service/pom.xml notify-service/pom.xml

RUN mvn -pl ${SERVICE_MODULE} -am dependency:go-offline -DskipTests

COPY blog-common blog-common
COPY blog-gateway blog-gateway
COPY user-service user-service
COPY article-service article-service
COPY comment-service comment-service
COPY notify-service notify-service

RUN mvn -pl ${SERVICE_MODULE} -am clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
ARG SERVICE_MODULE
ARG SERVICE_PORT=8080
ENV TZ=Asia/Shanghai
ENV SERVICE_PORT=${SERVICE_PORT}
WORKDIR /app

RUN mkdir -p /app/logs /app/data

COPY --from=build /workspace/${SERVICE_MODULE}/target/${SERVICE_MODULE}-*.jar /app/app.jar

EXPOSE ${SERVICE_PORT}

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --server.port=${SERVER_PORT:-$SERVICE_PORT}"]

