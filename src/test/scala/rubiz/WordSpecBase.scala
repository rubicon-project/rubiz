package rubiz

import org.scalatest.{ Matchers, OptionValues, WordSpec }
import org.scalatest.prop.Checkers
import org.typelevel.scalatest.{ DisjunctionMatchers, DisjunctionValues }

abstract class WordSpecBase
  extends WordSpec
  with Matchers
  with OptionValues
  with Checkers
  with DisjunctionMatchers
  with DisjunctionValues