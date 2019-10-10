# Vocabulary

[![Join the chat at https://gitter.im/vocabularyserver/community](https://badges.gitter.im/vocabularyserver/community.svg)](https://gitter.im/vocabularyserver/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) 
[![Quality Gate Status](https://sonar.gbif.org/api/project_badges/measure?project=org.gbif.vocabulary%3Avocabulary-parent&metric=alert_status)](https://sonar.gbif.org/dashboard?id=org.gbif.vocabulary%3Avocabulary-parent) 
[![Coverage](https://sonar.gbif.org/api/project_badges/measure?project=org.gbif.vocabulary%3Avocabulary-parent&metric=coverage)](https://sonar.gbif.org/dashboard?id=org.gbif.vocabulary%3Avocabulary-parent)

A simple registry of controlled vocabularies used for terms found in GBIF mediated data.

## Code style
The code formatting follows the [Google Java format](https://github.com/google/google-java-format).

## How to run the project

To run the tests Docker has to be installed and running in the machine. You can find instructions about how to install 
Docker on Mac on https://docs.docker.com/docker-for-mac/ (see other platforms in the menu).

To compile the project just run 
```
mvn clean compile
```

## REST WS

### How to run the REST WS
To run the project in your local machine you have to configure your database in the profile `local` of the [application.properties](vocabulary-rest-ws/src/main/resources/application.yml). The DB scheme can be recreated by using the [Liquibase files](core/src/main/resources/liquibase).

After that, you can run the project by specifying the local profile: 

```
java -jar vocabulary-rest-ws/target/vocabulary-rest-ws-0.20-SNAPSHOT.jar --spring.config.location=vocabulary-rest-ws/src/main/resources/application.yml --spring.profiles.active=local
```

### API Documentation
There is an API documentation available as [HTML files](vocabulary-rest-ws/src/docs/generated-docs). A preview is available [here](https://htmlpreview.github.io/?https://github.com/gbif/vocabulary/blob/master/vocabulary-rest-ws/src/docs/generated-docs/index.html). 


## Core module
Some of the business rules applied in the core module are available as [notes](core/notes.md) along with a brief explanation.
The DB model is also availabe in a [diagram](core/db_model.png)  
