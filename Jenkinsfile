pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            parallel {
                stage('order-service') {
                    steps {
                        dir('order-service') {
                            sh './mvnw -B verify'
                        }
                    }
                }
                stage('inventory-service') {
                    steps {
                        dir('inventory-service') {
                            sh './mvnw -B verify'
                        }
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    dir('order-service') {
                        sh './mvnw -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=order-service'
                    }
                    dir('inventory-service') {
                        sh './mvnw -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=inventory-service'
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Images') {
            when {
                branch 'main'
            }
            steps {
                sh 'docker build -t order-platform/order-service:${IMAGE_TAG} order-service'
                sh 'docker build -t order-platform/inventory-service:${IMAGE_TAG} inventory-service'
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/site/jacoco/jacoco.xml'
        }
    }
}
