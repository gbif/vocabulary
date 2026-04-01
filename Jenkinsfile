@Library('gbif-common-jenkins-pipelines') _

pipeline {
    agent any
    tools {
        maven 'Maven 3.9.9'
        jdk 'OpenJDK17'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }
    triggers {
        snapshotDependencies()
    }
    parameters {
        separator(name: "release_separator", sectionHeader: "Release Parameters")
        booleanParam(name: 'RELEASE',
                defaultValue: false,
                description: 'Do a Maven release')
        string(name: 'RELEASE_VERSION', defaultValue: '', description: 'Release version (optional)')
        string(name: 'DEVELOPMENT_VERSION', defaultValue: '', description: 'Development version (optional)')
        booleanParam(name: 'DRY_RUN_RELEASE', defaultValue: false, description: 'Dry Run Maven release')
    }
    stages {
        stage('Preconditions') {
            steps {
                scmSkip(skipPattern: '.*(\\[maven-release-plugin\\] prepare release |Google Java Format).*')
            }
        }
        stage('Package and unit tests') {
            when {
                allOf {
                    not { expression { params.RELEASE } };
                }
            }
            steps {
                withMaven (
                    maven: 'Maven 3.9.9',
                    globalMavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                    mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1396361652540',
                    traceability: true){
                        sh 'mvn clean package install dependency:analyze -U'
                    }
            }
        }
        stage('Integration tests') {
            when {
                allOf {
                    not { expression { params.RELEASE } };
                }
            }
            steps {
                configFileProvider([configFile(
                        fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                        variable: 'MAVEN_SETTINGS_XML')]) {
                    sh 'mvn clean verify -Pintegration -U'
                }
            }
        }
        stage('Snapshot to nexus') {
            when {
                allOf {
                    not { expression { params.RELEASE } };
                    anyOf {
                        branch 'dev';
                    }
                }
            }
            steps {
                withMaven (
                    maven: 'Maven 3.9.9',
                    globalMavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                    mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1396361652540',
                    traceability: true){
                        sh 'mvn -B -DskipTests deploy'
                    }
            }
        }
        stage('Release version to nexus') {
            when {
                allOf {
                    expression { params.RELEASE };
                    branch 'master';
                }
            }
            environment {
                RELEASE_ARGS = utils.createReleaseArgs(params.RELEASE_VERSION, params.DEVELOPMENT_VERSION, params.DRY_RUN_RELEASE)
            }
            steps {
                configFileProvider(
                        [configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                                variable: 'MAVEN_SETTINGS_XML')]) {
                    git 'https://github.com/gbif/vocabulary.git'
                    sh 'mvn -s $MAVEN_SETTINGS_XML -B release:prepare release:perform $RELEASE_ARGS'
                }
            }
        }
        stage('Trigger WS deploy dev') {
          when {
            allOf {
              not { expression { params.RELEASE } };
              branch 'dev';
            }
          }
          steps {
            build job: "vocabulary-dev-deploy", wait: false, propagate: false
          }
        }
    }
    post {
        failure {
            slackSend message: "Vocabulary build failed! - ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)",
                    channel: "#jenkins"
        }
    }
}

