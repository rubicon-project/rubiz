package rubiz.syntax

import scalaz.effect.IO, scalaz.Scalaz._
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
}
