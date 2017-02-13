package rubiz.syntax

import scalaz.concurrent.Task
import rubiz.CanCloseTest.Foo
import scala.concurrent.duration._

import all._

class TaskSyntaxTest extends rubiz.WordSpecBase {
  "Task.withSideEffectTiming" should {
    "use the timer in the happy path" in {
      val expected = "expected"
      var calledTimes = 0
      val task = Task.delay(expected).withSideEffectTiming(_ => calledTimes = calledTimes + 1)
      task.attemptRun.value shouldBe expected
      calledTimes shouldBe 1
      task.attemptRun.value shouldBe expected
      calledTimes shouldBe 2
    }
    "use the timer when an exception is thrown" in {
      var calledTimes = 0
      val task = Task.delay(throw new Exception).withSideEffectTiming(_ => calledTimes = calledTimes + 1)
      task.attemptRun shouldBe left
      calledTimes shouldBe 1
      task.attemptRun shouldBe left
      calledTimes shouldBe 2
    }
  }

  "Task.withTiming" should {
    "use the timer in the happy path" in {
      val expected = "expected"
      val task = Task.delay(expected).withTiming
      val (time, _) = task.attemptRun.value
      (time.toNanos > 0) shouldBe true
    }
  }
  "Task.failMap" should {
    "ignore a successful task" in {
      val expected = 1
      Task.now(expected).failMap(_ => fail("shouldn't get here")).attemptRun.value shouldBe expected
    }
    "reflect the changed exception" in {
      val original = new Exception("original")
      val better = new Exception("extra crispy")
      Task.fail(original).failMap(_ => better).attemptRun.leftValue shouldBe better
    }
  }
  "Task.peek" should {
    "be called for a success" in {
      var called = false
      val expected = 1
      Task.now(expected).peek { a =>
        called = true
        a shouldBe expected
      }.attemptRun.value shouldBe expected
      called shouldBe true
    }
    "be ignored for a failure" in {
      var called = false
      val expected = new Exception("fail")
      Task.fail(expected).peek { _: Exception =>
        called = true
      }.attemptRun.leftValue shouldBe expected
      called shouldBe false
    }
  }
  "Task.peekFail" should {
    "be ignored for a success" in {
      var called = false
      val expected = 1
      Task.now(expected).peekFail { a =>
        called = true
      }.attemptRun.value shouldBe expected
      called shouldBe false
    }
    "be called for a failure" in {
      var called = false
      val expected = new Exception("fail")
      Task.fail(expected).peekFail { ex =>
        ex shouldBe expected
        called = true
      }.attemptRun.leftValue shouldBe expected
      called shouldBe true
    }
  }

  "Task.attemptFold" should {
    "convert error to Int" in {
      //Make the 2nd function throw because it shouldn't be evaluated.
      val failedTask: Task[String] = Task.fail(new Exception(""))
      val resultTask = failedTask.attemptFold(_ => 1)(x => throw new Exception(x))
      resultTask.attemptRun.value shouldBe 1
    }
    "convert success to Int" in {
      //Make the 1st function throw because it shouldn't be evaluated.
      val successfulTask: Task[String] = Task.delay("foo")
      val resultTask = successfulTask.attemptFold(ex => throw ex)(_ => 1)
      resultTask.attemptRun.value shouldBe 1
    }
  }
  "Task.labeledTimeout" should {
    "allow a normal task to execute unhindered" in {
      Task.now("expected").labeledTimeout(9.days, "This shouldn't happen").run shouldBe "expected"
    }
    "report a nice message for a slow task which hits the limit" in {
      val slowTask = Task.delay {
        Thread.sleep(50.millis.toMillis)
        "unexpected"
      }
      val task = slowTask.labeledTimeout(2.millis, "the slow task")
      task.attemptRun.leftValue.getMessage should include regex "'the slow task'.* 2 milliseconds"
    }
  }
  "Task.using" should {
    "close the item if the Task succeeds" in {
      val foo = new Foo
      Task.delay(foo).using { _ =>
        Task.now(1)
      }.attemptRun.value shouldBe 1
      foo.isClosed shouldBe true
    }
    "close the item if the Task fails explicitly" in {
      val foo = new Foo
      Task.delay(foo).using { _ =>
        Task.fail(new IllegalArgumentException)
      }.attemptRun shouldBe left
      foo.isClosed shouldBe true
    }
    "close the item if the Task fails due to a thrown exception" in {
      val foo = new Foo
      Task.delay(foo).using { _ =>
        throw new InternalError
      }.attemptRun shouldBe left
      foo.isClosed shouldBe true
    }
  }
}
