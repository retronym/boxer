package demo

import java.util.concurrent.CompletableFuture

import scala.annotation.StaticAnnotation

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
            i += await(f1) // a) Breakpoint set here shows as grey, although it does fire. b) cannot evaluate "i" (need to manually enter `this.i`)
          }
        }
      }
      42
    }
    future.get()
  }
}




