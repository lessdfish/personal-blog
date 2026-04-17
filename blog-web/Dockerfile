# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS build
WORKDIR /app

COPY blog-web/package.json blog-web/package-lock.json ./
RUN npm ci

COPY blog-web ./
RUN npm run build

FROM nginx:1.27-alpine
COPY blog-web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

