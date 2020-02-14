package tech.simter.file.dao.reactive.mongo

import tech.simter.file.dto.AttachmentDtoWithChildren
import tech.simter.file.dto.AttachmentDtoWithUpper

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