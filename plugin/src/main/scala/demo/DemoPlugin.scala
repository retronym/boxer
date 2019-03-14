package demo

import scala.collection.mutable.ListBuffer
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.transform.{InfoTransform, TypingTransformers}

class DemoPlugin(val global: Global) extends Plugin {

  val name = "demo-plugin"
  val description = "Creates sibling methods"
  val components = List[PluginComponent](DemoMethodSplitterComponent)
  import global._
  import definitions._

  private val EntityAnnotName = TypeName("entity")
  private val NodeAnnotName = TypeName("node")
  private lazy val TypeInfoClass = rootMirror.staticClass("demo.TypeInfo")

  private object DemoMethodSplitterComponent extends PluginComponent with InfoTransform with TypingTransformers {
    override val global: DemoPlugin.this.global.type = DemoPlugin.this.global
    override val phaseName: String = "demo-method-splitter"
    override val runsAfter: List[String] = List("refchecks")

    protected def newTransformer(unit: CompilationUnit): Transformer = new MethodSplitterTransformer(unit)
    private val needTrees = perRunCaches.newAnyRefMap[Symbol, Symbol]()
    private val contName = newTermName("$cont")

    override def transformInfo(sym: Symbol, tpe: Type): Type = {
      def needsTransform(symbol: Symbol) = !sym.isJavaDefined && !sym.hasPackageFlag && sym.isClass

      tpe match {
        case GenPolyType(tparams, ClassInfoType(parents, decls, sym)) if needsTransform(sym) =>
          // Force info transform of parents.
          // This is not really needed in this simple example, but it would be needed if
          // the logic below were to look for inherited "$impl" methods.
          afterOwnPhase(parents map (_.typeSymbol.info))

          val newDecls = newScope
          var changed = false
          for (decl <- tpe.decls) {
            if (decl.isMethod && decl.annotations.exists(_.symbol.name.string_==("split"))) {
              changed = true
              val impl = decl.cloneSymbol(decl.owner, decl.flags, decl.name.append("$impl"))

              def cps(tp: Type): Type = {
                def contParam(restpe: Type) = decl.newSyntheticValueParam(functionType(restpe :: Nil, UnitTpe), contName).setPos(decl.pos)
                tp match {
                  case PolyType(tparams, restpe) =>
                    PolyType(tparams, restpe)
                  case MethodType(params, restpe: MethodType) =>
                    MethodType(params, cps(restpe))
                  case MethodType(params, restpe) =>
                    val params1 = params :+ contParam(restpe)
                    MethodType(params1, UnitTpe)
                  case NullaryMethodType(restpe) =>
                    val params1 = contParam(restpe) :: Nil
                    MethodType(params1, UnitTpe)
                  case _ => tp
                }
              }
              impl.modifyInfo(cps)

              if (currentRun.compiles(sym))
                needTrees(decl) = impl // notify the tree transformer

              newDecls.enter(decl)
              newDecls.enter(impl)
              println(decl.name)
            } else {
              newDecls.enter(decl)
            }
          }
          if (changed) {
            GenPolyType(tparams, ClassInfoType(parents, newDecls, sym))
          } else tpe
        case GenPolyType(tparams, mt) if sym.annotations.exists(_.symbol.name.string_==("split")) =>
          mt.cloneInfo(sym)
        case _ =>
          tpe
      }
    }

    class MethodSplitterTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {
      override def transform(tree: Tree): Tree = tree match {
        case tmpl@Template(parents, self, body) =>
          afterOwnPhase(currentOwner.info)

          val newBody = ListBuffer[Tree]()
          var changed = false
          for (tree <- body) {
            newBody += tree
            tree match {
              case _: DefTree =>
                needTrees.get(tree.symbol) match {
                  case Some(impl) =>
                    changed = true
                    newBody += localTyper.typedPos(impl.pos)(DefDef(impl, EmptyTree))
                  case None =>
                }
              case _ =>
            }
          }
          println((changed, newBody))
          treeCopy.Template(tree, parents, self, transformTrees(if (changed) newBody.result() else body))
        case _ =>
          super.transform(tree)
      }
    }

  }

}
