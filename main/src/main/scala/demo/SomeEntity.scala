package demo

@entity
class SomeEntity {
  @node val x = 42
  @node def y(a: String) = 42
  @node val unused = new Object
}
