---
description: Explain a class, concept, interface, or design decision in the RefineJ codebase. Great for onboarding or understanding unfamiliar code. Pass the class name, concept, or question as the argument.
argument-hint: <class name | concept | question>
---
Explain the following in the context of the RefineJ codebase: **$ARGUMENTS**

Steps:
1. Search the codebase with Glob/Grep to locate relevant files.
2. Read the identified files in full.
3. Reference `docs/ARCHITECTURE.adoc` and `docs/DETAILED-DESIGN.adoc` for design context where relevant.

Your explanation should cover:
- **What it is**: purpose and responsibility
- **Where it lives**: module and package
- **How it fits**: interactions with other components (dependencies, callers, implementors)
- **Key design decisions**: why it was built this way (reference ROADMAP phase if applicable)
- **Code walkthrough**: walk through the key methods/fields with inline references (`file:line`)

Keep the explanation concise but complete. Assume the reader knows Java and Spring Boot but is new to RefineJ.
