package lila.coach

import java.io.File

import lila.db.DbImage
import lila.db.dsl._

private final class Photographer(coll: Coll) {

  import Photographer.uploadMaxMb
  private val uploadMaxBytes = uploadMaxMb * 1024 * 1024
  private def pictureId(id: Coach.Id) = s"coach:${id.value}:picture"

  def apply(coachId: Coach.Id, uploaded: Photographer.Uploaded): Fu[DbImage] =
    if (uploaded.ref.path.toFile.length > uploadMaxBytes)
      fufail(s"File size must not exceed ${uploadMaxMb}MB.")
    else {

      process(uploaded.ref.path.toFile)

      val image = DbImage.make(
        id = pictureId(coachId),
        name = sanitizeName(uploaded.filename),
        contentType = uploaded.contentType,
        file = uploaded.ref.path.toFile
      )

      coll.update($id(image.id), image, upsert = true) inject image
    }

  private def process(file: File) = {

    import com.sksamuel.scrimage._

    Image.fromFile(file).cover(500, 500).output(file)
  }

  private def sanitizeName(name: String) = {
    // the char `^` breaks play, even URL encoded
    java.net.URLEncoder.encode(name, "UTF-8").replace("%", "")
  }
}

object Photographer {

  val uploadMaxMb = 3

  type Uploaded = play.api.mvc.MultipartFormData.FilePart[play.api.libs.Files.TemporaryFile]
}
