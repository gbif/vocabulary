pipeline {
    agent any
    tools {
       maven 'Maven3.2'
       jdk 'JDK8'
    }
    options {
       buildDiscarder(logRotator(numToKeepStr: '5'))
    }
    parameters {
       booleanParam(
          name: 'RELEASE',
          defaultValue: false,
          description: 'Do a Maven release (it also generates API documentation)')
       booleanParam(
          name: 'DOCUMENTATION',
          defaultValue: false,
          description: 'Generate API documentation')
    }
    stages {
        stage('Build') {
            when { allOf {
                    not { expression { params.RELEASE } };
                    not { expression { params.DOCUMENTATION } };
                    branch 'master'; // TODO: remove
            } }
            steps {
              configFileProvider([configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                                             variable: 'MAVEN_SETTINGS_XML')]) {
                sh 'mvn clean package verify dependency:analyze -U'
              }
            }
        }
        stage('SonarQube analysis') {
            when { allOf {
                    not { expression { params.RELEASE } };
                    not { expression { params.DOCUMENTATION } };
                    branch 'master'; // TODO: remove
            } }
            steps {
              withSonarQubeEnv('GBIF Sonarqube') {
                sh 'mvn sonar:sonar'
              }
            }
        }
        stage('Snapshot to nexus') {
            when { allOf {
                    not { expression { params.RELEASE } };
                    not { expression { params.DOCUMENTATION } };
                    branch 'master';
             } }
            steps {
              configFileProvider([configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                                             variable: 'MAVEN_SETTINGS_XML')]) {
                sh 'mvn -s $MAVEN_SETTINGS_XML deploy'
              }
            }
        }
        stage('Release version to nexus') {
          when { expression { params.RELEASE } }
          steps {
            configFileProvider([configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                                                         variable: 'MAVEN_SETTINGS_XML')]) {
              git 'https://github.com/gbif/vocabulary.git'
              sh 'mvn -s $MAVEN_SETTINGS_XML -B release:prepare release:perform'
            }
          }
        }
        stage('Generate API documentation') {
          when { anyOf { expression { params.RELEASE }; expression { params.DOCUMENTATION } } }
          steps {
            sshagent(['85f1747d-ea03-49ca-9e5d-aa9b7bc01c5f']) {
              git 'https://github.com/gbif/vocabulary.git'
              sh '''
                mvn clean package -Pdocumentation
                git add *.html
                git commit -m "Generated API documentation"
                git push git@github.com:gbif/vocabulary.git master
              '''
            }
          }
        }
        stage('Deploy to DEV') {
          environment {
            GIT_CREDENTIALS = credentials('4b740850-d7e0-4ab2-9eee-ecd1607e1e02')
            SERVICE_VOCABULARY = "service-vocabulary.yml"
            HOSTS_VOCABULARY = "hosts-vocabulary"
          }
          when { allOf {
            not { expression { params.RELEASE } };
            not { expression { params.DOCUMENTATION } }//;
//             branch 'master' TODO: uncomment
          } }
          steps {
            sshagent(['85f1747d-ea03-49ca-9e5d-aa9b7bc01c5f']) {
              sh '''
                rm -rf *
                git clone -b master git@github.com:gbif/gbif-configuration.git
                git clone -b master git@github.com:gbif/c-deploy
               '''

               createServiceFile("${env.WORKSPACE}/gbif-configuration/environments/dev/services.yml")
               createHostsFile()

              sh """
                cd c-deploy/services
                echo "Creating group_vars directory"
                mkdir group_vars

                # Configuration and services files are concatenated into a single file, that will contain the Ansible variables
                cat ../../gbif-configuration/environments/dev/configuration.yml \
                    ../../gbif-configuration/environments/dev/monitoring.yml \
                    ${env.WORKSPACE}/${SERVICE_VOCABULARY} >> group_vars/${BUILD_ID}

                # The default Ansible inventory file 'hosts' is concatenated with the input HOSTS file
                cat ../../gbif-configuration/environments/dev/hosts \
                    ${env.WORKSPACE}/${HOSTS_VOCABULARY} >> ${BUILD_ID}_hosts

                # Executes the Ansible playbook
                echo "Executing Ansible playbook"

                ansible-playbook -vvv -i ${BUILD_ID}_hosts services.yml --private-key=~/.ssh/id_rsa --extra-vars "git_credentials=$GIT_CREDENTIALS"
              """
            }
          }
        }
    }
    post {
      failure {
        mail to: 'mlopez@gbif.org',
             subject: "Failed Vocabulary Pipeline: ${currentBuild.fullDisplayName}",
             body: "Something is wrong with ${env.BUILD_URL}"
      }
    }
}

void createServiceFile(String servicesPath) {
 def allServices = readYaml file: servicesPath

 def vocabularyService
 for(service in allServices.services){
  if (service.artifactId == "vocabulary-rest-ws") {
    vocabularyService = service
  }
 }

  sh """
    cat <<-EOF> ${env.WORKSPACE}/${SERVICE_VOCABULARY}
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

void createHostsFile() {
  sh """
    cat <<-EOF> ${env.WORKSPACE}/${HOSTS_VOCABULARY}
    # this group is used to allow parallel executions of the Ansible scripts using different files of variables
    [${BUILD_ID}:children]
    appserver
    mapserver
    varnish5
    nagios
    EOF
  """.stripIndent()
}