## Boxer: a demo scalac plugin, embedded in your SBT project.

Ever wish you could inject some extra analysis into the
compiler pipeline for your project? You could write a compiler
plugin, package, distribute it, and depend on it, but the
activation energy for that is pretty high.

`Boxer` shows you how to embed a custom compiler plugin
directly into a sub-project of your SBT project. With this
in place, you can edit the plugin, run compile, and *immediately*
see the results in the context of your project.

### What's inside
 - [SBT Project Definition](https://github.com/retronym/boxer/blob/master/project/build.scala)
 - Compiler Plugin [Descriptor](https://github.com/retronym/boxer/blob/master/plugin/src/main/resources/scalac-plugin.xml) and
[Sources](https://github.com/retronym/boxer/blob/master/plugin/src/main/scala/demo/DemoPlugin.scala)

### Sample Output

    [info] Compiling 1 Scala source to /Users/jason/code/boxer/plugin/target/scala-2.9.2/classes...
    [info] Done updating.
    [info] Packaging /Users/jason/code/boxer/plugin/target/scala-2.9.2/plugin_2.9.2-0.1-SNAPSHOT.jar ...
    [info] Done packaging.
    [info] Compiling 1 Scala source to /Users/jason/code/boxer/main/target/scala-2.9.2/classes...
    [warn] /Users/jason/code/boxer/main/src/main/scala/demo/Demo.scala:8: Value class `Meter` instantiated!
    [warn]   println(m2)
    [warn]           ^
    [warn] one warning found