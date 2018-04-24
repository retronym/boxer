## Trait Field injection

Trying to mimic creation of a var in a trait by a post-typer, pre-pickler compiler
phase.

Tested under joint and separately compiled subclasses, see output below:

### What's inside

 - [SBT Project Definition](https://github.com/retronym/boxer/blob/master/project/build.scala)
 - Compiler Plugin [Descriptor](https://github.com/retronym/boxer/blob/master/plugin/src/main/resources/scalac-plugin.xml) and
[Sources](https://github.com/retronym/boxer/blob/master/plugin/src/main/scala/demo/DemoPlugin.scala)

### Sample Output

```
⚡ sbt
sbt:root> ;clean;main/compile:run;main/test:run
[success] Total time: 0 s, completed 24/04/2018 10:31:36 PM
[info] Updating plugin...
[info] Updating main...
[info] Done updating.
[info] Compiling 1 Scala source to /Users/jz/code/boxer/plugin/target/scala-2.12/classes ...
[info] Done updating.
[info] Done compiling.
[info] Packaging /Users/jz/code/boxer/plugin/target/scala-2.12/boxer_2.12-0.1.0-SNAPSHOT.jar ...
[info] Done packaging.
[info] Compiling 1 Scala source to /Users/jz/code/boxer/main/target/scala-2.12/classes ...
def someVar: Int
<method> <accessor>
=> Int
class scala.reflect.internal.Trees$ValDef
[[syntax trees at end of                      demo]] // Demo.scala
package demo {
  abstract trait T extends scala.AnyRef {
    def /*T*/$init$(): Unit = {
      ()
    };
    <accessor> val someVar: Int = 42;
    <accessor> def someVar_=(x$1: Int): Unit;
    <accessor> val foo$impl: Int = -42;
    <accessor> def foo$impl_=(x$1: Int): Unit = {
      -42;
      ()
    }
  };
  class Demo extends AnyRef with demo.T {
    def <init>(): demo.Demo = {
      Demo.super.<init>();
      ()
    }
  };
  object Demo extends scala.AnyRef {
    def <init>(): demo.Demo.type = {
      Demo.super.<init>();
      ()
    };
    def main(args: Array[String]): Unit = {
      val d: demo.Demo = new Demo();
      val cls: Class[demo.Demo] = classOf[demo.Demo];
      val getter: java.lang.reflect.Method = cls.getDeclaredMethod("foo$impl");
      val x: Object = getter.invoke(d);
      scala.Predef.println(x);
      scala.Predef.assert(x.==(-42));
      val setter: java.lang.reflect.Method = cls.getDeclaredMethod("foo$impl_$eq", java.lang.Integer.TYPE);
      setter.invoke(d, scala.Int.box(-1));
      scala.Predef.assert(getter.invoke(d).==(-1))
    }
  }
}

[[syntax trees at end of                    fields]] // Demo.scala
package demo {
  abstract trait T extends Object {
    def /*T*/$init$(): Unit = {
      ()
    };
    <accessor> <sub_synth> def someVar(): Int = 42.asInstanceOf[Int]();
    <accessor> <sub_synth> def someVar_=(x$1: Int): Unit;
    <accessor> <sub_synth> def foo$impl(): Int = -42.asInstanceOf[Int]();
    <accessor> <sub_synth> def foo$impl_=(x$1: Int): Unit = {
      -42;
      ()
    }
  };
  class Demo extends Object with demo.T {
    override <accessor> def someVar(): Int = Demo.this.someVar.asInstanceOf[Int]();
    private[this] var someVar: Int = _;
    override <accessor> def someVar_=(x$1: Int): Unit = Demo.this.someVar = x$1.asInstanceOf[Int]();
    override <accessor> def foo$impl(): Int = Demo.this.foo$impl.asInstanceOf[Int]();
    private[this] var foo$impl: Int = _;
    override <accessor> def foo$impl_=(x$1: Int): Unit = Demo.this.foo$impl = x$1.asInstanceOf[Int]();
    def <init>(): demo.Demo = {
      Demo.super.<init>();
      ()
    }
  };
  object Demo extends Object {
    def <init>(): demo.Demo.type = {
      Demo.super.<init>();
      ()
    };
    def main(args: Array[String]): Unit = {
      val d: demo.Demo = new demo.Demo();
      val cls: Class[demo.Demo] = classOf[demo.Demo];
      val getter: java.lang.reflect.Method = cls.getDeclaredMethod("foo$impl", Array[Class[_]]{});
      val x: Object = getter.invoke(d, Array[Object]{});
      scala.Predef.println(x);
      scala.Predef.assert(x.==(-42));
      val setter: java.lang.reflect.Method = cls.getDeclaredMethod("foo$impl_$eq", Array[Class[_]]{java.lang.Integer.TYPE});
      setter.invoke(d, Array[Object]{scala.Int.box(-1)});
      scala.Predef.assert(getter.invoke(d, Array[Object]{}).==(-1))
    }
  }
}

[[syntax trees at end of                     mixin]] // Demo.scala
package demo {
  abstract trait T extends Object {
    <accessor> <sub_synth> def someVar(): Int;
    <accessor> <sub_synth> def someVar_=(x$1: Int): Unit;
    <accessor> <sub_synth> def foo$impl(): Int;
    <accessor> <sub_synth> def foo$impl_=(x$1: Int): Unit;
    def /*T*/$init$(): Unit = {
      T.this.someVar_=((42: Int));
      T.this.foo$impl_=((-42: Int));
      ()
    }
  };
  class Demo extends Object with demo.T {
    override <accessor> def someVar(): Int = (Demo.this.someVar: Int);
    private[this] var someVar: Int = _;
    override <accessor> def someVar_=(x$1: Int): Unit = Demo.this.someVar = (x$1: Int);
    override <accessor> def foo$impl(): Int = (Demo.this.foo$impl: Int);
    private[this] var foo$impl: Int = _;
    override <accessor> def foo$impl_=(x$1: Int): Unit = Demo.this.foo$impl = (x$1: Int);
    def <init>(): demo.Demo = {
      Demo.super.<init>();
      Demo.super./*T*/$init$();
      ()
    }
  };
  object Demo extends Object {
    def main(args: Array[String]): Unit = {
      val d: demo.Demo = new demo.Demo();
      val cls: Class = classOf[demo.Demo];
      val getter: java.lang.reflect.Method = cls.getDeclaredMethod("foo$impl", Array[Class]{});
      val x: Object = getter.invoke(d, Array[Object]{});
      scala.Predef.println(x);
      scala.Predef.assert(x.==(scala.Int.box(-42)));
      val setter: java.lang.reflect.Method = cls.getDeclaredMethod("foo$impl_$eq", Array[Class]{java.lang.Integer.TYPE});
      setter.invoke(d, Array[Object]{scala.Int.box(-1)});
      scala.Predef.assert(getter.invoke(d, Array[Object]{}).==(scala.Int.box(-1)))
    };
    def <init>(): demo.Demo.type = {
      Demo.super.<init>();
      ()
    }
  }
}

[info] Done compiling.
[info] Packaging /Users/jz/code/boxer/main/target/scala-2.12/boxer_2.12-0.1.0-SNAPSHOT.jar ...
[info] Done packaging.
[info] Running demo.Demo
-42
[success] Total time: 1 s, completed 24/04/2018 10:31:37 PM
[info] Packaging /Users/jz/code/boxer/plugin/target/scala-2.12/boxer_2.12-0.1.0-SNAPSHOT.jar ...
[info] Done packaging.
[info] Packaging /Users/jz/code/boxer/main/target/scala-2.12/boxer_2.12-0.1.0-SNAPSHOT.jar ...
[info] Done packaging.
[info] Compiling 1 Scala source to /Users/jz/code/boxer/main/target/scala-2.12/test-classes ...
[[syntax trees at end of                      demo]] // SeparatelyCompiledDemo.scala
package demo {
  class SeparatelyCompiledDemo extends AnyRef with demo.T {
    def <init>(): demo.SeparatelyCompiledDemo = {
      SeparatelyCompiledDemo.super.<init>();
      ()
    }
  };
  object SeparatelyCompiledDemo extends scala.AnyRef {
    def <init>(): demo.SeparatelyCompiledDemo.type = {
      SeparatelyCompiledDemo.super.<init>();
      ()
    };
    def main(args: Array[String]): Unit = {
      val d: demo.SeparatelyCompiledDemo = new SeparatelyCompiledDemo();
      val cls: Class[demo.SeparatelyCompiledDemo] = classOf[demo.SeparatelyCompiledDemo];
      val getter: java.lang.reflect.Method = cls.getDeclaredMethod("foo$impl");
      val x: Object = getter.invoke(d);
      scala.Predef.println(x);
      scala.Predef.assert(x.==(-42));
      val setter: java.lang.reflect.Method = cls.getDeclaredMethod("foo$impl_$eq", java.lang.Integer.TYPE);
      setter.invoke(d, scala.Int.box(-1));
      scala.Predef.assert(getter.invoke(d).==(-1))
    }
  }
}

[[syntax trees at end of                    fields]] // SeparatelyCompiledDemo.scala
package demo {
  class SeparatelyCompiledDemo extends Object with demo.T {
    override <accessor> def someVar(): Int = SeparatelyCompiledDemo.this.someVar.asInstanceOf[Int]();
    private[this] var someVar: Int = _;
    override <accessor> def someVar_=(x$1: Int): Unit = SeparatelyCompiledDemo.this.someVar = x$1.asInstanceOf[Int]();
    override <accessor> def foo$impl(): Int = SeparatelyCompiledDemo.this.foo$impl.asInstanceOf[Int]();
    private[this] var foo$impl: Int = _;
    override <accessor> def foo$impl_=(x$1: Int): Unit = SeparatelyCompiledDemo.this.foo$impl = x$1.asInstanceOf[Int]();
    def <init>(): demo.SeparatelyCompiledDemo = {
      SeparatelyCompiledDemo.super.<init>();
      ()
    }
  };
  object SeparatelyCompiledDemo extends Object {
    def <init>(): demo.SeparatelyCompiledDemo.type = {
      SeparatelyCompiledDemo.super.<init>();
      ()
    };
    def main(args: Array[String]): Unit = {
      val d: demo.SeparatelyCompiledDemo = new demo.SeparatelyCompiledDemo();
      val cls: Class[demo.SeparatelyCompiledDemo] = classOf[demo.SeparatelyCompiledDemo];
      val getter: java.lang.reflect.Method = cls.getDeclaredMethod("foo$impl", Array[Class[_]]{});
      val x: Object = getter.invoke(d, Array[Object]{});
      scala.Predef.println(x);
      scala.Predef.assert(x.==(-42));
      val setter: java.lang.reflect.Method = cls.getDeclaredMethod("foo$impl_$eq", Array[Class[_]]{java.lang.Integer.TYPE});
      setter.invoke(d, Array[Object]{scala.Int.box(-1)});
      scala.Predef.assert(getter.invoke(d, Array[Object]{}).==(-1))
    }
  }
}

[[syntax trees at end of                     mixin]] // SeparatelyCompiledDemo.scala
package demo {
  class SeparatelyCompiledDemo extends Object with demo.T {
    override <accessor> def someVar(): Int = (SeparatelyCompiledDemo.this.someVar: Int);
    private[this] var someVar: Int = _;
    override <accessor> def someVar_=(x$1: Int): Unit = SeparatelyCompiledDemo.this.someVar = (x$1: Int);
    override <accessor> def foo$impl(): Int = (SeparatelyCompiledDemo.this.foo$impl: Int);
    private[this] var foo$impl: Int = _;
    override <accessor> def foo$impl_=(x$1: Int): Unit = SeparatelyCompiledDemo.this.foo$impl = (x$1: Int);
    def <init>(): demo.SeparatelyCompiledDemo = {
      SeparatelyCompiledDemo.super.<init>();
      SeparatelyCompiledDemo.super./*T*/$init$();
      ()
    }
  };
  object SeparatelyCompiledDemo extends Object {
    def main(args: Array[String]): Unit = {
      val d: demo.SeparatelyCompiledDemo = new demo.SeparatelyCompiledDemo();
      val cls: Class = classOf[demo.SeparatelyCompiledDemo];
      val getter: java.lang.reflect.Method = cls.getDeclaredMethod("foo$impl", Array[Class]{});
      val x: Object = getter.invoke(d, Array[Object]{});
      scala.Predef.println(x);
      scala.Predef.assert(x.==(scala.Int.box(-42)));
      val setter: java.lang.reflect.Method = cls.getDeclaredMethod("foo$impl_$eq", Array[Class]{java.lang.Integer.TYPE});
      setter.invoke(d, Array[Object]{scala.Int.box(-1)});
      scala.Predef.assert(getter.invoke(d, Array[Object]{}).==(scala.Int.box(-1)))
    };
    def <init>(): demo.SeparatelyCompiledDemo.type = {
      SeparatelyCompiledDemo.super.<init>();
      ()
    }
  }
}

[info] Done compiling.
[info] Packaging /Users/jz/code/boxer/main/target/scala-2.12/boxer_2.12-0.1.0-SNAPSHOT-tests.jar ...
[info] Done packaging.
[info] Running demo.SeparatelyCompiledDemo
-42
[success] Total time: 0 s, completed 24/04/2018 10:31:38 PM
```