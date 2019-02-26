package demo

import scala.collection.mutable.ListBuffer
import scala.reflect.internal.Flags
import scala.tools.nsc.{Global, Mode}
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.transform.{InfoTransform, Transform, TypingTransformers}

class DemoPlugin(val global: Global) extends Plugin {

  val name = "demo-plugin"
  val description = "Trait field injection"
  val components = List[PluginComponent](DemoComponent, DemoPreNamerComponent, DemoMethodSplitterComponent)
  import global._

  global.analyzer.addMacroPlugin(macroPlugin)
  global.analyzer.addAnalyzerPlugin(macroPlugin)

  private val EntityAnnotName = TypeName("entity")
  private val NodeAnnotName = TypeName("node")
  private lazy val TypeInfoClass = rootMirror.staticClass("demo.TypeInfo")

  case class EntityClassDefAttachment(caseClass: ClassDef)

  object macroPlugin extends analyzer.MacroPlugin with analyzer.AnalyzerPlugin {
    override def isActive(): Boolean = phase.id <= currentRun.typerPhase.id
    override def pluginsEnterSym(namer: analyzer.Namer, tree: Tree): Boolean = {
      tree match {
        case cd: ClassDef =>
          val ctx = namer.standardEnterSym(tree)
          if (cd.mods.hasAnnotationNamed(EntityAnnotName)) {
            assert(!cd.mods.isCase, cd)
            def companionModuleDef1(cd: ClassDef) = {
              val mod = global.analyzer.companionModuleDef(cd)
              // workaround https://github.com/scala/scala/pull/7461
              treeCopy.ModuleDef(mod, new Modifiers(mod.mods.flags, tpnme.EMPTY, mod.mods.annotations), mod.name, mod.impl)
            }
            val m = namer.ensureCompanionObject(cd, cd => companionModuleDef1(cd))
            m.moduleClass.updateAttachment(new EntityClassDefAttachment(cd))
          }
          true
        case _ => false
      }
    }

    override def pluginsMacroExpand(typer: analyzer.Typer, expandee: Tree, mode: Mode, pt: Type): Option[Tree] = {
      if (expandee.symbol eq propInfoDummyMacro) {
        typer.context.enclMethod.owner.info
          val propInfoType = typer.context.owner.info.typeArgs.head
          val cls = scala.reflect.reify.reifyRuntimeClass(global)(typer, propInfoType, true)
          Some(typer.typedPos(expandee.pos.focus)(New(TypeInfoClass, Literal(Constant(propInfoType.toString)), cls)))
      } else {
        None
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
                      val propSym = templateNamer.context.owner.newValue(vdd.name, vdd.pos.focus, Flags.SYNTHETIC)
                      // Lazily compute the result type to allow for cycles between the entity class
                      // and an user-written companion
                      propSym setInfo propTypeCompleter(vdd, cma.caseClass.symbol)
                      templateNamer.enterInScope(propSym)
                      // TODO: could we change `synthetics` to accept a `Type => Tree` function, rather than a `Tree`,
                      // avoid the need to emit a macro call that we need to later intercept.
                      templateNamer.context.unit.synthetics(propSym) = newValDef(propSym, Ident(propInfoDummyMacro))(tpt = TypeTree())
                    }
                  case t => t
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

  private lazy val propInfoDummyMacro = NoSymbol.newMethod(TermName("propInfoMacro"), NoPosition, Flags.MACRO).setInfo(NullaryMethodType(definitions.AnyRefTpe))

  def propTypeCompleter(nodeDef: ValOrDefDef, companionClass: Symbol): analyzer.TypeCompleter = new PropTypeCompleter(nodeDef, companionClass)

  class PropTypeCompleter(nodeDef: ValOrDefDef, companionClass: Symbol) extends analyzer.TypeCompleterBase[Tree](nodeDef) {
    override def completeImpl(propSym: Symbol): Unit = {
      val underlying = companionClass.info.decl(propSym.name)

      def toFunction(tpe: Type): Type = tpe match {
        case NullaryMethodType(res) => toFunction(res)
        case MethodType(params, res) => definitions.functionType(params.map(_.info), toFunction(res))
        case tp => tp // TODO error on PolyType, dependent method types.
      }

      propSym.setInfo(appliedType(TypeInfoClass, toFunction(underlying.info)))
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
            if (cls.name.string_==("T") && cls.isTrait) {

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
          case _ => super.transform(tree)
        }
      }
    }
  }

