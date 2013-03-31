package demo

import scala.tools.nsc.{ Global, Phase }
import scala.tools.nsc.plugins.{ Plugin, PluginComponent }

class DemoPlugin(val global: Global) extends Plugin {
  import global._

  val name = "demo-plugin"
  val description = "Enforces coding standards"
  val components = List[PluginComponent](DemoComponent)

  private object DemoComponent extends PluginComponent {
    val global = DemoPlugin.this.global
    import global._

    override val runsAfter = List("erasure")

    val phaseName = "Demo"

    override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
      override def apply(unit: CompilationUnit) {
        new DemoTraverser(unit) traverse unit.body
      }
    }

    class DemoTraverser(unit: CompilationUnit) extends Traverser {
      override def traverse(tree: Tree): Unit = tree match {
        case New(tpt) if afterTyper(tpt.tpe.typeSymbol.isDerivedValueClass) =>
          unit.warning(tree.pos, s"Value class `${tpt.tpe.typeSymbol.fullName}` instantiated!")
        case _ =>
          super.traverse(tree)
      }
    }
  }
}
