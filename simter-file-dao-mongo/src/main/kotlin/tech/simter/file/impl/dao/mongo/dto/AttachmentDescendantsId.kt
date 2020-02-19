package tech.simter.file.impl.dao.mongo.dto

data class AttachmentDescendantsId(
  private val id: String,
  private val aggregate: List<Id>
) {
  data class Id(val id: String)

  val descendants: List<String>
    get() {
      return mutableListOf(id).plus(aggregate.map { it.id })
    }
}