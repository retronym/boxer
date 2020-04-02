package demo

import scala.annotation.StaticAnnotation

// Calls to methods with this annotation are translated to `Async.await(Future.successful(<call>))`
// This lets us express and test async boundaries in extractor calls, which one can't do with the async/await macro.
final class autoawait extends StaticAnnotation
