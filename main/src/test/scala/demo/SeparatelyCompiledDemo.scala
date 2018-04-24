package demo

class SeparatelyCompiledDemo extends T {

}

object SeparatelyCompiledDemo {
  def main(args: Array[String]): Unit = {
    val d = new SeparatelyCompiledDemo
    val cls = classOf[SeparatelyCompiledDemo]

    val getter = cls.getDeclaredMethod("foo$impl")
    val x = getter.invoke(d)
    println(x)
    assert(x == -42)

    val setter = cls.getDeclaredMethod("foo$impl_$eq", java.lang.Integer.TYPE)
    setter.invoke(d, Int.box(-1))
    assert(getter.invoke(d) == -1)
  }
}
