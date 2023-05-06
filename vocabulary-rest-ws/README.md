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
  actuatorSecret: secret
  loginApiBasePath: http://api.gbif-dev.org/v1

export:
  releaseEnabled: false
```

Also, we need a bootstrap.yml file to disable the zookeeper registration in local development:
```
spring:
  cloud:
    zookeeper:
      enabled: false
      discovery:
        enabled: false
```

The DB scheme can be recreated by using the [Liquibase files](core/src/main/resources/liquibase).

After that, you can run the project with this command and by using your IDE:

```
java -jar vocabulary-rest-ws/target/vocabulary-rest-ws-{your-version}.jar --spring.config.location=your_path/application.yml --spring.cloud.bootstrap.location=your_path/bootstrap.yml
```
