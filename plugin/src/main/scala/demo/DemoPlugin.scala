package demo

import scala.reflect.internal.Flags
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.transform.{Transform, TypingTransformers}

class DemoPlugin(val global: Global) extends Plugin {

  val name = "demo-plugin"
  val description = "Trait field injection"
  val components = List[PluginComponent](DemoComponent)
  import global._

  global.analyzer.addMacroPlugin(macroPlugin)
  global.analyzer.addAnalyzerPlugin(macroPlugin)

  private val EntityAnnotName = TypeName("entity")
  private val NodeAnnotName = TypeName("node")
  private lazy val TypeInfoClass = rootMirror.staticClass("demo.TypeInfo")

  case class EntityClassDefAttachment(caseClass: ClassDef)

  object PropInfo

  object macroPlugin extends analyzer.MacroPlugin with analyzer.AnalyzerPlugin {
    override def isActive(): Boolean = phase.id <= currentRun.typerPhase.id
    override def pluginsEnterSym(namer: analyzer.Namer, tree: Tree): Boolean = {
      tree match {
        case cd: ClassDef =>
          val ctx = namer.standardEnterSym(tree)
          if (cd.mods.hasAnnotationNamed(EntityAnnotName)) {
            assert(!cd.mods.isCase, cd)
            val m = namer.ensureCompanionObject(cd, global.analyzer.companionModuleDef(_))
            m.moduleClass.updateAttachment(new EntityClassDefAttachment(cd))
          }
          true
        case _ => false
      }
    }
    override def pluginsTypeSig(tpe: Type, typer: analyzer.Typer, defTree: Tree, pt: Type): Type = defTree match {
      case tmpl: Template =>
        val templateNamer = typer.namer
        val clazz = typer.context.owner
        if (clazz.isModuleClass) {
          clazz.attachments.get[EntityClassDefAttachment] foreach { cma =>
            val cdef = cma.caseClass
            if (cdef.mods.hasAnnotationNamed(EntityAnnotName)) {
              // TODO Add apply factory

              for (member <- cdef.impl.body) {
                member match {
                  case vdd: ValOrDefDef =>
                    if (vdd.mods.hasAnnotationNamed(NodeAnnotName)) {
                      val propSym = templateNamer.context.owner.newMethod(vdd.name, vdd.pos.focus, Flags.SYNTHETIC)
                      // Lazily compute the result type to allow for cycles between the entity class
                      // and an user-written companion
                      propSym setInfo propTypeCompleter(vdd)
                      templateNamer.enterInScope(propSym)
                      templateNamer.context.unit.synthetics(propSym) = atPos(vdd.pos.focus)(DefDef(propSym, EmptyTree))
                      
                      // We'll fill in the body of the method after typer
                      propSym.updateAttachment(PropInfo)
                    }
                }
              }
            }
          }
        }
        tpe
      case _ =>
        tpe
    }
  }

  def propTypeCompleter(nodeDef: ValOrDefDef): analyzer.TypeCompleter = new PropTypeCompleter(nodeDef)

  class PropTypeCompleter(nodeDef: ValOrDefDef) extends analyzer.TypeCompleterBase[Tree](nodeDef) {
    override def completeImpl(propSym: Symbol): Unit = {
      val underlying = propSym.owner.companionClass.info.decl(propSym.name)

      def toFunction(tpe: Type): Type = tpe match {
        case NullaryMethodType(res) => toFunction(res)
        case MethodType(params, res) => definitions.functionType(params.map(_.info), toFunction(res))
        case tp => tp // TODO error on PolyType, dependent method types.
      }

      propSym.setInfo(NullaryMethodType(appliedType(TypeInfoClass, toFunction(underlying.info))))
    }
  }

  private object DemoComponent extends PluginComponent with Transform with TypingTransformers {
    val global: DemoPlugin.this.global.type = DemoPlugin.this.global

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

              val getterSym = cls.newMethodSymbol(TermName("foo$impl"), cls.pos.focus, newFlags = Flags.ACCESSOR)
              getterSym.setInfoAndEnter(NullaryMethodType(definitions.IntTpe))
              val getter = localTyper.typedPos(getterSym.pos.focus)(ValDef(getterSym, Literal(Constant(-42))))

              val setterSym = cls.newMethodSymbol(getterSym.name.setterName, cls.pos.focus, newFlags = Flags.ACCESSOR)
              val setterParams = setterSym.newSyntheticValueParams(definitions.IntTpe :: Nil)
              setterSym.setInfoAndEnter(MethodType(setterParams, definitions.UnitTpe))
              val setter = localTyper.typedPos(setterSym.pos.focus)(DefDef(setterSym, EmptyTree))

              super.transform(treeCopy.Template(templ, templ.parents, templ.self, templ.body ::: (getter :: setter :: Nil)))
            } else {
              super.transform(templ)
            }
          case dd: DefDef if dd.symbol.hasAttachment[PropInfo.type] =>
            val derived = deriveDefDef(dd) { _ =>
              val propInfoType = dd.symbol.info.finalResultType.typeArgs.head
              val cls = scala.reflect.reify.reifyRuntimeClass(global)(localTyper, propInfoType, true)
              localTyper.typedPos(dd.pos.focus)(New(TypeInfoClass, Literal(Constant(propInfoType.toString)), cls))
            }
            derived
          case _ => super.transform(tree)
        }
      }
    }

  }

}
