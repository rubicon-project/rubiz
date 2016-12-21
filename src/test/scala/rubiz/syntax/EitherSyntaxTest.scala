package rubiz.syntax

import all._
import scalaz.syntax.either._
import scalaz.\/
import scalaz.effect.IO

class EitherSyntaxTest extends rubiz.WordSpecBase {
  val right = "foo".right[Throwable]
  val left = \/.fromTryCatchNonFatal[String](throw new Exception("broken"))
  "EitherSyntax.toM" should {
    "put exception into fail" in {
      left.toM[IO].catchLeft.unsafePerformIO.leftValue shouldBe an[Exception]
    }
    "put a into M" in {
      right.toM[IO].catchLeft.unsafePerformIO.value shouldBe "foo"
    }
  }
  "EitherSyntax.toTask" should {
    "convert exception to fail" in {
      left.toTask.attemptRun shouldBe 'left
    }
    "convert right to fail" in {
      right.toTask.attemptRun shouldBe 'right
    }
  }
}
