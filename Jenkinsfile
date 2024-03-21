pipeline {
    agent any
    tools {
        jdk 'jdk21'
    }
    stages {
        stage('Build') {
            steps {
                dir("${WORKSPACE}") {
                    sh "chmod +x mvnw"
                    sh 'mvnw clean package'
                }
            }
        }
        stage('Test') {
            steps {
                dir("${WORKSPACE}") {
                    sh 'mvnw test'
                }
            }
        }
        stage('Deploy') {
            steps {
                dir("${WORKSPACE}") {
                    sh 'mvnw deploy'
                }
            }
        }
    }
}