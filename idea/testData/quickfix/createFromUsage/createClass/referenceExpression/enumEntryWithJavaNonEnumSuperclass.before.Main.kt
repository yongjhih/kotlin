// "Create enum constant 'A'" "false"
// ACTION: Convert to block body
// ACTION: Create member property 'A'
// ACTION: Rename reference
// ERROR: Unresolved reference: A
internal fun foo(): X = E.<caret>A
