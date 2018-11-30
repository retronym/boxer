package demo.macros

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object Macros {
  def macrotopia: String = macro impl
  def impl(c: blackbox.Context): c.Tree = {
    import c.universe._
    Literal(Constant(System.nanoTime().toString))
  }
}
