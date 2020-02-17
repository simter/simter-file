package tech.simter.file.core.domain

/**
 * The dto of the [Attachment] create fields.
 *
 * @author zh
 * @author RJ
 * */
interface AttachmentCreateInfo :
  AttachmentIdentityProperties,
  AttachmentRequiredProperties,
  AttachmentLinkProperties{
  val path: String
}