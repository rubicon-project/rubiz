package rubiz

import java.util.concurrent.{ Executors }

import scalaz.\/
import scalaz.concurrent.Task
import org.typelevel.scalatest.{ DisjunctionMatchers, DisjunctionValues }
import CanCloseTest.Foo

class CanCloseTest extends rubiz.WordSpecBase with DisjunctionValues with DisjunctionMatchers {

  "CanClose.using" should {
    val f: Int => Int = _ + 1
    "close single value" in {
      val foo = new Foo()
      val result = CanClose.using(foo) { _ => 1 }
      result.attemptRun.value shouldBe 1 //Just to prove we made it here
      foo.isClosed shouldBe true
    }

    "close single value for ExecutorService" in {
      val foo = Executors.newSingleThreadExecutor()
      val result = CanClose.using(foo) { _ => 1 }
      result.attemptRun.value shouldBe 1 //Just to prove we made it here
      foo.isShutdown shouldBe true
    }

    "close even if exception happens" in {
      val foo = new Foo()
      an[Exception] should be thrownBy {
        val task = CanClose.using(foo) { _ => throw new Exception() }
        task.run //This will throw the exception
      }
      foo.isClosed shouldBe true
    }
  }

  "CanClose.computeWithClose" when {
    "throw on acquire" should {
      "Result in exception and not close" in {
        val (closed, maybeValue) = computeWithClose(Task.delay(throw new Exception("BOOM!")))
        maybeValue should be(left)
        closed shouldBe false
      }
    }

    "throw on compute step" should {
      "Catch the non fatal exception and close" in {
        val (closed, maybeValue) = computeWithClose(stepFunc = _ => Task.delay(throw new Exception("Headshot!")))
        maybeValue should be(left)
        closed shouldBe true
      }
      "Catch the fatal exception and close" in {
        val (closed, maybeValue) = computeWithClose(stepFunc = _ => Task.delay(throw new VirtualMachineError {}))
        maybeValue should be(left)
        closed shouldBe true
      }
    }

    "throw uncaught exception outside of task on compute step" in {
      val (closed, maybeValue) = computeWithClose(stepFunc = _ => throw new VirtualMachineError {})
      maybeValue should be(left)
      closed shouldBe true
    }

    "successful compute" should {
      "Result in value and close" in {
        val (closed, maybeValue) = computeWithClose(stepFunc = _ => Task.delay(24L))
        maybeValue.value shouldBe 24L
        closed shouldBe true
      }
    }
  }

  def computeWithClose(acquire: Task[Int] = Task.delay(1), stepFunc: Int => Task[Long] = { i: Int => Task.delay(i.toLong) }): (Boolean, Throwable \/ Long) = {
    var closed = false
    val maybeValue = CanClose.computeWithClose(acquire)(stepFunc)(CanClose.createFromTask { _ => Task.delay(closed = true) }).attemptRun
    (closed, maybeValue)
  }
}

object CanCloseTest {
  class Foo extends java.io.Closeable {
    var isClosed = false
    def close: Unit = isClosed = true
  }
}
