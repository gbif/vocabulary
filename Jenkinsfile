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
        stage('documentation') {
          steps{
            git credentialsId: '4b740850-d7e0-4ab2-9eee-ecd1607e1e02', url: 'https://github.com/gbif/vocabulary.git'
            sh 'mvn clean package verify'
            sh('git commit *.html -m "API Documentation"')
            sh('git push origin master')
          }
        }
        stage('build') {
            when{ not { expression { params.RELEASE } } }
            steps {
              configFileProvider([configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                                             variable: 'MAVEN_SETTINGS_XML')]) {
                sh 'mvn clean package verify dependency:analyze -U'
              }
              sshagent(['85f1747d-ea03-49ca-9e5d-aa9b7bc01c5f']) {
                sh('git commit *.html -m "API Documentation"')
                sh('git push gbif-jenkins@github.com:gbif/vocabulary.git master')
              }
            }
        }
        stage('SonarQube analysis') {
            when{ not { expression { params.RELEASE } } }
            steps{
              withSonarQubeEnv('GBIF Sonarqube') {
                sh 'mvn sonar:sonar'
              }
            }
        }
        stage('release snapshot to nexus') {
            when{ allOf { not { expression { params.RELEASE } };
                          branch 'master' } }
            steps {
              configFileProvider([configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                                             variable: 'MAVEN_SETTINGS_XML')]) {
                sh 'mvn -s $MAVEN_SETTINGS_XML deploy'
              }
            }
        }
        stage('release version to nexus') {
          when{ expression { params.RELEASE } }
          steps {
            configFileProvider([configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                                                         variable: 'MAVEN_SETTINGS_XML')]) {
              git 'https://github.com/gbif/vocabulary.git'
              sh 'mvn -s $MAVEN_SETTINGS_XML -B release:prepare release:perform'
            }
          }
        }
    }
}