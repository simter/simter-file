package tech.simter.file.impl.dao.reactive.mongo

data class AttachmentDescendentsId(private val id: String,
                                   private val aggregate: List<Id>) {
  data class Id(val id: String)

  val descendents: List<String>
    get() {
      return mutableListOf(id).plus(aggregate.map { it.id })
    }
}