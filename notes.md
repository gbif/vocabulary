## Notes

### Business logic

- Deprecation of a concept

  A concept can be deprecated and replaced by another preferred concept. When doing so, any children of the concept being deprecated will be reassigned as children of the target.
  
  A concept can be deprecated with no replacement only if it has no children.


- Validity check for concepts

  A new concept will be created as long as there is no conflict with other concepts.
  A conflict happens when there is already another concept with the same label, including alternative and misspelled labels.