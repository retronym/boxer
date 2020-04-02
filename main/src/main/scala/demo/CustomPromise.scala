package demo

final class CustomPromise[T](wrapped: scala.concurrent.Promise[T]) {
  def _complete(either: Either[Throwable, T]): Unit = {
    wrapped.complete(either.toTry)
  }
  def _future: CustomFuture[T] = new CustomFuture[T](wrapped.future)
}
