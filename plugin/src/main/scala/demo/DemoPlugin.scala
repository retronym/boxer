package demo

import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.transform.TypingTransformers

final class AnnotationDrivenAsyncPlugin(val global: Global) extends Plugin {
  import global._

  override val components: List[PluginComponent] = List(new PluginComponent with TypingTransformers {
    val global: AnnotationDrivenAsyncPlugin.this.global.type = AnnotationDrivenAsyncPlugin.this.global

    lazy val asyncModuleSym = rootMirror.getClassIfDefined("demo.CustomFuture")
    lazy val awaitSym = rootMirror.getModuleIfDefined("demo.CustomFuture").info.member(TermName("_await"))
    lazy val autoAwaitSym = rootMirror.getClassIfDefined("demo.autoAwait")
    lazy val customAsyncSym = rootMirror.getClassIfDefined("demo.customAsync")
    lazy val CustomFuture_class = rootMirror.getClassIfDefined("demo.CustomFuture")
    lazy val CustomFuture_successful = CustomFuture_class.companionModule.info.member(TermName("_successful"))

    def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
      override def transform(tree: Tree): Tree = {
        def wrapAwait = {
          localTyper.typedPos(tree.pos) {
            Apply(TypeApply(gen.mkAttributedRef(asyncModuleSym.typeOfThis, awaitSym), TypeTree(tree.tpe) :: Nil), gen.mkMethodCall(CustomFuture_successful, tree :: Nil) :: Nil)
          }
        }
        super.transform(tree) match {
          case ap@Apply(fun, _) if fun.symbol.hasAnnotation(autoAwaitSym) =>
            wrapAwait
          case sel@Select(_, _) if sel.symbol.hasAnnotation(autoAwaitSym) && !(tree.tpe.isInstanceOf[MethodType] || tree.tpe.isInstanceOf[PolyType]) =>
            wrapAwait
          case dd: DefDef if dd.symbol.hasAnnotation(customAsyncSym) =>
            deriveDefDef(dd) {
              rhs =>
                val applyMethod =
                  q"""def apply(tr: _root_.scala.util.Either[_root_.scala.Throwable, _root_.scala.AnyRef]): _root_.scala.Unit = $rhs"""
                val applyMethodMarked = global.async.markForAsyncTransform(dd.symbol, applyMethod, awaitSym, Map.empty)
                val name = TypeName("fsm") //tpnme.stateMachine
                val wrapped =
                  q"""
                    class $name extends _root_.demo.CustomFutureStateMachine {
                     $applyMethodMarked
                    }
                    new $name().start()
                   """

                val tree =
                  q"""
                      val temp = ${wrapped}
                     temp._block
                    """
                val result = atOwner(dd.symbol) {
                  localTyper.typedPos(dd.pos) {
                    tree
                  }
                }
                result
            }
          case x => x
        }
      }
    }

    override def newPhase(prev: Phase): Phase = new StdPhase(prev) {
      override def apply(unit: CompilationUnit): Unit = {
        newTransformer(unit).transformUnit(unit)
      }
    }

    override val runsAfter: List[String] = "refchecks" :: "patmat" :: Nil
    override val phaseName: String = "postpatmat"

  })
  override val description: String = "postpatmat"
  override val name: String = "postpatmat"
}
