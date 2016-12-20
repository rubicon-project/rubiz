package rubiz

// scalastyle:off object.name

package object syntax {
  object all
    extends EitherSyntax
    with TaskSyntax
    with TrySyntax
    with CatchableSyntax

  object either extends EitherSyntax
  object task extends TaskSyntax
  object `try` extends TrySyntax
  object catchable extends CatchableSyntax
}
