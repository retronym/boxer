## Exploring IntellIJ supoprt for the new implementation of scala-async

I am working on refactoring the implementation of scala-async. Much of the
transformation will now live in a compiler phase. The existing async macro
will remain as a thin front-end.

There are also third party integrations of async, either as different
front end macros https://github.com/foursquare/twitter-util-async,
or as a (closed source) compiler plugin that demarcates the async
boundaries with method annotations, more in the flavour of Kotlin's
suspendable functions.

While testing my async changes against the annotation driven
front end, I was curious if the special cases in IntellIJ's
debugger for async worked in that use case. This repository
contains an open source replication of the essential part of
the annotation driven async integration to help explore improving
the debugger experience.

## Build Scala Commit with Async phase (optional)

This step can be skipped and you can use the Scala binary from
"https://scala-ci.typesafe.com/artifactory/scala-pr-validation-snapshots/"
which is configured as a resolver 

  - checkout https://github.com/retronym/scala/commit/d4a86da
  - sbt setupPublishCore publisLocal

# Build compiler plugin
  
  - sbt update
  - sbt plugin/packageBin
  - ls plugin/target/scala-*/*.jar

# Debug user code in IntelliJ 
  
  - Either import SBT project into IntellIj or just use the .idea folder checked in
  - Set breakpoint in demo.Demo.main
  - Debug demo.Demo
  - While stopped, set further breakpoints at `val x = ` / `val y = below`. These are compiled
    into `Demo$stateMachine$async$1.apply() { ... }`.
  - The gutter icon changes quickly to "no executable code at line"
  - Resume debugger, and the breakpoints _do_ fire and the gutter icons turn red.
    Why are they not red initially?

# Notes

  - ScalaPositionManager contains special logic when the context in within an async { .. } macro call.
    I'm assuming this is to look for locals in fresh-named fields in the state machine class? Is
    there anything else? What can async do to make the debuggers life easier here? How can third
    party integratons of async participate here?
  - ScalaSyntheticManager also contains a special case to treat async's synthetic class name
    (...$stateMachine$..) as synthetic. This appears to be used to generate wildcard searches
    for host classes in the debugger, but I don't understand the code well enough to be sure. 
