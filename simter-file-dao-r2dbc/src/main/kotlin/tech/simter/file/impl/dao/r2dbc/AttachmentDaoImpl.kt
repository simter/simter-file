package tech.simter.file.impl.dao.r2dbc

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.*
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.data.r2dbc.core.DatabaseClient
import org.springframework.data.r2dbc.query.Criteria
import org.springframework.data.r2dbc.query.Update
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import tech.simter.exception.NotFoundException
import tech.simter.file.TABLE_ATTACHMENT
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.domain.Attachment
import tech.simter.file.core.domain.AttachmentTreeNode
import tech.simter.file.core.domain.AttachmentZipInfo
import tech.simter.file.impl.dao.r2dbc.po.AttachmentPo
import tech.simter.file.impl.domain.AttachmentWithUpperImpl
import tech.simter.file.impl.domain.AttachmentZipInfoImpl
import tech.simter.util.StringUtils.underscore
import java.util.*

/**
 * The spring-data-r2dbc implementation of [AttachmentDao].
 *
 * @author RJ
 */
@Repository
class AttachmentDaoImpl @Autowired constructor(
  private val databaseClient: DatabaseClient,
  private val repository: AttachmentRepository
) : AttachmentDao {
  @Suppress("UNCHECKED_CAST")
  override fun get(id: String): Mono<Attachment> {
    return repository.findById(id) as Mono<Attachment>
  }

  @Suppress("UNCHECKED_CAST")
  override fun find(pageable: Pageable): Mono<Page<Attachment>> {
    return Mono.zip(
      // query content
      databaseClient.select()
        .from(TABLE_ATTACHMENT)
        .page(PageRequest.of(
          pageable.pageNumber,
          if (pageable.pageSize < 1) 25 else pageable.pageSize,
          pageable.getSortOr(Sort.by(DESC, "create_on")) // default order by createOn desc
        ))
        .`as`(AttachmentPo::class.java)
        .fetch()
        .all()
        .collectList(),
      // query total count
      databaseClient.execute("select count(*) c from $TABLE_ATTACHMENT")
        .`as`(Long::class.javaObjectType)
        .fetch()
        .one()
    ) { content, total -> PageImpl(content, pageable, total) as Page<Attachment> }
      .defaultIfEmpty(Page.empty<Attachment>(pageable))
  }

  @Suppress("UNCHECKED_CAST")
  override fun find(puid: String, upperId: String?): Flux<Attachment> {
    val criteria = Criteria.where("puid").`is`(puid)
    return databaseClient.select()
      .from(TABLE_ATTACHMENT)
      .matching(if (null == upperId) criteria else criteria.and("upper_id").`is`(upperId))
      .orderBy(Sort.by(DESC, "create_on")) // default order by createOn desc
      .`as`(AttachmentPo::class.java)
      .fetch()
      .all() as Flux<Attachment>
  }

  override fun save(vararg attachments: Attachment): Mono<Void> {
    return if (attachments.isEmpty()) Mono.empty()
    else repository.saveAll(attachments.asIterable().map { AttachmentPo.from(it) }).then()
  }

  override fun delete(vararg ids: String): Flux<String> {
    return if (ids.isEmpty()) Flux.empty()
    else {
      // 1. Query the full path of the attachments
      val fullPathSql = """
        with recursive p(id, path, upper_id) as (
          select id, concat(path, ''), upper_id
            from st_attachment where id in (:ids)
          union
          select p.id, concat(a.path, '/', p.path), a.upper_id
            from st_attachment as a
            join p on a.id = p.upper_id
            -- If the ancestors of attachment in the attachments list, ignored the attachment
            where p.upper_id not in (:ids1)
        )
        select path as full_path from p where upper_id is null
      """.trimIndent()
      return databaseClient.execute(fullPathSql)
        .bind("ids", ids.toList())
        .bind("ids1", ids.toList()) // why? see https://github.com/spring-projects/spring-data-r2dbc/issues/310
        .`as`(String::class.java)
        .fetch()
        .all()
        .collectList()
        .flatMapMany { fullPaths ->
          // 2. Find all attachmentIds to delete include all theirs descendants
          val nodeSql = """
            with recursive u(id, upper_id ) as (
              select id, upper_id  from st_attachment where id in (:ids)
              union
              select a.id, a.upper_id from st_attachment as a join u on a.upper_id = u.id
            )
            select id from u
          """.trimIndent()
          databaseClient.execute(nodeSql)
            .bind("ids", ids.toList())
            .`as`(String::class.java)
            .fetch()
            .all()
            .collectList()
            .flatMap {
              // 3. Delete attachments and all theirs descendants
              if (it.isNotEmpty()) databaseClient.execute("delete from st_attachment where id in (:ids)")
                .bind("ids", it)
                .`as`(String::class.java)
                .fetch()
                .rowsUpdated()
              else Mono.empty()
            }
            .flatMapMany {
              // 4. Return fullPaths
              fullPaths.toFlux()
            }
        }
    }
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
    return databaseClient.execute(sql)
      .bind("id", id)
      .`as`(String::class.java)
      .fetch()
      .one()
  }

  override fun findDescendants(id: String): Flux<AttachmentTreeNode> {
    val sql = """
      with recursive n(id, path, paths, name, type, size, modify_on, modifier, upper_id) as (
        select id, path, path as paths, name, type, size, modify_on, modifier, upper_id
          from st_attachment where upper_id = :id
        union
        select c.id, c.path, concat(n.path, '/', c.path), c.name, c.type, c.size, c.modify_on, c.modifier, c.upper_id
          from st_attachment as c join n on c.upper_id = n.id
      )
      select id, path, name, type, size, modify_on, modifier, upper_id
        from n order by paths asc
    """.trimIndent()
    return databaseClient.execute(sql)
      .bind("id", id)
      .`as`(AttachmentWithUpperImpl::class.java)
      .fetch()
      .all()
      .collectList()
      .flatMapIterable { descendants ->
        AttachmentTreeNode.from(
          upperId = id,
          descendants = descendants
        )
      }
  }

  override fun update(id: String, info: Map<String, Any?>): Mono<Void> {
    if (info.isEmpty()) return Mono.empty() // nothing to update

    // convert camel-case key to underscore key
    val originKeys = info.keys.toList()
    val underscoreKeys = underscore(originKeys.joinToString(",")).split(",")
    // build Update instance
    lateinit var update: Update
    originKeys.forEachIndexed { index, originKey ->
      update = if (index == 0) Update.update(underscoreKeys[index], info[originKey])
      else update.set(underscoreKeys[index], info[originKey])
    }

    // 执行 update
    return databaseClient.update()
      .table(TABLE_ATTACHMENT)
      .using(update)
      .matching(Criteria.where("id").`is`(id))
      .fetch()
      .rowsUpdated()
      .doOnNext { if (it < 1) throw NotFoundException() }
      .then()
  }

  @Suppress("UNCHECKED_CAST")
  override fun findDescendantsZipPath(vararg ids: String): Flux<AttachmentZipInfo> {
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
        select e.id, e.upper_id, count <> 1 or e.id in (:ids1)
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
          from st_attachment as a where id in (:ids2)
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
    val idList = ids.toList()
    return databaseClient.execute(sql)
      .bind("ids", idList)
      .bind("ids1", idList) // why? see https://github.com/spring-projects/spring-data-r2dbc/issues/310
      .bind("ids2", idList)
      .`as`(AttachmentZipInfoImpl::class.java)
      .fetch()
      .all() as Flux<AttachmentZipInfo>
  }

  override fun findPuids(vararg ids: String): Flux<Optional<String>> {
    if (ids.isEmpty()) return Flux.empty()
    val sql = "select distinct (case when a.puid is null then '' else a.puid end) as puid" +
      " from $TABLE_ATTACHMENT a where id in (:ids) order by puid asc"
    return databaseClient.execute(sql)
      .bind("ids", ids.toList())
      .`as`(String::class.java)
      .fetch()
      .all()
      .map { if (it.isEmpty()) Optional.empty() else Optional.of(it) }
  }
}