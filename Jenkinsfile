pipeline {
    agent any

    tools {
        maven 'M3'                      // must match Manage Jenkins → Tools
    }

    environment {
        COMPOSE_PROJECT_NAME = 'devops-appgestiondesprojets'
        BACKEND_DIR   = '.'             // folder containing the backend pom.xml (e.g. 'backend')
        FRONTEND_DIR  = 'frontend'      // folder containing the frontend Dockerfile
        DOCKER_USER   = 'your-dockerhub-username'
        BACKEND_IMAGE = "${DOCKER_USER}/gestion-projets-backend"
        FRONTEND_IMAGE = "${DOCKER_USER}/gestion-projets-frontend"
        NEXUS_URL     = 'http://192.168.33.10:8083/repository/maven-releases/'
    }

    stages {
        // Stage 1
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // Stage 2
        stage('Maven Compile') {
            steps {
                dir(env.BACKEND_DIR) {
                    sh 'mvn clean compile'
                }
            }
        }

        // Stage 3 (tests first, so SonarQube gets the JaCoCo coverage report)
        stage('Maven Test') {
            steps {
                dir(env.BACKEND_DIR) {
                    sh 'mvn test'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: "${env.BACKEND_DIR}/target/surefire-reports/*.xml"
                }
            }
        }

        // Stage 4 (Atelier 6)
        stage('SonarQube') {
            steps {
                dir(env.BACKEND_DIR) {
                    withSonarQubeEnv('SonarQube') {
                        sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar'
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // Stage 5
        stage('Maven Package') {
            steps {
                dir(env.BACKEND_DIR) {
                    sh 'mvn package -DskipTests'
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        // Stage 6
        stage('Maven Deploy') {
            steps {
                dir(env.BACKEND_DIR) {
                    withCredentials([usernamePassword(credentialsId: 'nexus-creds',
                                                      usernameVariable: 'NEXUS_USER',
                                                      passwordVariable: 'NEXUS_PASS')]) {
                        sh '''
                            cat > settings-ci.xml <<EOF
<settings>
  <servers>
    <server>
      <id>nexus</id>
      <username>${NEXUS_USER}</username>
      <password>${NEXUS_PASS}</password>
    </server>
  </servers>
</settings>
EOF
                            mvn deploy -DskipTests -s settings-ci.xml
                            rm -f settings-ci.xml
                        '''
                    }
                }
            }
        }

        // Stage 7 (continuous delivery)
        stage('Docker Build & Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                                                  usernameVariable: 'DH_USER',
                                                  passwordVariable: 'DH_PASS')]) {
                    sh '''
                        echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin

                        docker build -t $BACKEND_IMAGE:${BUILD_NUMBER} -t $BACKEND_IMAGE:latest $BACKEND_DIR
                        docker build -t $FRONTEND_IMAGE:${BUILD_NUMBER} -t $FRONTEND_IMAGE:latest $FRONTEND_DIR

                        docker push $BACKEND_IMAGE:${BUILD_NUMBER}
                        docker push $BACKEND_IMAGE:latest
                        docker push $FRONTEND_IMAGE:${BUILD_NUMBER}
                        docker push $FRONTEND_IMAGE:latest

                        docker logout
                    '''
                }
            }
        }

        // Stage 8
        stage('Docker Compose Up') {
            steps {
                sh 'docker compose down || true'
                sh 'docker compose up -d --build'
            }
        }

        stage('Verify') {
            steps {
                sh 'docker compose ps'
                sh '''
                    echo "Waiting for backend to become ready..."
                    for i in $(seq 1 30); do
                        if curl -sf http://localhost:8081/projet/all > /dev/null; then
                            echo "Backend is up!"
                            break
                        fi
                        echo "Attempt $i/30: backend not ready yet, waiting 5s..."
                        sleep 5
                    done
                    curl -sf http://localhost:8081/projet/all || (echo "Backend never became ready" && exit 1)
                '''
                sh 'curl -sf http://localhost:80 || (echo "Frontend not responding" && exit 1)'
            }
        }
    }

    post {
        failure {
            sh 'docker compose logs --tail=50 || true'
        }
    }
}
