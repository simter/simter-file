package tech.simter.file.dao.jpa

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import tech.simter.exception.NotFoundException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.dto.AttachmentDto4FullPath
import tech.simter.file.dto.AttachmentDto4Zip
import tech.simter.file.dto.AttachmentDtoWithChildren
import tech.simter.file.dto.AttachmentDtoWithUpper
import tech.simter.file.po.Attachment
import java.io.File
import javax.persistence.EntityManager
import javax.persistence.NoResultException
import javax.persistence.PersistenceContext
import javax.persistence.Query

/**
 * The JPA implementation of [AttachmentDao].
 *
 * @author RJ
 * @author zh
 */
@Component
@Transactional
class AttachmentDaoImpl @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  @PersistenceContext private val em: EntityManager,
  private val repository: AttachmentJpaRepository
) : AttachmentDao {
  @Suppress("UNCHECKED_CAST")
  override fun findDescendentsZipPath(vararg ids: String): Flux<AttachmentDto4Zip> {
    if (ids.isEmpty()) return Flux.empty()
    val sql = """
      with recursive
      -- Below the subtree is subtree from root to "ids"
      -- e: subtree of edge plus edge from "ids" to it
      e(id, upper_id) as (
        select id, id from st_attachment where id in :ids
        union
        select a.id, a.upper_id from st_attachment as a
        join e on a.id = e.upper_id
      ),
      -- o：e's node of out-degree
      o(id, count) as (
        select upper_id, count(0) from e group by upper_id
      ),
      -- c: "ids" of common-ancestor
      c(id, upper_id, least) as(
        select o.id, o.id, count <> 1 from o where  o.id is null
        union
        select e.id, e.upper_id, count <> 1 or e.id in :ids
        from e join o on e.id = o.id
        join c on (e.upper_id = c.id or (c.id is null and e.upper_id is null))
          and c.least = false
      ),
      -- l: "ids" of least-common-ancestor
      l(id, upper_id)
      as (
        select id, upper_id from c where least = true
      ),
      -- a: path from "ids" of all ancestors to "ids"
      a(id, upper_id, physical_path, zip_path, type)
      as(
        select id, upper_id, concat(path, ''), concat(name, ''), type
        from st_attachment as a where id in :ids
        union
        select a.id, s.upper_id, concat(path, '/', physical_path), concat(name, '/', zip_path), a.type
        from st_attachment as s
        join a on a.upper_id = s.id
      )
      -- d: path from "ids" of least-common-ancestor to "ids" of all descendant
      , d(id, lca_id, physical_path, zip_path, type)
      as(
        select a.id, l.id, a.physical_path, a.zip_path, a.type
        from a
        join l on a.upper_id = l.upper_id or (a.upper_id is null and l.upper_id is null)
        union
        select a.id, d.lca_id, concat(physical_path, '/', path), concat(zip_path, '/', name), a.type
        from st_attachment as a join d on a.upper_id = d.id
      )
      select id as terminus, lca_id as origin, physical_path, zip_path, type,
        concat(case when lca_id is null then 'null' else concat('"', lca_id, '"') end, '-"', id, '"') as id
      from d
    """.trimIndent()
    val dtos = em.createNativeQuery(sql, AttachmentDto4Zip::class.java)
      .setParameter("ids", ids.toList())
      .resultList as List<AttachmentDto4Zip>
    return dtos.toFlux()
  }

  override fun update(id: String, data: Map<String, Any?>): Mono<Void> {
    return if (data.isEmpty()) {
      Mono.empty()
    } else {
      Mono.fromSupplier {
        em.createQuery("update Attachment set ${data.keys.joinToString(", ") { "$it = :$it" }} where id =:id")
          .apply { data.forEach { key, value -> setParameter(key, value) } }
          .setParameter("id", id)
          .executeUpdate()
      }.delayUntil { em.clear().toMono() }
        .flatMap { if (it > 0) Mono.empty<Void>() else Mono.error(NotFoundException()) }
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun findDescendents(id: String): Flux<AttachmentDtoWithChildren> {
    val sql = """
      with recursive n(id, path, name, type, size, modify_on, modifier, upper_id)
      as (
        select id, path, name, type, size, modify_on, modifier, upper_id
        from st_attachment where upper_id = :id
        union
        select a.id, a.path, a.name, a.type, a.size, a.modify_on, a.modifier, a.upper_id
        from st_attachment as a join n on a.upper_id = n.id
      )
      select id, path, name, type, size, modify_on, modifier, upper_id from n
    """.trimIndent()
    var descendents = em.createNativeQuery(sql, AttachmentDtoWithUpper::class.java)
      .setParameter("id", id)
      .resultList as List<AttachmentDtoWithUpper>
    val root = AttachmentDtoWithChildren().also {
      it.id = id
      it.children = listOf()
    }
    val queue = mutableListOf(root)

    while (queue.isNotEmpty()) {
      val top = queue.removeAt(0)
      descendents.groupBy { if (top.id == it.upperId) "children" else "other" }
        .also {
          descendents = it["other"] ?: listOf()
          it["children"]?.map { AttachmentDtoWithChildren().copy(it) }
            ?.also {
              top.children = it
              queue.addAll(it)
            }
        }
    }

    return root.children!!.toFlux()
  }

  override fun getFullPath(id: String): Mono<String> {
    val sql = """
      with recursive p(id, path, upper_id) as (
        select id, concat(path, ''), upper_id from st_attachment where id = :id
        union
        select a.id, concat(a.path, '/', p.path), a.upper_id from st_attachment as a
        join p on a.id = p.upper_id
      )
      select p.path from p where upper_id is null
    """.trimIndent()
    return Mono.fromSupplier { em.createNativeQuery(sql).setParameter("id", id).singleResult as String }
      .onErrorResume(NoResultException::class.java) { Mono.empty() }
  }

  override fun get(id: String): Mono<Attachment> {
    return Mono.justOrEmpty(repository.findById(id))
  }

  override fun find(pageable: Pageable): Mono<Page<Attachment>> {
    return Mono.justOrEmpty(repository.findAll(pageable))
  }

  @Suppress("UNCHECKED_CAST")
  override fun find(puid: String, upperId: String?): Flux<Attachment> {
    val hasSubgroup = null != upperId
    val sql = """
      select a from Attachment a
      where puid = :puid ${if (hasSubgroup) "and upperId = :upperId" else ""}
      order by createOn desc
    """.trimIndent()
    val query: Query = em.createQuery(sql, Attachment::class.java).setParameter("puid", puid)
    if (hasSubgroup) query.setParameter("upperId", upperId)
    return Flux.fromIterable(query.resultList as List<Attachment>)
  }

  override fun save(vararg attachments: Attachment): Mono<Void> {
    repository.saveAll(attachments.asIterable())
    return Mono.empty()
  }

  @Suppress("UNCHECKED_CAST")
  override fun delete(vararg ids: String): Mono<Void> {
    if (ids.isNotEmpty()) {
      // Query the full path of the attachments
      val fullPathSql = """
        with recursive p(id, path, upper_id) as (
          select id, concat(path, ''), upper_id from st_attachment where id in :ids
          union
          select p.id, concat(a.path, '/', p.path), a.upper_id from st_attachment as a
          join p on a.id = p.upper_id
          -- If the ancestors of attachment in the attachments list, ignored the attachment
          where p.upper_id not in :ids
        )
        select id, path as full_path from p where upper_id is null
      """.trimIndent()
      val fullPathDaos = em.createNativeQuery(fullPathSql, AttachmentDto4FullPath::class.java)
        .setParameter("ids", ids.toList())
        .resultList as List<AttachmentDto4FullPath>

      // Delete attachments and all theirs descendants
      val nodeSql = """
        with recursive u(id, upper_id ) as (
          select id, upper_id  from st_attachment where id in :ids
          union
          select a.id, a.upper_id from st_attachment as a join u on a.upper_id = u.id
        )
        select id from u
      """.trimIndent()
      val nodeDtos = em.createNativeQuery(nodeSql).setParameter("ids", ids.toList()).resultList
      if (nodeDtos.isNotEmpty()) {
        em.createNativeQuery("delete from st_attachment where id in :ids")
          .setParameter("ids", nodeDtos).executeUpdate()
      }

      // Delete physics file
      fullPathDaos.forEach { File("$fileRootDir/${it.fullPath}").deleteRecursively() }
    }
    return Mono.empty<Void>()
  }
}
