package tech.simter.file.impl.dao.mongo.dto

internal data class AttachmentUppersPath(
  private val id: String,
  private val aggregate: List<Path>
) {
  data class Path(val path: String)

  val fullPath: String
    get() {
      return this.aggregate.joinToString(separator = "/") { it.path }
    }
}