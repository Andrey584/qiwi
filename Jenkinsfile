pipeline {
    agent any
    tools {
        jdk 'jdk21'
    }
    stages {
        stage('Build') {
            steps {
                script {
                    sh "chmod +x gradlew"
                    sh './mvnw clean package'
                }
            }
        }
        stage('Test') {
            steps {
                sh './mvnw test'
            }
        }
        stage('Deploy') {
            steps {
                sh './mvnw deploy'
            }
        }
    }
}