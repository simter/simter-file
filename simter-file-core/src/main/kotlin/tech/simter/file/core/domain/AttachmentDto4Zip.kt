package tech.simter.file.core.domain

import tech.simter.file.core.domain.DynamicBean
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class AttachmentDto4Zip : DynamicBean() {
  /**
   * the origin is nullable, so cannot set terminus and origin to joint the primary key
   * if origin is null, id = "null-$terminus"
   * if origin is not null, id = "\"$origin\"-\"$terminus\""
   */
  @get:Id
  var id: String? by holder
  /** descendant's id  */
  var terminus: String? by holder
  /** least-common-ancestor's id  */
  var origin: String? by holder
  var physicalPath: String? by holder
  var zipPath: String? by holder
  var type: String? by holder
}