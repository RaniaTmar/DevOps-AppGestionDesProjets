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
                sh 'sleep 20'
                sh 'docker compose ps'
                sh 'curl -sf http://localhost:8081/projet/all || (echo "Backend not responding" && exit 1)'
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
// pipeline test
