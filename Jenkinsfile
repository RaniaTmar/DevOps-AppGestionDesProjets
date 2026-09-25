pipeline {
    agent any

    environment {
        COMPOSE_PROJECT_NAME = 'devops-appgestiondesprojets'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Stop previous stack') {
            steps {
                sh 'docker compose down || true'
            }
        }

        stage('Build images') {
            steps {
                sh 'docker compose build'
            }
        }

        stage('Deploy') {
            steps {
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
            sh 'docker compose logs --tail=50'
        }
    }
}
