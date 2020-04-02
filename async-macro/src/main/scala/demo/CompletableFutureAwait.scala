package demo

import java.util.Objects
import java.util.concurrent.{CompletableFuture, Executor}
import java.util.function.BiConsumer

import scala.language.experimental.macros
import scala.annotation.compileTimeOnly
import scala.reflect.macros.blackbox
import scala.util.{Failure, Success, Try}

object CompletableFutureAwait {
  def async[T](executor: Executor)(body: T): CompletableFuture[T] = macro impl
  @compileTimeOnly("[async] `await` must be enclosed in `async`")
  def await[T](completableFuture: CompletableFuture[T]): T = ???
  def impl(c: blackbox.Context)(executor: c.Tree)(body: c.Tree): c.Tree = {
    import c.universe._
    val awaitSym = typeOf[CompletableFutureAwait.type].decl(TermName("await"))
    def mark(t: DefDef): Tree = c.internal.markForAsyncTransform(c.internal.enclosingOwner, t, awaitSym, Map.empty)
    val name = TypeName("stateMachine$async")
    q"""
      final class $name extends _root_.demo.CompletableFutureStateMachine($executor) {
        ${mark(q"""override def apply(tr$$async: _root_.scala.util.Try[_root_.scala.AnyRef]) = ${body}""")}
      }
      new $name().start().asInstanceOf[${c.macroApplication.tpe}]
    """
  }
}

abstract class CompletableFutureStateMachine(_executor: Executor) extends demo.AsyncStateMachine[CompletableFuture[AnyRef], Try[AnyRef]] with Runnable with BiConsumer[AnyRef, Throwable] {
  Objects.requireNonNull(_executor)

  private[this] var _result: CompletableFuture[AnyRef] = new CompletableFuture[AnyRef]();
  private[this] var _state: Int = 0

  // Adapters
  def accept(value: AnyRef, throwable: Throwable): Unit = {
    this(if (throwable != null) Failure(throwable) else Success(value))
  }
  def run(): Unit = {
    apply(null)
  }

  // FSM translated method
  def apply(tr$async: Try[AnyRef]): Unit

  // Required methods
  override protected def state$async: Int = _state
  override protected def state$async_=(i: Int): Unit = _state = i

  protected def completeFailure(t: Throwable): Unit = _result.completeExceptionally(t)
  protected def completeSuccess(value: AnyRef): Unit = _result.complete(value)
  protected def onComplete(f: CompletableFuture[AnyRef]): Unit = f.whenCompleteAsync(this)
  protected def getCompleted(f: CompletableFuture[AnyRef]): Try[AnyRef] = try {
    val r = f.getNow(this)
    if (r == this) null
    else Success(r)
  } catch {
    case t: Throwable => Failure(t)
  }
  protected def tryGet(tr: Try[AnyRef]): AnyRef = tr match {
    case Success(value) =>
      value.asInstanceOf[AnyRef]
    case Failure(throwable) =>
      _result.completeExceptionally(throwable)
      this // sentinel value to indicate the dispatch loop should exit.
  }
  def start(): CompletableFuture[AnyRef] = {
    _executor.execute(this)
    _result
  }
}
