package demo

object EntityClient extends App {
  def is[T](t: T): T = t
  assert(is[TypeInfo[Int]](SomeEntity.x).str == "Int")
  assert(is[TypeInfo[String => Int]](SomeEntity.y).str == "String => Int")
  assert(SomeEntity.y.runtimeClass == classOf[Function1[_, _]])
  assert(is[TypeInfo[Int]](SomeEntity1.x).str == "Int")
  assert(is[TypeInfo[String => Int]](SomeEntity1.y).str == "String => Int")
  assert(SomeEntity1.y.runtimeClass == classOf[Function1[_, _]])
}
