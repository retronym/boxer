package demo

import scala.tools.nsc.{ Global, Phase }
import scala.tools.nsc.plugins.{ Plugin, PluginComponent }

class DemoPlugin(val global: Global) extends Plugin {
  import global._

  val name = "demo-plugin"
  val description = "saves original types"
  val components = List[PluginComponent](DemoTyperComponent, DemoErasureComponent)

  final case class OriginalTypeAttachment(tp: Type)

  private object DemoTyperComponent extends PluginComponent {
    val global: DemoPlugin.this.global.type = DemoPlugin.this.global
    import global._

    override val runsAfter = List("typer")

    val phaseName = "demo-plugin-typer"

    override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
      override def apply(unit: CompilationUnit) {
        new DemoTraverser(unit) traverse unit.body
      }
    }

    class DemoTraverser(unit: CompilationUnit) extends Traverser {
      override def traverse(tree: Tree): Unit = {
        tree.updateAttachment(OriginalTypeAttachment(tree.tpe))
        super.traverse(tree)
      }
    }
  }

  private object DemoErasureComponent extends PluginComponent {
    val global: DemoPlugin.this.global.type = DemoPlugin.this.global
    import global._

    override val runsAfter = List("erasure")

    val phaseName = "demo-plugin-erasure"

    override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
      override def apply(unit: CompilationUnit) {
        new DemoTraverser(unit) traverse unit.body
      }
    }

    class DemoTraverser(unit: CompilationUnit) extends Traverser {
      override def traverse(tree: Tree): Unit = {
        tree match {
          case _ if tree.isTerm && !tree.isEmpty =>
            tree.attachments.get[OriginalTypeAttachment] match {
              case Some(att) => println((tree, att.tp))
              case None =>
            }
          case _ =>
        }
        super.traverse(tree)
      }
    }
  }
}
