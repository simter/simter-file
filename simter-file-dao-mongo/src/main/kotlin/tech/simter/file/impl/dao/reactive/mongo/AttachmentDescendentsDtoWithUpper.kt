package tech.simter.file.impl.dao.reactive.mongo

import tech.simter.file.core.domain.AttachmentDtoWithChildren
import tech.simter.file.core.domain.AttachmentDtoWithUpper

data class AttachmentDescendentsDtoWithUpper(private val id: String,
                                             private val aggregate: List<AttachmentDtoWithUpper>) {
  val dtoWithChildren: AttachmentDtoWithChildren
    get() {
      return AttachmentDtoWithChildren().also {
        it.id = this.id
        it.generateChildren(this.aggregate.reversed())
      }
    }
}