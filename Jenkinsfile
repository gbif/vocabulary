pipeline {
    agent any
    tools {
        maven 'Maven3.2'
        jdk 'JDK8'
      }
    stages {
        stage('build') {
            steps {
              sh 'mvn clean package verify'
            }
        }
    }
}