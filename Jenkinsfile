pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        REGISTRY = 'localhost:8083'
        NEXUS_MAVEN_URL = 'http://nexus:8081/repository/maven-snapshots/'
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

        stage('Publish to Nexus') {
            when {
                expression { env.GIT_BRANCH == 'origin/main' || env.BRANCH_NAME == 'main' }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'nexus', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASSWORD')]) {
                    writeFile file: 'nexus-settings.xml', text: """<settings>
  <servers>
    <server>
      <id>nexus</id>
      <username>\${env.NEXUS_USER}</username>
      <password>\${env.NEXUS_PASSWORD}</password>
    </server>
  </servers>
</settings>"""
                    script {
                        ['order-service', 'inventory-service'].each { service ->
                            dir(service) {
                                sh "./mvnw -B -s ../nexus-settings.xml deploy -DskipTests -DaltDeploymentRepository=nexus::${NEXUS_MAVEN_URL}"
                            }
                            sh "docker build -t ${REGISTRY}/order-platform/${service}:${IMAGE_TAG} -t ${REGISTRY}/order-platform/${service}:latest ${service}"
                        }
                    }
                    sh 'echo "$NEXUS_PASSWORD" | docker login ${REGISTRY} -u "$NEXUS_USER" --password-stdin'
                    script {
                        ['order-service', 'inventory-service'].each { service ->
                            sh "docker push ${REGISTRY}/order-platform/${service}:${IMAGE_TAG}"
                            sh "docker push ${REGISTRY}/order-platform/${service}:latest"
                        }
                    }
                }
            }
            post {
                always {
                    sh 'rm -f nexus-settings.xml'
                }
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
