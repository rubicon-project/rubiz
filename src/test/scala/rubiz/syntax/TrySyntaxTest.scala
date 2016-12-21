package rubiz.syntax

import scala.util.Try
import all._

class TrySyntaxTest extends rubiz.WordSpecBase {
  "Try.toDisjunction" should {
    "create left for Failure" in {
      Try(throw new Exception()).toDisjunction shouldBe 'left
    }
    "create right for values" in {
      Try(1).toDisjunction.value shouldBe 1
    }
  }

  "Try.toTask" should {
    "create Task.fail for Failure" in {
      Try(throw new Exception()).toTask.attemptRun shouldBe 'left
    }
    "create Task.now for values" in {
      Try(1).toTask.attemptRun.value shouldBe 1
    }
  }
}
