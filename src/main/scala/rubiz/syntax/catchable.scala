package rubiz
package syntax

import scalaz.{ Catchable, Monad }
import scalaz.syntax.monad._
trait CatchableSyntax {
  implicit final def catchableOps[M[_]: Catchable, A](ma: M[A]): CatchableOps[M, A] = new CatchableOps[M, A](ma)
}

final class CatchableOps[M[_], A](val ma: M[A])(implicit catchableM: Catchable[M]) {
  /**
   * Fail M with the error if `f` returns false. Return ma unchanged otherwise.
   */
  def ensure(error: => Throwable)(f: A => Boolean)(implicit monadM: Monad[M]): M[A] = ma.flatMap { a =>
    if (f(a)) ma else catchableM.fail(error)
  }
}
