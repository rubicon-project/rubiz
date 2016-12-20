package rubiz.syntax

import scala.util.Try
import all._

class TrySyntaxTest extends rubiz.WordSpecBase {
  "Try.toDisjunction" should {
    "create left for Failure" in {
      Try(throw new Exception()).toDisjunction shouldBe (left)
    }
    "create right for values" in {
      Try(1).toDisjunction.value shouldBe 1
    }
  }
}
