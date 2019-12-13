package lila.db

import com.github.ghik.silencer.silent
import reactivemongo.api._
import scala.collection.Factory

trait QueryBuilderExt { self: dsl =>

  implicit final class ExtendQueryBuilder[P <: SerializationPack](
      @silent b: collections.GenericQueryBuilder[P]
  ) {

    // like collect, but with stopOnError defaulting to false
    def gather[A, M[_]](upTo: Int, readPreference: ReadPreference = ReadPreference.primary)(
        implicit
        factory: Factory[A, M[A]],
        reader: b.pack.Reader[A],
        cp: CursorProducer[A]
    ): Fu[M[A]] =
      b.cursor[A](readPreference = readPreference)(reader, cp)
        .collect[M](upTo, Cursor.ContOnError[M[A]]())

    def list[A: b.pack.Reader](): Fu[List[A]] =
      gather[A, List](Int.MaxValue)

    def list[A: b.pack.Reader](limit: Int): Fu[List[A]] =
      gather[A, List](limit)

    def list[A: b.pack.Reader](limit: Option[Int]): Fu[List[A]] =
      gather[A, List](limit | Int.MaxValue)

    def list[A: b.pack.Reader](limit: Option[Int], readPreference: ReadPreference): Fu[List[A]] =
      gather[A, List](limit | Int.MaxValue, readPreference)

    def list[A: b.pack.Reader](limit: Int, readPreference: ReadPreference): Fu[List[A]] =
      gather[A, List](limit, readPreference)
  }
}
