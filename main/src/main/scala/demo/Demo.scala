package demo

class split() extends scala.annotation.StaticAnnotation

class Test {
  @split()
  def foo(x: Int) = x + 1
  @split()
  def bar(x: Int)(y: Int) = x + y
  @split()
  def baz = ???

  @split
  def fail[TT](foo: R[_ <: TT]): X[TT] = {
    foo.toX[TT]
  }
}

// https://github.com/scala/bug/issues/11383
trait R[T] {
  def toX[K >: T]: X[K]
}
trait X[X]
