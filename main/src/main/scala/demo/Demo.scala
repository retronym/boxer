class Meter(val value: Double) extends AnyVal {
  def +(other: Meter) = new Meter(value + other.value)
}

object Demo {
  val m = new Meter(1d)
  val m2 = m + m
  println(m2)
}

object UseMacro {
  println(demo.macros.Macros.macrotopia)
  new demo.support.Support().foo
}
