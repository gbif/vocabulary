pipeline {
    agent any
    tools {
        maven 'Maven 3.8.5'
        jdk 'OpenJDK11'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
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
                configFileProvider([configFile(
                        fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                        variable: 'MAVEN_SETTINGS_XML')]) {
                    sh 'mvn clean package dependency:analyze -U'
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
        stage('SonarQube analysis') {
            when {
                allOf {
                    not { expression { params.RELEASE } };
                }
            }
            steps {
                withSonarQubeEnv('GBIF Sonarqube') {
                    withCredentials([usernamePassword(credentialsId: 'SONAR_CREDENTIALS', usernameVariable: 'SONAR_USER', passwordVariable: 'SONAR_PWD')]) {
                        sh 'mvn sonar:sonar -Dsonar.login=${SONAR_USER} -Dsonar.password=${SONAR_PWD} -Dsonar.server=${SONAR_HOST_URL}'
                    }
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
                configFileProvider(
                        [configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                                variable: 'MAVEN_SETTINGS_XML')]) {
                    sh 'mvn -s $MAVEN_SETTINGS_XML -B -DskipTests deploy'
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
                RELEASE_ARGS = createReleaseArgs()
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
        stage('Deploy to DEV2') {
            environment {
                GIT_CREDENTIALS = credentials('4b740850-d7e0-4ab2-9eee-ecd1607e1e02')
                SERVICE_VOCABULARY = "${env.WORKSPACE}/service-vocabulary.yml"
                HOSTS_VOCABULARY = "${env.WORKSPACE}/hosts-vocabulary"
                BUILD_HOSTS = "${BUILD_ID}_hosts"
            }
            when {
                allOf {
                    not { expression { params.RELEASE } };
                    branch 'dev';
                }
            }
            steps {
                sshagent(['85f1747d-ea03-49ca-9e5d-aa9b7bc01c5f']) {
                    sh '''
                rm -rf *
                git clone -b master git@github.com:gbif/gbif-configuration.git
                git clone -b dev2 git@github.com:gbif/c-deploy.git
               '''

                    createServiceFile("${env.WORKSPACE}/gbif-configuration/environments/dev2/services.yml")
                    createHostsFile()

                    sh """
                cd c-deploy/services
                echo "Creating group_vars directory"
                mkdir group_vars

                # Configuration and services files are concatenated into a single file, that will contain the Ansible variables
                cat ../../gbif-configuration/environments/dev2/configuration.yml \
                    ../../gbif-configuration/environments/dev2/monitoring.yml \
                    ${SERVICE_VOCABULARY} >> group_vars/${BUILD_ID}

                # The default Ansible inventory file 'hosts' is concatenated with the input HOSTS file
                cat ../../gbif-configuration/environments/dev2/hosts \
                    ${HOSTS_VOCABULARY} >> ${BUILD_HOSTS}

                # Executes the Ansible playbook
                echo "Executing Ansible playbook"
                ansible-playbook -vvv -i ${BUILD_HOSTS} services.yml --private-key=~/.ssh/id_rsa --extra-vars "git_credentials=${GIT_CREDENTIALS}"
              """
                }
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

void createServiceFile(String servicesPath) {
    def allServices = readYaml file: servicesPath

    def vocabularyService
    for (service in allServices.services) {
        if (service.artifactId == "vocabulary-rest-ws") {
            vocabularyService = service
            break
        }
    }

    if (vocabularyService) {
        sh """
    cat <<-EOF> ${SERVICE_VOCABULARY}
    services: [
    {
      groupId: ${vocabularyService.groupId},
      artifactId: ${vocabularyService.artifactId},
      packaging: jar,
      version: ${vocabularyService.version},
      framework: ${vocabularyService.framework},
      testOnDeploy: ${vocabularyService.testOnDeploy},
      httpPort: ${vocabularyService.httpPort},
      httpAdminPort: ${vocabularyService.httpAdminPort},
      useFixedPorts: 0
    }
    ]
    EOF
    """.stripIndent()
    }
}

void createHostsFile() {
    sh """
    cat <<-EOF> ${HOSTS_VOCABULARY}
    # this group is used to allow parallel executions of the Ansible scripts using different files of variables
    [${BUILD_ID}:children]
    appserver
    mapserver
    varnish5
    nagios
    EOF
  """.stripIndent()
}

def createReleaseArgs() {
    def args = ""
    if (params.RELEASE_VERSION != '') {
        args += "-DreleaseVersion=${params.RELEASE_VERSION} "
    }
    if (params.DEVELOPMENT_VERSION != '') {
        args += "-DdevelopmentVersion=${params.DEVELOPMENT_VERSION} "
    }
    if (params.DRY_RUN_RELEASE) {
        args += "-DdryRun=true"
    }

    return args
}
