# Vocabulary

[![Join the chat at https://gitter.im/vocabularyserver/community](https://badges.gitter.im/vocabularyserver/community.svg)](https://gitter.im/vocabularyserver/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

A simple registry of controlled vocabularies used for terms found in GBIF mediated data.

## Code style
The code formatting follows the Google Java format https://github.com/google/google-java-format

## How to run the project

To run the tests Docker has to be installed and running in the machine. You can find instructions about how to install 
Docker on Mac here https://docs.docker.com/docker-for-mac/ (see other platforms in the menu).

To compile the project just run 
```
mvn clean compile
```


## REST WS

### How to run the REST WS
To run the project in local you have to configure your database in the profile `local` of the [application.properties](vocabulary-rest-ws/src/main/resources/application.yml).
After that, you can run the project by specifying the local profile: 

```
java -jar vocabulary-rest-ws/target/vocabulary-rest-ws-0.20-SNAPSHOT.jar --spring.config.location=vocabulary-rest-ws/src/main/resources/application.yml --spring.profiles.active=local
```

### API Documentation
There is an API documentation available [here](vocabulary-rest-ws/src/docs/generated-docs) in HTML. 
