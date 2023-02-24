# Vocabulary importer

This is a tool to do the initial import of a vocabulary from a spreadsheet. Currently it's possible to use only a 
subset of the fields that a `vocabulary` and a `concept` supports. 
The rest of the fields have to be added manually through the API or the UI after the initial import.

The vocabulary concepts and labels have to be in CSV format - other separators are also accepted.

It's necessary to have the following 2 CSVs:
* **Concepts**

It contains the concepts of the vocabulary with its labels and alternative labels. At this moment, both EN and ES labels are accepted.

The columns of this file are (note that it uses a semicolon as separator):

```
Concept;Parent;Label_en;AlternativeLabels_en;Label_es;AlternativeLabels_es;Definition_en;sameAsUris;externalDefinitions
```

For example, for `LifeStage` we could have a concepts file like this:

```
Concept;Parent;Label_en;AlternativeLabels_en;Label_es;AlternativeLabels_es;Definition_en;sameAsUris;externalDefinitions
Egg;;Egg;;Huevo;;The egg is the organic vessel containing the zygote in which an embryo develops until it can survive on its own, at which point the animal hatches;;
Chick;;Chick;;Polluelo;;A chick is a bird that has not yet reached adulthood;;
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

**NOTE**: the names of the columns of these files can be different than those but the order has to be the one specified above.

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

For example:

```
java -jar vocabulary-importer/target/vocabulary-importer-0.37-SNAPSHOT.jar \ 
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

The jar can be obtained from this source code after building the project:

```
mvn clean package
```

or downloaded from our nexus repository:

https://repository.gbif.org/service/rest/repository/browse/gbif/org/gbif/vocabulary/vocabulary-importer/

If there were issues during the import the tool creates a file called `errors_{timestamp}` in the directory where it was run where we can see all these issues. 

Most of the issues should be related with duplicates that cannot be imported. Some of the restrictions are documented in the [core module](https://github.com/gbif/vocabulary/blob/master/core/notes.md).

