package rubiz
package syntax

import scala.concurrent.duration.FiniteDuration
import scalaz.concurrent.Task
import scala.concurrent.duration._

trait TaskSyntax {
  implicit final def taskOps[A](t: Task[A]): TaskOps[A] = new TaskOps[A](t)
}

final class TaskOps[A](val t: Task[A]) extends AnyVal {
  private def currentNanoTime = Task.delay(System.nanoTime)

  /**
   * When `t` executes, time it and return how long it took and its result.
   * If `t` fails, timing will not be captured.
   */
  def withTiming: Task[(FiniteDuration, A)] = for {
    start <- currentNanoTime
    result <- t
    end <- currentNanoTime
  } yield ((end - start).nanos, result)

  /**
   * When `t` executes, pass the duration to `f`.
   * Timing is logged regardless of whether `t` succeeds.
   */
  def withSideEffectTiming(f: FiniteDuration => Unit): Task[A] = {
    currentNanoTime.flatMap { startTimeNanos =>
      t.onFinish { _ =>
        currentNanoTime.map { endTimeNanos =>
          f((endTimeNanos - startTimeNanos).nanos)
        }
      }
    }
  }

  /**
   * If `t` fails this allows you to report a different exception, no-op otherwise.
   */
  def failMap(f: Throwable => Throwable): Task[A] = {
    t.handleWith {
      case throwable => Task.fail(f(throwable))
    }
  }

  /**
   * When `t` evaluates, handle success and failure separately.
   */
  def attemptFold[B](errorToB: Throwable => B)(aToB: A => B): Task[B] = {
    t.attempt.map(_.fold(errorToB, aToB))
  }

  /**
   * An opportunity to side effect (log), when `t` evaluates to a successful value.
   */
  def peek(f: A => Unit): Task[A] = {
    t.map { a =>
      f(a)
      a
    }
  }

  /**
   * An opportunity to side effect (log), when `t` evaluates to a failure.
   */
  def peekFail(f: Throwable => Unit): Task[A] = {
    t.handleWith {
      case throwable =>
        f(throwable)
        t
    }
  }
}
