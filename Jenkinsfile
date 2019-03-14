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
            when{ not { expression { params.RELEASE } } }
            steps {
              configFileProvider([configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709')]) {
                sh 'mvn clean package -DskipTests'
                sh 'mvn deploy'
              }
            }
        }
        stage('release') {
          when{ expression { params.RELEASE } }
          steps {
            echo 'release'
          }
        }
    }
}