# Vocabulary-lookup
This model is a utility to do lookups in a vocabulary. This library is intended to be as small as possible and 
with the least number of dependencies possible. This way it's easy to integrate it in other projects 
and avoid classpath problems.

To use this library you have to create a `VocabularyLookup` instance and load the vocabulary. There are 2 ways to do it:
1. Pass an `InputStream` of the exported vocabulary in json format.
```
VocabularyLookup vocabularyLookup = VocabularyLookup.load(new FileInputStream("my-vocabulary.json"));
```
2. Let the library download the latest version of the vocabulary:
```
VocabularyLookup vocabularyLookup = VocabularyDownloader.downloadLatestVocabularyVersion("http://api.gbif-dev.org/v1/", "LifeStage");
```

Once we have our instance created we can invoke the `lookup` method:
```
Optional<Concept> concept = vocabularyLookup.lookup("Adult");
```
As we can see, it returns an `Optional` object wich will be empty when we can't find a match for the received value.