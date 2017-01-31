package rubiz
package syntax

import scalaz.{ Catchable, Monad, \/ }
import scalaz.syntax.monad._

trait CatchableSyntax {
  implicit final def catchableOps[M[_]: Catchable, A](ma: M[A]): CatchableOps[M, A] = new CatchableOps[M, A](ma)
}

final class CatchableOps[M[_], A](val ma: M[A])(implicit catchableM: Catchable[M]) {
  /**
   * Fail M with the error if `f` returns false, otherwise return ma unchanged.
   */
  def ensure(error: => Throwable)(f: A => Boolean)(implicit monadM: Monad[M]): M[A] = ma.flatMap { a =>
    if (f(a)) ma else catchableM.fail(error)
  }

  /**
   * Like `attempt` but catches (and maps) only where defined.
   */
  def attemptSome[B](p: PartialFunction[Throwable, B])(implicit monadM: Monad[M]): M[B \/ A] =
    catchableM.attempt(ma).map(_.leftMap(e => p.lift(e).getOrElse(throw e)))

  /**
   * Executes the handler, for exceptions propagating from `ma`.
   */
  def except(handler: Throwable => M[A])(implicit monadM: Monad[M]): M[A] =
    catchableM.attempt(ma).flatMap(_.bimap(handler, _.pure[M]).merge)

  /**
   * Executes the handler where defined, for exceptions propagating from `ma`.
   */
  def exceptSome(pf: PartialFunction[Throwable, M[A]])(implicit monadM: Monad[M]): M[A] =
    except(e => pf.lift(e).getOrElse((throw e): M[A]))

  /**
   * Like "finally", but only performs the final action if there was an exception.
   */
  def onException[B](action: M[B])(implicit monadM: Monad[M]): M[A] =
    except(e => action *> catchableM.fail(e))

  /**
   * Always execute `sequel` following `ma`; generalizes `finally`.
   */
  def ensuring[B](sequel: M[B])(implicit monadM: Monad[M]): M[A] =
    onException(sequel) <* sequel
}
