class Meter(val value: Double) extends AnyVal {
  def +(other: Meter) = new Meter(value + other.value)
}

object Demo {
  val m = new Meter(1d)
  val m2 = m + m
  println(m2)
}
