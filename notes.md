## Notes

### Business logic

- Deletion of a vocabulary

  A vocabulary that has concepts associated cannot be deleted.
  There is an option to delete a vocabulary and all its concepts.

- Deprecation and deletion of a concept

  A concept can be deprecated and replaced by another preferred concept. When doing so, any children of the concept being deprecated will be reassigned as children of the target.
  
  A concept that has children cannot be deleted.
  There is an option to delete a concept and all its children.

- Validity check for vocabularies 

  A new vocbaulary will be created as long as there is no conflict with other vocabularies.
  A conflict happens when there is already another vocabulary with the same label.
  This check also checks deleted and replaced vocabularies, it doesn't make sense to create a vocabulary that was already deleted.

- Validity check for concepts 

  A new concept will be created as long as there is no conflict with other concepts.
  A conflict happens when there is already another concept with the same label, including alternative and misspelled labels.
  This check also checks deleted and replaced concepts, it doesn't make sense to create a concept that was already deleted.
  
  
- Update of a concept
   - When restoring a concept, if it has a parent replaced by another concept, we update it
     with the current replacement.
   - When restoring a concept, if it has a replacement replaced by another concept, we update
     it with the current replacement.
   - When replacing a concept, if it has children we reassign them to the new replacement.
   - When replacing a concept, if there are concepts that were replaced by this concept, we
   set their replacement to the new one.
  
- Restore a vocabulary

  When a vocabulary is restored, there is an option to restore all its deleted concepts.
  
- Restore a concept

  When a concept is restored, its parent and/or its replacement will be updated. It could be the case that its former parent or replacement 
  is deprecated and another concept has to be used instead. 
  Also, there is an option to restore all its deleted children.
  