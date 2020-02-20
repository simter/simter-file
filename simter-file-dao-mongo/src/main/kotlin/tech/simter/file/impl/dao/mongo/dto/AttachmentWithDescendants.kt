package tech.simter.file.impl.dao.mongo.dto

import tech.simter.file.impl.domain.AttachmentWithUpperImpl

internal data class AttachmentWithDescendants(
  val id: String,
  val descendants: List<AttachmentWithUpperImpl>?
)