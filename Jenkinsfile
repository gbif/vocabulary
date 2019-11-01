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
          description: 'Do a Maven release (it also generates API documentation)')
       booleanParam(
          name: 'DOCUMENTATION',
          defaultValue: false,
          description: 'Generate API documentation')
    }
    stages {
        stage('build') {
            when{ not { expression { params.RELEASE } } }
            steps {
              configFileProvider([configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                                             variable: 'MAVEN_SETTINGS_XML')]) {
                sh 'mvn clean package verify dependency:analyze -U'
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
        stage('snapshot to nexus') {
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
        stage('Generate API documentation') {
          when{ anyOf { expression { params.RELEASE }; expression { params.DOCUMENTATION } } }
          steps{
            sshagent(['85f1747d-ea03-49ca-9e5d-aa9b7bc01c5f']) {
              sh '''
                mvn clean package -Pdocumentation'
                git add *.html'
                git commit -m "Generated API documentation"'
                git push git@github.com:gbif/vocabulary.git master
              '''
            }
          }
        }
        stage('Deploy to DEV') {
          environment {
            GIT_CREDENTIALS = credentials('4b740850-d7e0-4ab2-9eee-ecd1607e1e02')
          }
          steps {
            sshagent(['85f1747d-ea03-49ca-9e5d-aa9b7bc01c5f']) {
              sh '''
                rm -rf *
                git clone -b master git@github.com:gbif/gbif-configuration.git
                git clone -b master git@github.com:gbif/c-deploy

                cd c-deploy/services
                echo "Creating group_vars directory"
                mkdir group_vars

                # create service for vocabulary
                cat <<EOF > tt
a
a
EOF

                cat > service.yml <<-'EOF'
                services: [
                {
                  groupId: org.gbif.vocabulary,
                  artifactId: vocabulary-rest-ws,
                  packaging: jar,
                  version: LATEST,
                  framework: spring,
                  testOnDeploy: 1,
                  httpPort: 8114,
                  httpAdminPort: 8115,
                  useFixedPorts: 0
                }
                ]
                EOF

                # Configuration and services files are concatenated into a single file, that will contain the Ansible variables
                cat ../../gbif-configuration/environments/dev/configuration.yml \
                    ../../gbif-configuration/environments/dev/monitoring.yml \
                    service.yml >> group_vars/$BUILD_ID

                # create hosts for this build
                cat > hosts <<-'EOF'
                [$BUILD_ID:children]
                appserver
                mapserver
                varnish5
                nagios
                EOF

                # The default Ansible inventory file 'hosts' is concatenated with the input HOSTS file
                cat ../../gbif-configuration/environments/dev/hosts hosts >> $BUILD_ID_hosts

                # Executes the Ansible playbook
                echo "Executing Ansible playbook"

                ansible-playbook -vvv -i $BUILD_ID_hosts services.yml --private-key=~/.ssh/id_rsa --extra-vars "git_credentials=$GIT_CREDENTIALS"
              '''
            }
          }
        }
    }
}