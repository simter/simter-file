package tech.simter.file.impl.dao.mongo.dto

import tech.simter.file.core.domain.AttachmentZipInfo
import tech.simter.file.impl.domain.AttachmentZipInfoImpl

internal fun generateId(origin: String?, terminus: String): String {
  return origin?.let { "\"$origin\"-\"$terminus\"" } ?: "null-\"$terminus\""
}

internal fun List<AttachmentUppersWithDescendants>.convertToAttachmentZipInfo(): List<AttachmentZipInfo> {
  if (isEmpty()) return listOf()
  val commonAncestors = this.map { it.uppers }.reduce { reduce, next ->
    reduce.intersect(next).toList()
  }
  val commonAncestorCount = commonAncestors.size
  val leastCommonAncestor = commonAncestors.lastOrNull()?.id
  return this.map { ud ->
    val uppers = ud.uppers
    var descendants = ud.descendants.reversed()
    val root = AttachmentZipInfoImpl(
      origin = leastCommonAncestor,
      terminus = ud.id,
      zipPath = (
        if (commonAncestorCount == 0) uppers
        else uppers.takeLast(uppers.size - commonAncestorCount + 1)
        ).joinToString("/") { it.name },
      physicalPath = uppers.joinToString("/") { it.path },
      type = ud.type,
      id = generateId(leastCommonAncestor, ud.id)
    )
    val results = mutableListOf(root)
    val queue = mutableListOf(root)
    while (queue.isNotEmpty()) {
      val top = queue.removeAt(0)
      descendants.groupBy { if (top.terminus == it.upperId) "children" else "other" }.also { map ->
        descendants = map["other"] ?: listOf()
        (map["children"] ?: listOf()).map {
          AttachmentZipInfoImpl(
            origin = leastCommonAncestor,
            terminus = it.id,
            zipPath = "${top.zipPath}/${it.name}",
            physicalPath = "${top.physicalPath}/${it.path}",
            type = it.type,
            id = generateId(leastCommonAncestor, it.id)
          )
        }.also { list ->
          results.addAll(list)
          queue.addAll(list)
        }
      }
    }
    results
  }
    .fold(listOf<AttachmentZipInfo>()) { reduce, next -> reduce.plus(next) }
    .distinct()
}

internal data class AttachmentUppersWithDescendants(
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