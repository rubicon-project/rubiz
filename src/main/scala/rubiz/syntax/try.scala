package rubiz
package syntax

import scalaz.{ -\/, \/, \/- }
import scalaz.concurrent.Task
import scala.util.{ Try, Success, Failure }
import either._

trait TrySyntax {
  implicit final def tryOps[A](t: Try[A]): TryOps[A] = new TryOps[A](t)
}

final class TryOps[A](val t: Try[A]) extends AnyVal {
  def toDisjunction: Throwable \/ A = t match {
    case Failure(ex) => -\/(ex)
    case Success(a) => \/-(a)
  }

  def toTask: Task[A] = toDisjunction.toTask
}
