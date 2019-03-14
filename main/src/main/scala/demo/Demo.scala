package demo

class split() extends scala.annotation.StaticAnnotation

class Test {
  @split()
  def foo(x: Int) = x + 1
  @split()
  def bar(x: Int)(y: Int) = x + y
  @split()
  def baz = ???
}
