#!/bin/bash

# Clean code. Used for things that go beyond what should be in
# pre-commit due to length of time they take.
sbt 'scalafix OrganizeImports'
