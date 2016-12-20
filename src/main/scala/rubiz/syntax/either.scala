package rubiz
package syntax

import scalaz.{ \/, Catchable, Applicative }
import scalaz.concurrent.Task

trait EitherSyntax {
  implicit final def eitherOps[A, B](e: A \/ B): EitherOps[A, B] = new EitherOps[A, B](e)
}

final class EitherOps[A, B](val e: A \/ B) extends AnyVal {
  /**
   * Syntax for `Task.fromDisjunction`.
   */
  def toTask(implicit ev: A <:< Throwable): Task[B] = {
    Task.fromDisjunction(e.leftMap(ev))
  }

  /**
   * Assuming this is a `\/` with a Throwable on the Left we can convert this to an M of B as long as M has
   * both a Catchable and Applicative instance.
   */
  def toM[M[_]](implicit ev: A <:< Throwable, catchM: Catchable[M], apM: Applicative[M]): M[B] = {
    e.fold(a => catchM.fail(ev(a)), apM.point(_))
  }
}
