# Notes

## Business logic

### Vocabulary
- Validity check for vocabularies 

  A new vocabulary will be created as long as there is no conflict with other vocabularies.
  A conflict happens when there is already another vocabulary with the same label.
  This also checks deleted and replaced vocabularies, it doesn't make sense to create a vocabulary that was already deleted.

- Deleting a vocabulary
  
  A vocabulary that has concepts cannot be deleted - if we don't do so the concepts will still appear in the searches.
  
  
  ?? There is an option to delete a vocabulary and all its concepts -> this implies that we have to allow the deletion of concepts
  
- Restoring a vocabulary

  ?? When a deleted vocabulary is restored, there is an option to restore all its deleted concepts -> only if we finally do the previous point of deleting concepts


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
  - A concept can be deprecated with no replacement only if it has no children.
  - There is an option to deprecate a concept and all its children - children will be deprecated without replacement.
    
- Restore a deprecated concept
  - Fields deprecated, deprecatedBy and replacedBy are set to null.    
  - If the restored concept has a parent deprecated, we update it with the current replacement.
  - There is an option to restore a concept and all its deprecated children.
