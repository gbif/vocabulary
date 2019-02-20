# Notes

## Business logic

### Vocabulary
- Validity check for vocabularies 

  A new vocabulary will be created as long as there is no conflict with other vocabularies.
  A conflict happens when there is already another vocabulary with the same label.
  This also checks deleted and replaced vocabularies, it doesn't make sense to create a vocabulary that was already deleted.

- Deleting a vocabulary

  A vocabulary cannot be deleted, only deprecated.
  
- Deprecate a vocabulary
  - A vocabulary that has concepts cannot be deprecated - if we don't do so the concepts will still appear in the searches.
  - There is an option to deprecate a vocabulary and all its concepts.
  - A vocabulary can be deprecated with or without replacement.
  - When deprecating a vocabulary with replacement, its concepts will NOT be reassigned to the replacement - if we would reassign them it will be like 
    a duplicate of the vocabulary
  
- Restoring a deprecated vocabulary
  - Fields deprecated, deprecatedBy and replacedBy are set to null.    
  - There is an option to restored a deprecated vocabulary and all its deprecated concepts.

### Concept  
- Validity check for concepts 

  A new concept will be created as long as there is no conflict with other concepts.
  A conflict happens when there is already another concept with the same label, including alternative and misspelled labels.
  This also checks deprecated concepts, it doesn't make sense to create a concept that was already deprecated.
  
- Deleting a concept

  A concept cannot be deleted, only deprecated.
  
- Deprecating a concept
  - A concept can be deprecated with or without replacement.
  - Any children of the concept being deprecated will be reassigned as children of the target.
  - A concept can be deprecated with no replacement only if it has no children or if we deprecate all the children too.
  - There is an option to deprecate a concept and all its children - children will be deprecated without replacement.
    
- Restore a deprecated concept
  - Fields deprecated, deprecatedBy and replacedBy are set to null.    
  - If the restored concept has a parent deprecated, we update it with the current replacement.
  - There is an option to restore a concept and all its deprecated children.
  - A deprecated concept cannot be restored if its vocabulary is deprecated.