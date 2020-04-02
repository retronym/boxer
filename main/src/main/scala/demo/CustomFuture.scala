package demo

import scala.concurrent.duration.Duration

// Method names prefixed with `_` to better test flush out hard-coded names in the
// async implementation.
final class CustomFuture[T](val wrapped: scala.concurrent.Future[T]) {
  def _onComplete[U](f: Either[Throwable, T] => U): Unit = {
    wrapped.onComplete((tr => f(tr.toEither)))(scala.concurrent.ExecutionContext.Implicits.global)
  }
  def _isCompleted: Boolean = wrapped.isCompleted
  def _getCompleted: Either[Throwable, T] = if (wrapped.isCompleted) wrapped.value.get.toEither else null
  def _block: T = scala.concurrent.Await.result(wrapped, Duration.Inf)
}

object CustomFuture {
  def _apply[T](f: => T): CustomFuture[T] = new CustomFuture[T](scala.concurrent.Future(f)(scala.concurrent.ExecutionContext.Implicits.global))
  val _unit = new CustomFuture[Unit](scala.concurrent.Future.unit)
  def _successful[T](t: T): CustomFuture[T] = new CustomFuture[T](scala.concurrent.Future.successful(t))
  def _await[T](f: CustomFuture[T]): T = ???
}