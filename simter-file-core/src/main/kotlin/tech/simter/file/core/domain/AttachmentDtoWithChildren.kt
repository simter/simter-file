package tech.simter.file.core.domain

/**
 * The dto of the [Attachment] basic information plus descendants information
 *
 * @author zh
 * */
class AttachmentDtoWithChildren : AttachmentDto() {
  var children: List<AttachmentDtoWithChildren>? by holder

  fun copy(attachment: Attachment): AttachmentDtoWithChildren {
    this.id = attachment.id
    this.name = attachment.name
    this.type = attachment.type
    this.size = attachment.size
    this.modifyOn = attachment.modifyOn
    this.modifier = attachment.modifier
    return this
  }

  fun copy(node: AttachmentDtoWithUpper): AttachmentDtoWithChildren {
    this.id = node.id
    this.name = node.name
    this.type = node.type
    this.size = node.size
    this.modifyOn = node.modifyOn
    this.modifier = node.modifier
    return this
  }

  fun generateChildren(children: List<AttachmentDtoWithUpper>): AttachmentDtoWithChildren {
    var descendants = children
    this.children = listOf()
    val queue = mutableListOf(this)
    while (queue.isNotEmpty()) {
      val top = queue.removeAt(0)
      descendants.groupBy { if (top.id == it.upperId) "children" else "other" }
        .also {
          descendants = it["other"] ?: listOf()
          (it["children"] ?: listOf()).map { c -> AttachmentDtoWithChildren().copy(c) }
            .also { children ->
              top.children = children
              queue.addAll(children)
            }
        }
    }
    return this
  }
}