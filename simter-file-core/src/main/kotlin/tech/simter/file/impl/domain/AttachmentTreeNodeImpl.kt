package tech.simter.file.impl.domain

import tech.simter.file.core.domain.AttachmentTreeNode
import tech.simter.file.core.domain.AttachmentWithUpper
import java.time.OffsetDateTime

data class AttachmentTreeNodeImpl(
  override val id: String,
  override val path: String,
  override val name: String,
  override val type: String,
  override val size: Long,
  override val modifyOn: OffsetDateTime,
  override val modifier: String,
  override var children: List<AttachmentTreeNode>?
) : AttachmentTreeNode {
  companion object {
    internal fun from(upperId: String, descendants: List<AttachmentWithUpper>): List<AttachmentTreeNode> {
      var descendantsT = descendants
      val upper = AttachmentTreeNodeImpl(
        id = upperId,
        children = listOf(),
        name = "",
        type = "",
        path = "",
        size = 0,
        modifier = "",
        modifyOn = OffsetDateTime.now()
      )
      val queue = mutableListOf(upper)
      while (queue.isNotEmpty()) {
        val top = queue.removeAt(0)
        descendantsT.groupBy { if (top.id == it.upperId) "children" else "other" }
          .also { groups ->
            descendantsT = groups["other"] ?: emptyList()

            (groups["children"] ?: emptyList())
              .map(::from)
              .also { children ->
                top.children = children
                queue.addAll(children)
              }
          }
      }
      return upper.children ?: emptyList()
    }

    private fun from(node: AttachmentWithUpper): AttachmentTreeNodeImpl {
      return AttachmentTreeNodeImpl(
        id = node.id,
        name = node.name,
        type = node.type,
        size = node.size,
        path = node.path,
        modifyOn = node.modifyOn,
        modifier = node.modifier,
        children = null
      )
    }
  }
}

data class AttachmentWithUpperImpl(
  override val id: String,
  override val path: String,
  override val name: String,
  override val type: String,
  override val size: Long,
  override val modifyOn: OffsetDateTime,
  override val modifier: String,
  override val upperId: String?
) : AttachmentWithUpper