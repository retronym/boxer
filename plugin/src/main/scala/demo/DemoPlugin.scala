package demo

import scala.collection.mutable.ListBuffer
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.transform.{InfoTransform, TypingTransformers}

class DemoPlugin(val global: Global) extends Plugin {

  val name = "demo-plugin"
  val description = "CPS transform demo"
  val components = List[PluginComponent](CpsComponent)
  import global._
  import definitions._

  private val EntityAnnotName = TypeName("entity")
  private val NodeAnnotName = TypeName("node")
  private lazy val TypeInfoClass = rootMirror.staticClass("demo.TypeInfo")

  private object CpsComponent extends PluginComponent with InfoTransform with TypingTransformers {
    override val global: DemoPlugin.this.global.type = DemoPlugin.this.global
    override val phaseName: String = "demo-method-splitter"

    override val runsAfter: List[String] = List("typer")
    override val runsBefore: List[String] = List("refchecks") // to demonstrate absense of ill-bounded type applications ala https://github.com/scala/bug/issues/11383

    protected def newTransformer(unit: CompilationUnit): Transformer = new CpsTransformer(unit)
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
              val declInfo = decl.info

              // Clone all but the type of decl, we'll use the uncloned decl.info as the info of
              // impl to avoid the need to perform substitutions through the RHS.
              decl.setInfo(NoType)
              val impl = decl.cloneSymbol(decl.owner, decl.flags, decl.name.append("$impl"))

              // Give decl a clone of its former info
              decl.setInfo(declInfo.cloneInfo(decl))

              def cps(tp: Type): Type = {
                def contParam(restpe: Type) =
                  decl.newSyntheticValueParam(functionType(restpe :: Nil, UnitTpe), contName).setPos(decl.pos)
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
              class ChangeOwner(oldowner: Symbol, newowner: Symbol) extends TypeTraverser {
                final def change(sym: Symbol): Unit = {
                  if (sym != NoSymbol && sym.owner == oldowner) {
                    sym.owner = newowner
                    if (sym.isModule) sym.moduleClass.owner = newowner
                  }
                }
                override def traverse(tp: Type): Unit = {
                  tp match {
                    case PolyType(qs, res) => qs.foreach(change)
                    case MethodType(ps, _) => ps.foreach(change)
                    case ExistentialType(qs, _) => qs.foreach(change)
                    case _ =>
                  }
                  mapOver(tp)
                }
              }
              val changeOwner = new ChangeOwner(decl, impl)
              changeOwner(declInfo)
              impl.setInfo(cps(declInfo))

              if (currentRun.compiles(sym))
                needTrees(decl) = impl // notify the tree transformer

              newDecls.enter(decl)
              newDecls.enter(impl)
            } else {
              newDecls.enter(decl)
            }
          }
          if (changed) {
            GenPolyType(tparams, ClassInfoType(parents, newDecls, sym))
          } else tpe
        case _ =>
          tpe
      }
    }

    /** Adds CPS-transformed sibling methods
      * ```
      * @demo.split def foo(x: Int): Int = x.+(1);
      * @demo.split def foo$impl(x: Int, $cont: Int => Unit): Unit;
      * @demo.split def bar(x: Int)(y: Int): Int = x.+(y);
      * @demo.split def bar$impl(x: Int)(y: Int, $cont: Int => Unit): Unit;
      * @demo.split def baz: Nothing = scala.Predef.???;
      * @demo.split def baz$impl($cont: Nothing => Unit): Unit
      * ```
      */
    class CpsTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {
      override def transform(tree: Tree): Tree = tree match {
        case tmpl@Template(parents, self, body) =>
          afterOwnPhase(currentOwner.info)

          val newBody = ListBuffer[Tree]()
          var changed = false
          for (tree <- body) {
            tree match {
              case decl: DefDef =>
                needTrees.get(tree.symbol) match {
                  case Some(impl) =>
                    changed = true
                    val forwarderRhs = EmptyTree
                    newBody += localTyper.typedPos(decl.pos)(DefDef(decl.symbol, forwarderRhs))

                    // Look Ma, no substitution!
                    val implDefDef = deriveDefDef(decl)(_ => decl.rhs).changeOwner(decl.symbol -> impl).setSymbol(impl)
                    newBody += implDefDef
                  case None =>
                    newBody += tree
                }
              case _ =>
                newBody += tree
            }
          }
          treeCopy.Template(tree, parents, self, transformTrees(if (changed) newBody.result() else body))
        case _ =>
          super.transform(tree)
      }
    }

  }

}
