package tech.simter.file.impl.dao.jpa

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import tech.simter.exception.NotFoundException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.domain.*
import tech.simter.file.impl.dao.jpa.po.AttachmentPo
import java.util.*
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
@Repository
@Transactional
class AttachmentDaoImpl @Autowired constructor(
  @PersistenceContext private val em: EntityManager,
  private val repository: AttachmentJpaRepository
) : AttachmentDao {
  @Suppress("UNCHECKED_CAST")
  override fun findPuids(vararg ids: String): Flux<Optional<String>> {
    if (ids.isEmpty()) return Flux.empty()
    val sql = "select distinct a.puid from AttachmentPo a where id in (:ids) order by a.puid asc nulls first"
    val result = em.createQuery(sql)
      .setParameter("ids", ids.toList())
      .resultList as List<String?>
    return result.map { Optional.ofNullable<String?>(it) }.toFlux()
  }

  @Suppress("UNCHECKED_CAST")
  override fun findDescendantsZipPath(vararg ids: String): Flux<AttachmentDto4Zip> {
    if (ids.isEmpty()) return Flux.empty()
    val sql = """
      with recursive
      -- Below the subtree is subtree from root to "ids"
      -- e: subtree of edge plus edge from "ids" to it
      e(id, upper_id) as (
        select id as id, id as upper_id from st_attachment where id in (:ids)
        union
        select a.id, a.upper_id from st_attachment as a join e on a.id = e.upper_id
      )
      -- o：e's node of out-degree
      , o(id, count) as (
        select upper_id as id, count(0) as count from e group by upper_id
      )
      -- c: "ids" of common-ancestor
      , c(id, upper_id, least) as(
        select o.id as id, o.id as upper_id, count <> 1 as least from o where  o.id is null
        union
        select e.id, e.upper_id, count <> 1 or e.id in (:ids)
          from e join o on e.id = o.id
          join c on (e.upper_id = c.id or (c.id is null and e.upper_id is null))
            and c.least = false
      )
      -- l: "ids" of least-common-ancestor
      , l(id, upper_id) as (
        select id as id, upper_id as upper_id from c where least = true
      )
      -- a: path from "ids" of all ancestors to "ids"
      , a(id, upper_id, physical_path, zip_path, type) as (
        select id as id, upper_id as upper_id, concat(path, '') as physical_path, concat(name, '') as zip_path, type as type
          from st_attachment as a where id in (:ids)
        union
        select a.id, s.upper_id, concat(path, '/', physical_path), concat(name, '/', zip_path), a.type
          from st_attachment as s
          join a on a.upper_id = s.id
      )
      -- d: zip_path from "ids" of least-common-ancestor to "ids" of all descendant
      , d(id, lca_id, zip_path, type) as (
        select a.id as id, l.id as lca_id, a.zip_path as zip_path, a.type as type
          from a
          join l on a.upper_id = l.upper_id or (a.upper_id is null and l.upper_id is null)
        union
        select a.id, d.lca_id, concat(zip_path, '/', name), a.type
          from st_attachment as a join d on a.upper_id = d.id
      )
       -- d2: physical_path from "ids" of root to "ids" of all descendant
      , d2(id, physical_path) as (
        select a.id as id, a.physical_path as physical_path
          from a where a.upper_id is null
        union
        select a.id, concat(physical_path, '/', path)
          from st_attachment as a join d2 as d on a.upper_id = d.id
      )
      select d.id as terminus, lca_id as origin, physical_path, zip_path, type,
        concat(case when lca_id is null then 'null' else concat('"', lca_id, '"') end, '-"', d.id, '"') as id
      from d, d2 where d.id = d2.id
      order by d.zip_path asc
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
      val result = em
        .createQuery("update AttachmentPo set ${data.keys.joinToString(", ") { "$it = :$it" }} where id =:id")
        .apply { data.forEach { (key, value) -> setParameter(key, value) } }
        .setParameter("id", id)
        .executeUpdate()
      em.clear()
      if (result > 0) Mono.empty<Void>()
      else Mono.error(NotFoundException())
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun findDescendants(id: String): Flux<AttachmentDtoWithChildren> {
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
    val descendants = em.createNativeQuery(sql, AttachmentDtoWithUpper::class.java)
      .setParameter("id", id)
      .resultList as List<AttachmentDtoWithUpper>
    return AttachmentDtoWithChildren().apply {
      this.id = id
      generateChildren(descendants)
    }.children!!.toFlux()
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
    return try {
      Mono.just(em.createNativeQuery(sql).setParameter("id", id).singleResult as String)
    } catch (e: NoResultException) {
      Mono.empty()
    } catch (e: Throwable) {
      Mono.error(e)
    }
  }

  override fun get(id: String): Mono<Attachment> {
    return Mono.justOrEmpty(repository.findById(id))
  }

  @Suppress("UNCHECKED_CAST")
  override fun find(pageable: Pageable): Mono<Page<Attachment>> {
    return Mono.justOrEmpty(repository.findAll(pageable) as Page<Attachment>)
  }

  @Suppress("UNCHECKED_CAST")
  override fun find(puid: String, upperId: String?): Flux<Attachment> {
    val hasSubgroup = null != upperId
    val sql = """
      select a from AttachmentPo a
      where puid = :puid ${if (hasSubgroup) "and upperId = :upperId" else ""}
      order by createOn desc
    """.trimIndent()
    val query: Query = em.createQuery(sql, AttachmentPo::class.java).setParameter("puid", puid)
    if (hasSubgroup) query.setParameter("upperId", upperId)
    return Flux.fromIterable(query.resultList as List<Attachment>)
  }

  override fun save(vararg attachments: Attachment): Mono<Void> {
    repository.saveAll(attachments.asIterable().map { AttachmentPo.from(it) })
    return Mono.empty()
  }

  @Suppress("UNCHECKED_CAST")
  override fun delete(vararg ids: String): Flux<String> {
    return if (ids.isNotEmpty()) {
      // Query the full path of the attachments
      val fullPathSql = """
        with recursive p(id, path, upper_id) as (
          select id, concat(path, ''), upper_id from st_attachment where id in (:ids)
          union
          select p.id, concat(a.path, '/', p.path), a.upper_id from st_attachment as a
          join p on a.id = p.upper_id
          -- If the ancestors of attachment in the attachments list, ignored the attachment
          where p.upper_id not in (:ids)
        )
        select id, path as full_path from p where upper_id is null
      """.trimIndent()
      val fullPathDaos = em.createNativeQuery(fullPathSql, AttachmentDto4FullPath::class.java)
        .setParameter("ids", ids.toList())
        .resultList as List<AttachmentDto4FullPath>

      // Delete attachments and all theirs descendants
      val nodeSql = """
        with recursive u(id, upper_id ) as (
          select id, upper_id  from st_attachment where id in (:ids)
          union
          select a.id, a.upper_id from st_attachment as a join u on a.upper_id = u.id
        )
        select id from u
      """.trimIndent()
      val nodeDtos = em.createNativeQuery(nodeSql).setParameter("ids", ids.toList()).resultList
      if (nodeDtos.isNotEmpty()) {
        em.createNativeQuery("delete from st_attachment where id in (:ids)")
          .setParameter("ids", nodeDtos).executeUpdate()
      }
      fullPathDaos.map { it.fullPath!! }.toFlux()
    } else Flux.empty()
  }
}
