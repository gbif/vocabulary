# Vocabulary REST WS

## How to run the REST WS
To run the project in your local machine you have to use an application.properties with all the needed configurations like
the one for the database. This is an example of a properties file that can be used:

```
spring:
  application.name: vocabulary-rest-ws
  jackson.default-property-inclusion: NON_EMPTY
  datasource:
    url: jdbc:postgresql://your_host:your_port/your_db_name
    username: your_user
    password: your_password

management:
  server.port: 8081
  endpoints.web.exposure.include: "*"
  endpoint.shutdown.enabled: true

security:
  loginApiBasePath: http://api.gbif-dev.org/v1

export:
  releaseEnabled: false
```

The DB scheme can be recreated by using the [Liquibase files](core/src/main/resources/liquibase).

After that, you can run the project with this command and by using your IDE: 

```
java -jar vocabulary-rest-ws/target/vocabulary-rest-ws-0.20-SNAPSHOT.jar --spring.config.location=vocabulary-rest-ws/src/main/resources/application.yml
```

## API Documentation
There is an API documentation available as [HTML files](vocabulary-rest-ws/src/docs/generated-docs). A preview is available [here](https://htmlpreview.github.io/?https://github.com/gbif/vocabulary/blob/master/vocabulary-rest-ws/src/docs/generated-docs/index.html). 
