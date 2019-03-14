pipeline {
    agent any
    tools {
        maven 'Maven3.2'
        jdk 'JDK8'
      }
    parameters {
       booleanParam(
          name: 'RELEASE',
          defaultValue: false,
          description: 'Do a Maven release')
    }
    stages {
        stage('build') {
            steps {
              sh 'mvn clean package verify'
            }
        }
        stage('release') {
          when{ expression { params.RELEASE } }
          steps {
            echo 'release'
          }
        }
        stage('release snapshot') {
          when{ not { expression { params.RELEASE } } }
          steps {
            echo 'snapshot'
          }
        }
    }
}