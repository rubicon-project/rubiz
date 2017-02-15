package rubiz.syntax

import scalaz.effect.IO, scalaz.Scalaz._
import scalaz.concurrent.Task
import all._

class CatchableSyntaxTest extends rubiz.WordSpecBase {
  "Catchable.ensure" should {
    "fail if f is false" in {
      IO("foo").ensure(new Exception("dead"))(_ == "bar").attempt.unsafePerformIO shouldBe 'left
    }
    "succeed if f is true" in {
      IO(true).ensure(new Exception("dead"))(identity).attempt.unsafePerformIO shouldBe 'right
    }
  }

  "attemptSome" should {
    "do nothing on success" in {
      IO(3).attemptSome {
        case _ => 4
      }.unsafePerformIO.value shouldBe 3
    }

    "catch matching throwables" in {
      IO(throw new IllegalArgumentException).attemptSome {
        case ie: IllegalArgumentException => 42
      }.unsafePerformIO.leftValue shouldBe 42
    }

    "ignore non-matching throwables" in {
      an[IllegalArgumentException] should be thrownBy {
        IO(throw new IllegalArgumentException).attemptSome {
          case ise: IllegalStateException => 42
        }.unsafePerformIO
      }
    }

  }

  "except" should {
    "do nothing on success" in {
      IO(3).except(t => IO(4)).unsafePerformIO shouldEqual 3
    }

    "catch all exceptions" in {
      IO[Int](Predef.???).except(t => IO(4)).unsafePerformIO shouldEqual 4
    }

  }

  "exceptSome" should {
    "do nothing on success" in {
      IO(3).exceptSome {
        case _ => IO(4)
      }.unsafePerformIO shouldBe 3
    }

    "catch matching throwables" in {
      IO[Int](throw new IllegalArgumentException).exceptSome {
        case ie: IllegalArgumentException => IO(42)
      }.unsafePerformIO shouldBe 42
    }

    "ignore non-matching throwables" in {
      an[IllegalArgumentException] should be thrownBy {
        IO[Int](throw new IllegalArgumentException).exceptSome {
          case ise: IllegalStateException => IO(42)
        }.unsafePerformIO
      }
    }

  }

  "onException" should {
    "do nothing on success" in {
      var a = 1
      Task.now(42).onException(Task.delay(a += 1)).run
      a shouldBe 1
    }

    "perform its effect on exception" in {
      var a = 1
      try {
        Task.delay[Int](Predef.???).onException(Task.delay(a += 1)).run
        false
      } catch {
        case _: Throwable => a == 2
      }
    }

  }

  "ensuring" should {
    "perform its effect on success" in {
      var a = 1
      Task.delay(42).ensuring(Task.delay(a += 1)).run
      a shouldBe 2
    }

    "perform its effect on exception" in {
      var a = 1
      try {
        Task.delay[Int](Predef.???).ensuring(Task.delay(a += 1)).run
        false
      } catch {
        case _: Throwable => a == 2
      }
    }

  }

}
