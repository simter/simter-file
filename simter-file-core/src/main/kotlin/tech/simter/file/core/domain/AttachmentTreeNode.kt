package tech.simter.file.core.domain

import tech.simter.file.impl.domain.AttachmentTreeNodeImpl

/**
 * A tree-node attachment with its children.
 *
 * @author RJ
 * */
interface AttachmentTreeNode :
  AttachmentIdentityProperties,
  AttachmentRequiredProperties,
  AttachmentModificationProperties {
  val children: List<AttachmentTreeNode>?

  companion object {
    fun from(upperId: String, descendants: List<AttachmentWithUpper>): List<AttachmentTreeNode> {
      return AttachmentTreeNodeImpl.from(upperId, descendants)
    }
  }
}

interface AttachmentWithUpper :
  AttachmentIdentityProperties,
  AttachmentRequiredProperties,
  AttachmentUpperProperties,
  AttachmentModificationProperties