pipeline {
    agent any

    tools {
        maven 'M2_HOME'                       // must match the name in Manage Jenkins -> Tools
    }

    environment {
        COMPOSE_PROJECT_NAME = 'devops-appgestiondesprojets'
        BACKEND_DIR = 'backend'
        DOCKER_USER = 'raniaat12'   // CHANGE ME (same as your Docker Hub login)
        TAG         = "${BUILD_NUMBER}"
    }

    stages {
        // Stage 1: get code from Git
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // Stage 2: Maven compile
        stage('Maven Compile') {
            steps {
                dir(env.BACKEND_DIR) {
                    sh 'mvn clean compile'
                }
            }
        }

        // Stage 3: tests first, so SonarQube receives the JaCoCo coverage report
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

        // Stage 4: SonarQube analysis (Atelier 6)
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

        // Stage 5: build the .jar
        stage('Maven Package') {
            steps {
                dir(env.BACKEND_DIR) {
                    sh 'mvn package -DskipTests'
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        // Stage 6: mvn deploy -DskipTests (publish the jar to Nexus)
        stage('Maven Deploy') {
            steps {
                dir(env.BACKEND_DIR) {
                    withCredentials([usernamePassword(credentialsId: 'nexus-creds',
                                                      usernameVariable: 'NEXUS_USER',
                                                      passwordVariable: 'NEXUS_PASS')]) {
                        sh '''
                            printf '<settings><servers><server><id>nexus</id><username>%s</username><password>%s</password></server></servers></settings>' "$NEXUS_USER" "$NEXUS_PASS" > settings-ci.xml
                            mvn deploy -DskipTests -s settings-ci.xml && rc=0 || rc=$?
                            rm -f settings-ci.xml
                            exit $rc
                        '''
                    }
                }
            }
        }

        // Stage 7: Docker image, login and push
        stage('Docker Build & Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                                                  usernameVariable: 'DH_USER',
                                                  passwordVariable: 'DH_PASS')]) {
                    sh '''
                        echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin

                        docker compose build backend frontend
                        docker compose push backend frontend

                        for svc in backend frontend; do
                            docker tag $DOCKER_USER/gestion-projets-$svc:$TAG $DOCKER_USER/gestion-projets-$svc:latest
                            docker push $DOCKER_USER/gestion-projets-$svc:latest
                        done

                        docker logout
                    '''
                }
            }
        }

        // Stage 8: docker compose up
        stage('Docker Compose Up') {
            steps {
                sh 'docker compose down || true'
                sh 'docker compose up -d'
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
