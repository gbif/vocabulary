## Notes

### Business logic

- Deprecation of a concept

A concept can be deprecated and replaced by another preferred concept. When doing so, any children of the concept being deprecated will be reassigned as children of the target.

A concept can be deprecated with no replacement only if it has no children.


- Validity check for concepts

When creating a new concept, we will validate that it doesn't conflict with other existing concepts. A conflict happens when there is already a concept with the same label, including alternative and misspelled labels. In case of conflict we won't allow the insertion.