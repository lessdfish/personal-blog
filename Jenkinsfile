pipeline {
  agent any

  environment {
    VM_HOST = '192.168.147.129'
    VM_USER = 'deploy'
    DEPLOY_DIR = '/opt/blog-cloud/source'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Backend Build Check') {
      steps {
        sh 'mvn -ntp -DskipTests package'
      }
    }

    stage('Deploy To VM') {
      steps {
        withCredentials([
          file(credentialsId: 'blog-cloud-env-file', variable: 'BLOG_CLOUD_ENV')
        ]) {
          sshagent(credentials: ['vm-deploy-key']) {
            sh '''
              set +x

              ssh ${VM_USER}@${VM_HOST} "mkdir -p ${DEPLOY_DIR}"

              rsync -az --delete \
                --exclude .git \
                --exclude .env \
                --exclude target \
                --exclude node_modules \
                ./ ${VM_USER}@${VM_HOST}:${DEPLOY_DIR}/

              scp "$BLOG_CLOUD_ENV" ${VM_USER}@${VM_HOST}:${DEPLOY_DIR}/.env.tmp

              ssh ${VM_USER}@${VM_HOST} "
                cd ${DEPLOY_DIR} &&
                mv .env.tmp .env &&
                chmod 600 .env &&
                docker compose -f docker-compose.yml -f deploy/docker/compose.production.yml up -d --build
              "
            '''
          }
        }
      }
    }
  }
}