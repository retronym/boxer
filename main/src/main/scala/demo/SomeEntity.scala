package demo

@entity
class SomeEntity {
  @node val x = 42
  @node def y(a: String) = 42
  @node val unused = new Object


  @entity
  class SomeNestedEntity {
    @node val x = 42
    @node def y(a: String) = 42
    @node val unused = new Object
  }

  def test: Unit = {
    @entity
    class SomeLocalEntity {
      @node val x = 42
      @node def y(a: String) = 42
      @node val unused = new Object
    }
  }
}


@entity
private[demo] class SomeEntity1 {
  @node val x = 42
  @node def y(a: String) = 42
  @node val unused = new Object
}

object SomeEntity1 {
  def publicity = 0
}
