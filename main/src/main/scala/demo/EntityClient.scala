package demo

object EntityClient extends App {
  def is[T](t: T): T = t
  assert(is[TypeInfo[Int]](SomeEntity.x).str == "Int")
  assert(is[TypeInfo[String => Int]](SomeEntity.y).str == "String => Int")
  assert(SomeEntity.y.runtimeClass == classOf[Function1[_, _]])
}
