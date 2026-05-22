# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:17-jre-jammy
ARG SERVICE_MODULE
ARG SERVICE_PORT=8080
ENV TZ=Asia/Shanghai
ENV SERVICE_PORT=${SERVICE_PORT}
WORKDIR /app

RUN mkdir -p /app/logs /app/data

COPY ${SERVICE_MODULE}/target/${SERVICE_MODULE}-*.jar /app/app.jar

EXPOSE ${SERVICE_PORT}

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --server.port=${SERVER_PORT:-$SERVICE_PORT}"]

