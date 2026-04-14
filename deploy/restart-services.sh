#!/bin/bash

set -e

APP_HOME=/opt/blog-cloud
LOG_HOME=/opt/blog-cloud/logs

mkdir -p "$LOG_HOME"

pkill -f "blog-gateway" || true
pkill -f "user-service" || true
pkill -f "article-service" || true
pkill -f "comment-service" || true
pkill -f "notify-service" || true

nohup java -jar "$APP_HOME/user-service.jar" --spring.profiles.active=prod > "$LOG_HOME/user-service.out" 2>&1 &
nohup java -jar "$APP_HOME/article-service.jar" --spring.profiles.active=prod > "$LOG_HOME/article-service.out" 2>&1 &
nohup java -jar "$APP_HOME/comment-service.jar" --spring.profiles.active=prod > "$LOG_HOME/comment-service.out" 2>&1 &
nohup java -jar "$APP_HOME/notify-service.jar" --spring.profiles.active=prod > "$LOG_HOME/notify-service.out" 2>&1 &
nohup java -jar "$APP_HOME/blog-gateway.jar" --spring.profiles.active=cloud,prod > "$LOG_HOME/blog-gateway.out" 2>&1 &