  private object DemoPreNamerComponent extends PluginComponent with Transform {
    override val global: DemoPlugin.this.global.type = DemoPlugin.this.global
    override val phaseName: String = "demo-pre-namer"
    override val runsAfter: List[String] = List("parser")

    protected def newTransformer(unit: CompilationUnit): Transformer = new PreNamerTransformer()

    class PreNamerTransformer extends Transformer {
      def addCompanions(stats: List[Tree]): List[Tree] = {
        val modules: Set[TermName] = stats.collect { case ModuleDef(_, name, _) => name.toTermName }.toSet
        val stats1 = stats.flatMap {
          case cdef: ClassDef if !cdef.mods.isCase && !modules.contains(cdef.name.toTermName) =>
            List(cdef, analyzer.companionModuleDef(cdef, Nil, Nil))
          case t => List(t)
        }
        stats1
      }
      override def transform(tree: Tree): Tree = tree match {
        case pd: PackageDef =>
          treeCopy.PackageDef(pd, pd.pid, addCompanions(transformTrees(pd.stats)))
        case Block(stats, expr) =>
          treeCopy.Block(tree, addCompanions(transformTrees(stats)), transform(expr))
        case Template(parents, self, body) =>
          treeCopy.Template(tree, parents, self, addCompanions(transformTrees(body)))
        case _ => super.transform(tree)
      }
    }
  }

  private object DemoMethodSplitterComponent extends PluginComponent with InfoTransform with TypingTransformers {
    override val global: DemoPlugin.this.global.type = DemoPlugin.this.global
    override val phaseName: String = "demo-method-splitter"
    override val runsAfter: List[String] = List("refchecks")

    protected def newTransformer(unit: CompilationUnit): Transformer = new MethodSplitterTransformer(unit)
    private val needTrees = collection.mutable.AnyRefMap[Symbol, Symbol]()

    override def transformInfo(sym: Symbol, tpe: Type): Type = {
      tpe match {
        case GenPolyType(tparams, ClassInfoType(parents, decls, sym)) =>
          val newDecls = newScope
          var changed = false
          for (decl <- tpe.decls) {
            if (decl.isMethod && decl.annotations.exists(_.symbol.name.string_==("node"))) {
              changed = true
              val forwarder = decl.cloneSymbol
              needTrees(decl) = forwarder
              decl.setName(decl.name.append("$split"))
              newDecls.enter(forwarder)
              newDecls.enter(decl)
              println(decl.name)
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

    class MethodSplitterTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {
      override def transform(tree: Tree): Tree = tree match {
        case tmpl @ Template(parents, self, body) =>
          afterOwnPhase(currentOwner.info)

          val newBody = ListBuffer[Tree]()
          var changed = false
          for (tree <- body) {

              newBody += tree
            tree match {
              case _: DefTree =>
                needTrees.remove(tree.symbol) match {
                  case Some(forwarder) =>
                    val target = tree.symbol
                    changed = true
                    val callPrefix = gen.mkAttributedSelect(This(currentOwner), target)
                    val rhs = gen.mkForwarder(callPrefix, forwarder.paramss)
                    newBody += localTyper.typedPos(forwarder.pos)(DefDef(forwarder, rhs))
                  case None =>
                }
              case _ =>
            }
          }
          treeCopy.Template(tree, parents, self, transformTrees(if (changed) newBody.result() else body))
        case _ =>
          super.transform(tree)
      }
    }
  }
}
