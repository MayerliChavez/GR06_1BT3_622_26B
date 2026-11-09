pipeline {
    agent any

    tools {
        maven 'Maven-3.9.16'
    }

    environment {
        DOCKER_IMAGE = 'mayerlichavez/pomora'
    }

    stages {

        stage('Descargar codigo') {
            steps {
                checkout scm
            }
        }

        stage('Compilar y probar') {
            steps {
                sh 'mvn -B clean verify'
            }
        }

        stage('Verificar archivo WAR') {
            steps {
                archiveArtifacts artifacts: 'target/*.war',
                                 fingerprint: true
            }
        }

        stage('Construir imagen Docker') {
            steps {
                script {
                    def tag = "${env.BUILD_NUMBER}"
                    sh """
                        docker build -t ${DOCKER_IMAGE}:${tag} .
                        docker tag ${DOCKER_IMAGE}:${tag} ${DOCKER_IMAGE}:latest
                    """
                }
            }
        }

        stage('Publicar en Docker Hub') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'dockerhub-credentials',
                            usernameVariable: 'DOCKER_USERNAME',
                            passwordVariable: 'DOCKER_PASSWORD'
                        )
                    ]) {
                        sh """
                            echo "\$DOCKER_PASSWORD" | docker login -u "\$DOCKER_USERNAME" --password-stdin
                            docker push ${DOCKER_IMAGE}:${BUILD_NUMBER}
                            docker push ${DOCKER_IMAGE}:latest
                            docker logout
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            junit testResults: 'target/surefire-reports/*.xml',
                  allowEmptyResults: true
        }
    }
}