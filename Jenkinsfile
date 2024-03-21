pipeline {
    agent any
    tools {
        jdk 'jdk21'
    }
    stages {
        stage('Build') {
            steps {
                script {
                    sh "chmod +x mvnw"
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
        stage('Sonar') {
            steps {
                sh './mvnw clean verify sonar:sonar -Dsonar.projectKey=s3-file-mover -Dsonar.host.url=http://sonar.it-spectrum.ru -Dsonar.login=sqp_ee79e6043b0f6caa2bd61141ec7e61cdbbb45cd8'
            }
        }