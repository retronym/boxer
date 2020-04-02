package demo

import java.util.concurrent.CompletableFuture

import scala.concurrent.duration.Duration

object Demo {
  def main(args: Array[String]): Unit = {
    test
  }
  @customAsync
  def test: Any = {
    val x = reverse("abc")
    val y = reverse(x)
    (x, y)
  }
  @autoawait def reverse(a: String) = a.reverse
}

object MacroDemo extends MacroDemoImpl {
  def main(args: Array[String]): Unit = {
    test
  }
}
class MacroDemoImpl {
  def test = {
    import CompletableFutureAwait.{async, await}
    val pool = java.util.concurrent.Executors.newWorkStealingPool()
    def f1 = CompletableFuture.supplyAsync(() => 1, pool)

    val future: CompletableFuture[Int] = async(pool) {
      if ("".isEmpty) {
        val j = 1
        await(f1)
        identity(j)

        if ("".isEmpty) {
          val j = 1
          await(f1)
          identity(j)
          var i = 0
          while (i < 100) {
            i += await(f1) // a) Breakpoint set here shows as grey, although it does fire.
                           // b) cannot evaluate "i": "Cannot find local variable 'i'". Need to manually enter `this.i`
          }
        }
      }
      42
    }
    future.get()
  }
}

object OldAsyncMacroDemo extends OldAsyncMacroDemoImpl {
  def main(args: Array[String]): Unit = {
    test
  }
}
class OldAsyncMacroDemoImpl {
  def test = {
    import scala.async.Async.{async, await}
    import scala.concurrent._
    import ExecutionContext.Implicits.global
    def f1 = Future.successful(1)

    val future = async {
      // OK: Breakpoints in here show never show as grey.
      if ("".isEmpty) {
        val j = 1
        await(f1)
        identity(j)

        if ("".isEmpty) {
          val j = 1
          await(f1)
          identity(j)
          var i = 0
          while (i < 100) {
            i += await(f1)
            // b) cannot evaluate "j": "no such instance field j$macro".
            //    Since scala-async 1.0, the locals j an j ar lifted to j amd j$async$1.
            //    In prior versions. they used to be called j$macro$1 and j$macro$2.
            //    In the upcoming Scala async, they will be `j` and `j$n`
            //
            //    The most reliable way to identify these without naming conventions would be to scan
            //    the bytecode for the _first_ assignment to the field and associate this with the
            //    the source local val declaration with line number debug info.
          }
        }
      }
      42
    }
    Await.result(future, Duration.Inf)
  }
}
