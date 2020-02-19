package tech.simter.file.impl.dao.mongo.dto

import tech.simter.file.core.domain.AttachmentDto4Zip

fun AttachmentDto4Zip.generateId() {
  id = origin?.let { "\"$origin\"-\"$terminus\"" } ?: "null-\"$terminus\""
}

fun List<AttachmentUppersWithDescendants>.convertToAttachmentDto4Zip(): List<AttachmentDto4Zip> {
  if (isEmpty()) return listOf()
  val commonAncestors = this.map { it.uppers }.reduce { reduce, next ->
    reduce.intersect(next).toList()
  }
  val commonAncestorCount = commonAncestors.size
  val leastCommonAncestor = commonAncestors.lastOrNull()?.id
  return this.map { ud ->
    val uppers = ud.uppers
    var descendants = ud.descendants.reversed()
    val root = AttachmentDto4Zip().apply {
      origin = leastCommonAncestor
      terminus = ud.id
      zipPath = (
        if (commonAncestorCount == 0) uppers
        else uppers.takeLast(uppers.size - commonAncestorCount + 1)
        ).joinToString("/") { it.name }
      physicalPath = uppers.joinToString("/") { it.path }
      type = ud.type
      generateId()
    }
    val results = mutableListOf(root)
    val queue = mutableListOf(root)
    while (queue.isNotEmpty()) {
      val top = queue.removeAt(0)
      descendants.groupBy { if (top.terminus == it.upperId) "children" else "other" }.also {
        descendants = it["other"] ?: listOf()
        (it["children"] ?: listOf()).map {
          AttachmentDto4Zip().apply {
            origin = leastCommonAncestor
            terminus = it.id
            zipPath = "${top.zipPath}/${it.name}"
            physicalPath = "${top.physicalPath}/${it.path}"
            type = it.type
            generateId()
          }
        }.also { list ->
          results.addAll(list)
          queue.addAll(list)
        }
      }
    }
    results
  }
    .fold(listOf<AttachmentDto4Zip>()) { reduce, next -> reduce.plus(next) }
    .distinct()
}

data class AttachmentUppersWithDescendants(
  val id: String,
  val type: String,
  val descendants: List<Info>,
  val uppers: List<Info>
) {
  data class Info(
    val id: String,
    val type: String,
    val path: String,
    val name: String,
    val upperId: String?
  )
}