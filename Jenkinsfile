pipeline {
    agent any
    tools {
        jdk 'jdk21'
    }
    stages {
        stage('Test') {
            steps {
                sh './mvnw test'
            }
        }
        stage('Sonar') {
            steps {
                sh './mvnw sonar:sonar -Dsonar.projectKey=s3-file-mover -Dsonar.host.url=http://sonar.it-spectrum.ru -Dsonar.login=sqp_ee79e6043b0f6caa2bd61141ec7e61cdbbb45cd8'
            }
        }
         stage('Build') {
            steps {
                script {
                  sh "chmod +x mvnw"
                  sh './mvnw clean package'
                         }
                   }
           }
    }
}

