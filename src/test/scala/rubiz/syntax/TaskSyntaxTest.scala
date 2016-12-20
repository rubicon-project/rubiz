package rubiz.syntax

import scalaz.concurrent.Task
import all._

class TaskSyntaxTest extends rubiz.WordSpecBase {
  "Task.withSideEffectTiming" should {
    "use the timer in the happy path" in {
      val expected = "expected"
      var calledTimes = 0
      val task = Task.delay(expected).withSideEffectTiming(_ => calledTimes = calledTimes + 1)
      task.run shouldBe expected
      calledTimes shouldBe 1
      task.run shouldBe expected
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
  "Task.failMap" should {
    "ignore a successful task" in {
      val expected = 1
      Task.now(expected).failMap(_ => fail("shouldn't get here")).run shouldBe expected
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
      }.run shouldBe expected
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
      }.run shouldBe expected
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
}
