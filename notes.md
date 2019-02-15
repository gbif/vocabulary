# Notes

## Business logic

### Vocabulary
- Validity check for vocabularies 

  A new vocbaulary will be created as long as there is no conflict with other vocabularies.
  A conflict happens when there is already another vocabulary with the same label.
  This also checks deleted and replaced vocabularies, it doesn't make sense to create a vocabulary that was already deleted.

- Deleting a vocabulary
  
  A vocabulary that has concepts cannot be deleted - if we don't do so the concepts will still appear in the searches.
  There is an option to delete a vocabulary and all its concepts.
  
- Restoring a vocabulary

  When a deleted vocabulary is restored, there is an option to restore all its deleted concepts.


### Concept  
- Validity check for concepts 

  A new concept will be created as long as there is no conflict with other concepts.
  A conflict happens when there is already another concept with the same label, including alternative and misspelled labels.
  This also checks deleted and replaced concepts, it doesn't make sense to create a concept that was already deleted or deprecated.
  
- Deprecation and deletion of a concept

  Deprecation and deletion of a concept are 2 different things. The first suggests that a concept shouldn't be used anymore, and it has to be public to let everybody know.
  A deletion suggests that a concept should be removed and it doesn't have to appear anymore.
  
- Deprecating a concept
  - Any children of the concept being deprecated will be reassigned as children of the target.
  
  - ?? If there are concepts that were deprecated by this concept, we set their replacement to the new one. (we'd lose deprecation history)
  
- Deleting a concept
  - A concept that has children cannot be deleted - if we don't do so the children will still appear in the searches.
  - There is an option to delete a concept and all its children.
    
- Restoring a concept
   - There is an option to restore all its deleted children.
   
   - ?? If the restored concept has a parent replaced by another concept, we update it with the current replacement.
   - ?? If the restored concept has a replacement replaced by another concept, we update it with the current replacement.
