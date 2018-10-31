package tech.simter.file.dto

import tech.simter.file.common.DynamicBean

class AttachmentDto4Update : DynamicBean() {
  var name: String? by holder
  var upperId: String? by holder
  var path: String? by holder
  var puid: String? by holder
}