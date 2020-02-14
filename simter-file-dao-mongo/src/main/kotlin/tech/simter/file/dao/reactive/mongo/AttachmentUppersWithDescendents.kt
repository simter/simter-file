package tech.simter.file.dao.reactive.mongo

import tech.simter.file.core.domain.AttachmentDto4Zip

fun AttachmentDto4Zip.generateId() {
  id = origin?.let { "\"$origin\"-\"$terminus\"" } ?: "null-\"$terminus\""
}

fun List<AttachmentUppersWithDescendents>.convertToAttachmentDto4Zip(): List<AttachmentDto4Zip> {
  if (isEmpty()) return listOf()
  val commonAncestors = this.map { it.uppers }.reduce { reduce, next ->
    reduce.intersect(next).toList()
  }
  val commonAncestorCount = commonAncestors.size
  val leastCommonAncestor = commonAncestors.lastOrNull()?.id
  return this.map {
    val uppers = it.uppers
    var descendents = it.descendents.reversed()
    val root = AttachmentDto4Zip().apply {
      origin = leastCommonAncestor
      terminus = it.id
      zipPath = (
        if (commonAncestorCount == 0) uppers
        else uppers.takeLast(uppers.size - commonAncestorCount + 1)
        ).joinToString("/") { it.name }
      physicalPath = uppers.joinToString("/") { it.path }
      type = it.type
      generateId()
    }
    val results = mutableListOf(root)
    val queue = mutableListOf(root)
    while (queue.isNotEmpty()) {
      val top = queue.removeAt(0)
      descendents.groupBy { if (top.terminus == it.upperId) "children" else "other" }.also {
        descendents = it["other"] ?: listOf()
        (it["children"] ?: listOf()).map {
          AttachmentDto4Zip().apply {
            origin = leastCommonAncestor
            terminus = it.id
            zipPath = "${top.zipPath}/${it.name}"
            physicalPath = "${top.physicalPath}/${it.path}"
            type = it.type
            generateId()
          }
        }.also {
          results.addAll(it)
          queue.addAll(it)
        }
      }
    }
    results
  }
    .fold(listOf<AttachmentDto4Zip>()) { reduce, next -> reduce.plus(next) }
    .distinct()
}

data class AttachmentUppersWithDescendents(val id: String,
                                           val type: String,
                                           val descendents: List<Info>,
                                           val uppers: List<Info>) {
  data class Info(val id: String,
                  val type: String,
                  val path: String,
                  val name: String,
                  val upperId: String?)
}