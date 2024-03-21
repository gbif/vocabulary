# Vocabulary importer

### Full import of a vocabulary and its concepts
This is a tool to do the initial import of a vocabulary from a spreadsheet. Currently it's possible to use only a
subset of the fields that a `vocabulary` and a `concept` supports.
The rest of the fields have to be added manually through the API or the UI after the initial import.

The vocabulary concepts and labels have to be in CSV format - other separators are also accepted.

It's necessary to have the following 2 CSVs:
* **Concepts**

It contains the concepts of the vocabulary with its labels and alternative labels. To indicate the language of a label,
alternative label or definition, it has to be suffixed to the column name as "_{locale}", for example, alternativeLabels_es-es.
The values for the locales can be found [here](../model/src/main/java/org/gbif/vocabulary/model/LanguageRegion.java).

The columns of this file are (note that it uses a semicolon as separator):

```
Concept;Parent;Label_en;AlternativeLabels_en;Label_es-es;AlternativeLabels_es-es;Definition_en;sameAsUris;externalDefinitions;tags
```

For example, for `LifeStage` we could have a concepts file like this:

```
Concept;Parent;Label_en;AlternativeLabels_en;Label_es-es;AlternativeLabels_es-es;Definition_en;sameAsUris;externalDefinitions;tags
Egg;;Egg;;Huevo;;The egg is the organic vessel containing the zygote in which an embryo develops until it can survive on its own, at which point the animal hatches;;;tag1|tag2
Chick;;Chick;;Polluelo;;A chick is a bird that has not yet reached adulthood;;;
```

For columns that accept a list of values, such as alternative labels, sameAs uris and external definitions, we should use a delimiter to separate them.
A pipe("|") should be used as default but another one can be used if it conflicts in some vocabularies (see below how to specify a custom delimiter in the tool).

* **Hidden labels**

It contains the hidden labels of the concepts of the vocabulary. Each row can contain only 1 hidden label.

The columns for this file are (note that it uses a semicolon as separator):

```
Concept;Hidden_label
```

For the `LifeStage` example we could have a labels file like this:

```
Concept;Hidden_label
Egg;Eggs
Egg;Huevo
Egg;Huevos
```

### Import of hidden labels
The tool allows to import only the hidden labels to existing concepts of a vocabulary. In this case we only need the hidden labels
CSV that it's described in the section above. This is an example:

```
java -jar vocabulary-importer/target/vocabulary-importer-1.0.3.jar \
--vocabularyName LifeStage \
--importHiddenLabelsOnly \
--hiddenLabelsPath "/mydir/my_hidden_labels.csv" \
--csvDelimiter ";" \
--apiUrl https://api.gbif-dev.org/v1/ \
--apiUser myusername \
--apiPassword
```

**NOTE**: the columns of these files can be in any order but the column name has to match with the ones specified above(the case doesn't matter).

## How to import a vocabulary

This tool can be run via command line by sending these params:

* *--vocabularyName, -vn*

    Name of the vocabulary to import. It's the unique identifier of the vocabulary that will be used in the URLs

* *--vocabularyLabelEN, -vlen*

    Label for the vocabulary in EN language

* *--vocabularyDefinitionEN, -vden (Optional)*

    Definition of the vocabulary in EN language

* *--conceptsPath, -cp*

    Path of the CSV that contains the concepts of the vocabulary

* *--hiddenLabelsPath, -hp (Optional)*

    Path of the CSV that contains the hidden labels of the vocabulary

* *--csvDelimiter, -d  (Optional)*

    Delimiter of the CSV files for concepts and hidden labels. If not specified it uses a comma (",").
    Note that some delimiters may need to be escaped for Java, e.g.: \\\\|

* *--listDelimiter, -ld (Optional)*

    Delimiter to specify multiple values in the alternative labels. If not specified it uses a pipe ("|").
    Note that some delimiters may need to be escaped for Java, e.g.: \\\\|

* *--apiUrl, -a*

    Base URL of the vocabulary API where the vocabulary will be created, e.g.: https://api.gbif-dev.org/v1/

* *--apiUser, -au*

    User for the API. It has to be a vocabulary admin

* *--apiPassword, -ap*

    Password of the user. It has to be blank in the command and it will be prompted on the console

* *--encoding, -enc*

    Encoding of the CSV files. If not specified it uses UTF-8

* *--importHiddenLabelsOnly, -hlo*

    Flag to indicate the import of only hidden labels to existing concepts. When this option is used the concepts path
    and the vocabulary label and definition are not required.
* *--importLabelsAndDefinitionsOnly", -ldo*

    Flag to indicate the import of only labels, alternative labels and definitions to existing concepts. None of these
    columns are required so, for example, it can be used to import only definitions. When this option is used the vocabulary
    label and definition are not required.

Example of a migration:

```
java -jar vocabulary-importer/target/vocabulary-importer-1.0.3.jar \
--vocabularyName LifeStage \
--vocabularyLabelEN "Life Stage" \
--vocabularyDefinitionEN "A vocabulary to capture the broad stages that an organism passes through during its life cycle. This vocabulary was assembled based on the observed terms commonly used by the open data community, including those from citizen scientists." \
--conceptsPath "/mydir/my_concepts.csv" \
--hiddenLabelsPath "/mydir/my_hidden_labels.csv" \
--csvDelimiter ";" \
--apiUrl https://api.gbif-dev.org/v1/ \
--apiUser myusername \
--apiPassword
```

Example of an only-labels import :

```
java -jar vocabulary-importer/target/vocabulary-importer-1.0.3.jar \
--vocabularyName LifeStage \
--importLabelsAndDefinitionsOnly \
--conceptsPath "/mydir/my_concepts.csv" \
--csvDelimiter ";" \
--apiUrl https://api.gbif-dev.org/v1/ \
--apiUser myusername \
--apiPassword
```

The jar can be obtained from this source code after building the project:

```
mvn clean package
```

or downloaded from our nexus repository:

https://repository.gbif.org/service/rest/repository/browse/gbif/org/gbif/vocabulary/vocabulary-importer/

If there were issues during the import the tool creates a file called `errors_{timestamp}` in the directory where it was run where we can see all these issues.

Most of the issues should be related with duplicates that cannot be imported. Some of the restrictions are documented in the [core module](https://github.com/gbif/vocabulary/blob/master/core/notes.md).

### How to migrate a vocabulary across environments

The importer tool can also be used to migrate vocabularies across environments. To do so we just need to set the `migration` flag and specify the target API
where the vocabulary has to be copied to, e.g.:

```
java -jar vocabulary-importer/target/vocabulary-importer-1.0.3.jar \
--vocabularyName LifeStage \
--migration \
--apiUrl https://api.gbif-dev.org/v1/ \
--targetApiUrl https://api.gbif-uat.org/v1/ \
--apiUser myusername \
--targetApiUser myusername \
--apiPassword \
--targetApiPassword
```

Note that you will be prompted to enter 2 passwords for each of the users.
