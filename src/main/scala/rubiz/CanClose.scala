package rubiz

import scalaz.concurrent.Task

/**
 * Typeclass for things that can be closed; best paired with `Task.using` syntax.
 */
trait CanClose[-A] {
  /**
   * Close the thing of type A. Note this is done in a Task so exceptions are
   * caught and you can deal with them in a composable way.
   */
  def close(a: A): Task[Unit]

}

final object CanClose {

  def apply[A](implicit cc: CanClose[A]): CanClose[A] = cc

  /**
   * Create a CanClose for A given its close function
   */
  def create[A](f: A => Unit): CanClose[A] = new CanClose[A] {
    def close(a: A): Task[Unit] = Task.delay(f(a))
  }

  /**
   * Create a CanClose for A given its close function which is already in Task.
   */
  def createFromTask[A](f: A => Task[Unit]): CanClose[A] = new CanClose[A] {
    def close(a: A): Task[Unit] = f(a)
  }

  /**
   * Given some by name A, and a function to run on that A produce a Task which will ensure the A is closed even
   * if any piece throws an exception.
   * @param a - A resource to aquire (in an exception catching context)
   * @param f - Function to run on the resource which will produce some B
   */
  def using[A, B](a: => A)(f: A => B)(implicit cc: CanClose[A]): Task[B] =
    computeWithClose(Task.delay(a)) { aa => Task.delay(f(aa)) }

  /**
   * Given a way to acquire a resource and a function to run on that resource, produce a Task which will do that
   * each time it's invoked, ensuring the resource's close will always be called.
   * @param acquire - A Task which acquires a resource of type A
   * @param compute - A function from the resource to some Task[B]
   */
  def computeWithClose[A, B](acquire: Task[A])(compute: A => Task[B])(implicit cc: CanClose[A]): Task[B] = {
    acquire.flatMap(a => Task.now(a).flatMap(compute).onFinish(_ => cc.close(a)))
  }

  implicit final val CloseableInstance: CanClose[java.io.Closeable] = create(_.close)
  implicit final val ExecutorServiceInstance: CanClose[java.util.concurrent.ExecutorService] = create(_.shutdown)
}
