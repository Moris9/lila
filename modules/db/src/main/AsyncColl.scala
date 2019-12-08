package lila.db

import dsl._

final class AsyncColl(resolve: () => Fu[Coll]) {

  def get: Fu[Coll] = resolve()

  def apply[A](f: Coll => Fu[A]) = get flatMap f

  def map[A](f: Coll => A): Fu[A] = get map f
}
