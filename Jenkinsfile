pipeline {
  agent any

  environment {
    GHCR_OWNER = 'lessdfish'
    REGISTRY = "ghcr.io/${GHCR_OWNER}"
    VM_HOST = '192.168.147.129'
    VM_USER = 'deploy'
    DEPLOY_DIR = '/opt/blog-cloud'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          def shortCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
          env.IMAGE_TAG = "${env.BUILD_NUMBER}-${shortCommit}"
        }
      }
    }

    stage('Test') {
      steps {
        sh '''
          docker run --rm \
            -v "$WORKSPACE:/workspace" \
            -v "$HOME/.m2:/root/.m2" \
            -w /workspace \
            maven:3.9.9-eclipse-temurin-17 \
            mvn -ntp -B verify

          docker run --rm \
            -v "$WORKSPACE/blog-web:/workspace" \
            -w /workspace \
            node:22-alpine \
            sh -c "npm ci && npm run build"
        '''
      }
    }

    stage('Build Images') {
      steps {
        sh '''
          docker build -f docker/backend-service.Dockerfile --build-arg SERVICE_MODULE=user-service --build-arg SERVICE_PORT=8081 -t $REGISTRY/blog-cloud-user-service:$IMAGE_TAG .
          docker build -f docker/backend-service.Dockerfile --build-arg SERVICE_MODULE=article-service --build-arg SERVICE_PORT=8082 -t $REGISTRY/blog-cloud-article-service:$IMAGE_TAG .
          docker build -f docker/backend-service.Dockerfile --build-arg SERVICE_MODULE=comment-service --build-arg SERVICE_PORT=8083 -t $REGISTRY/blog-cloud-comment-service:$IMAGE_TAG .
          docker build -f docker/backend-service.Dockerfile --build-arg SERVICE_MODULE=notify-service --build-arg SERVICE_PORT=8084 -t $REGISTRY/blog-cloud-notify-service:$IMAGE_TAG .
          docker build -f docker/backend-service.Dockerfile --build-arg SERVICE_MODULE=blog-gateway --build-arg SERVICE_PORT=18080 -t $REGISTRY/blog-cloud-gateway:$IMAGE_TAG .
          docker build -f blog-web/Dockerfile -t $REGISTRY/blog-cloud-web:$IMAGE_TAG .
        '''
      }
    }

    stage('Push Images') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'github-ghcr-token', usernameVariable: 'GHCR_USER', passwordVariable: 'GHCR_TOKEN')]) {
          sh '''
            set +x
            echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
            docker push $REGISTRY/blog-cloud-user-service:$IMAGE_TAG
            docker push $REGISTRY/blog-cloud-article-service:$IMAGE_TAG
            docker push $REGISTRY/blog-cloud-comment-service:$IMAGE_TAG
            docker push $REGISTRY/blog-cloud-notify-service:$IMAGE_TAG
            docker push $REGISTRY/blog-cloud-gateway:$IMAGE_TAG
            docker push $REGISTRY/blog-cloud-web:$IMAGE_TAG
          '''
        }
      }
    }

    stage('Deploy VM') {
      steps {
        sshagent(credentials: ['vm-deploy-key']) {
          withCredentials([
            file(credentialsId: 'blog-cloud-env-file', variable: 'BLOG_CLOUD_ENV'),
            usernamePassword(credentialsId: 'github-ghcr-token', usernameVariable: 'GHCR_USER', passwordVariable: 'GHCR_TOKEN')
          ]) {
            sh '''
              set +x

              ssh ${VM_USER}@${VM_HOST} "mkdir -p ${DEPLOY_DIR} && rm -f ${DEPLOY_DIR}/.env.production.tmp"

              scp docker-compose.yml docker-compose.images.yml ${VM_USER}@${VM_HOST}:${DEPLOY_DIR}/
              rsync -az --delete deploy/ ${VM_USER}@${VM_HOST}:${DEPLOY_DIR}/deploy/
              scp "$BLOG_CLOUD_ENV" ${VM_USER}@${VM_HOST}:${DEPLOY_DIR}/.env.production.tmp

              echo "$GHCR_TOKEN" | ssh ${VM_USER}@${VM_HOST} "docker login ghcr.io -u '$GHCR_USER' --password-stdin"

              ssh ${VM_USER}@${VM_HOST} "
                set -e
                unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY all_proxy
                export NO_PROXY=127.0.0.1,localhost,::1
                cd ${DEPLOY_DIR} &&
                mv .env.production.tmp .env.production &&
                chmod 600 .env.production &&
                find deploy -name '*.sh' -exec sed -i 's/\\r$//' {} \\; &&
                chmod +x deploy/scripts/apply-mysql-migrations.sh deploy/post-deploy-check.sh deploy/import-nacos-configs.sh &&
                export GHCR_OWNER='${GHCR_OWNER}' &&
                export IMAGE_TAG='${IMAGE_TAG}' &&
                docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.images.yml pull &&
                docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.images.yml up -d mysql redis rabbitmq nacos elasticsearch &&
                for i in \$(seq 1 60); do
                  if curl --noproxy '*' -fsS http://127.0.0.1:8848/nacos/v1/console/health/readiness >/dev/null; then
                    break
                  fi
                  echo \"waiting for nacos readiness... \$i\"
                  sleep 5
                done &&
                bash deploy/import-nacos-configs.sh &&
                bash deploy/scripts/apply-mysql-migrations.sh &&
                docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.images.yml up -d --no-build --remove-orphans &&
                sleep 30 &&
                bash deploy/post-deploy-check.sh http://127.0.0.1:18080
              "
            '''
          }
        }
      }
    }
  }
}
