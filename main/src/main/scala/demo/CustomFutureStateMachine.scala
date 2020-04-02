package demo

abstract class CustomFutureStateMachine extends AsyncStateMachine[CustomFuture[AnyRef], scala.util.Either[Throwable, AnyRef]] with Function1[scala.util.Either[Throwable, AnyRef], Unit] {
  private val result$async: CustomPromise[AnyRef] = new CustomPromise[AnyRef](scala.concurrent.Promise.apply[AnyRef]);
  protected var state$async: Int = 0
  def apply(tr$async: R[AnyRef]): Unit

  type F[A] = CustomFuture[A]
  type R[A] = Either[Throwable, A]
  // Adapter methods
  protected def completeFailure(t: Throwable): Unit = result$async._complete(Left(t))
  protected def completeSuccess(value: AnyRef): Unit = result$async._complete(Right(value))
  protected def onComplete(f: F[AnyRef]): Unit = f._onComplete(this)
  protected def getCompleted(f: F[AnyRef]): R[AnyRef] = f._getCompleted
  protected def tryGet(tr: R[AnyRef]): AnyRef = tr match {
    case Right(value) =>
      value
    case Left(throwable) =>
      result$async._complete(tr)
      this // sentinel value to indicate the dispatch loop should exit.
  }
  def start(): CustomFuture[AnyRef] = {
    CustomFuture._unit.asInstanceOf[CustomFuture[AnyRef]]._onComplete(this)
    result$async._future
  }
}
