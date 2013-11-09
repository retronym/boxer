class Cell[+T](val value: T)

object Cell {
  Cell.of(1)

  def of[T](value: T) = new Cell(value)
}
