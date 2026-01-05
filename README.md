# Vocabulary

[![Build Status](https://builds.gbif.org/job/vocabulary/job/master/badge/icon)](https://builds.gbif.org/job/vocabulary/job/master/)
[![Quality Gate Status](https://sonar.gbif.org/api/project_badges/measure?project=org.gbif.vocabulary%3Avocabulary-parent&metric=alert_status&token=sqb_73699e1c05e321fd451545b47ae6b3fcbd06046b)](https://sonar.gbif.org/dashboard?id=org.gbif.vocabulary%3Avocabulary-parent)
[![Coverage](https://sonar.gbif.org/api/project_badges/measure?project=org.gbif.vocabulary%3Avocabulary-parent&metric=coverage&token=sqb_73699e1c05e321fd451545b47ae6b3fcbd06046b)](https://sonar.gbif.org/dashboard?id=org.gbif.vocabulary%3Avocabulary-parent)

A simple registry of controlled vocabularies used for terms found in GBIF mediated data.

## Code style
The code formatting follows the [Google Java format](https://github.com/google/google-java-format).

## How to run the project

To run the tests Docker has to be installed and running in the machine. You can find instructions about how to install
Docker on Mac on https://docs.docker.com/docker-for-mac/ (see other platforms in the menu).

To compile the project just run:
```
mvn clean compile
```

## Modules

- [Core](core/README.md): contains the persistence and the service layer
- [Model](model/README.md): contains all the model classes
- [Rest-WS](vocabulary-rest-ws/README.md): a REST ws that exposes the public API
- [Vocabulary-lookup](vocabulary-lookup/README.md): utility library to do lookups in a vocabulary
- [Vocabulary-cli](vocabulary-cli/README.md): CLI applications
- [Vocabulary-importer](vocabulary-importer/README.md): tool to import vocabularies
