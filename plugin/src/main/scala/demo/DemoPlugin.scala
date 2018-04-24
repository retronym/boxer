package demo

import scala.reflect.internal.Flags
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.transform.{Transform, TypingTransformers}

class DemoPlugin(val global: Global) extends Plugin {

  val name = "demo-plugin"
  val description = "Trait field injection"
  val components = List[PluginComponent](DemoComponent)

  private object DemoComponent extends PluginComponent with Transform with TypingTransformers {
    val global = DemoPlugin.this.global
    import global._

    override val runsAfter = List("superaccessors")
    override val runsBefore: List[String] = List("pickler")
    val phaseName = "demo"

    protected def newTransformer(unit: CompilationUnit): Transformer = {
      new FieldTransformer(unit)
    }

    class FieldTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {
      override def transform(tree: Tree): Tree = {
        tree match {
          case templ: Template =>
            val cls = templ.symbol.enclClass
            if (cls.name.string_==("T")) {
              val other = cls.info.decl(TermName("someVar"))
              println(other.defString)
              println(other.debugFlagString)
              println(other.info)
              println(templ.body.find(_.symbol == other).get.getClass)

              val getterSym = cls.newMethodSymbol(TermName("foo$impl"), cls.pos.focus, newFlags = Flags.ACCESSOR)
              getterSym.setInfoAndEnter(NullaryMethodType(definitions.IntTpe))
              val getter = localTyper.typedPos(getterSym.pos.focus)(ValDef(getterSym, Literal(Constant(-42))))

              val setterSym = cls.newMethodSymbol(getterSym.name.setterName, cls.pos.focus, newFlags = Flags.ACCESSOR)
              val setterParams = setterSym.newSyntheticValueParams(definitions.IntTpe :: Nil)
              setterSym.setInfoAndEnter(MethodType(setterParams, definitions.UnitTpe))
              val setter = localTyper.typedPos(setterSym.pos.focus)(DefDef(setterSym, EmptyTree))

              treeCopy.Template(templ, templ.parents, templ.self, templ.body ::: (getter :: setter :: Nil))
            } else {
              super.transform(templ)
            }
          case _ => super.transform(tree)
        }
      }
    }

  }
}
