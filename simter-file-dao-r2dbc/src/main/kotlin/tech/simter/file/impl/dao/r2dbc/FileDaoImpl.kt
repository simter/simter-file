package tech.simter.file.impl.dao.r2dbc

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query.query
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.file.TABLE_FILE
import tech.simter.file.core.*
import tech.simter.file.core.ModuleMatcher.ModuleEquals
import tech.simter.file.impl.dao.r2dbc.po.FileStorePo
import tech.simter.file.standardModuleValue
import tech.simter.kotlin.data.Page
import java.util.*

/**
 * The spring-data-r2dbc implementation of [FileDao].
 *
 * @author RJ
 */
@Repository
class FileDaoImpl @Autowired constructor(
  private val databaseClient: DatabaseClient,
  private val entityOperations: R2dbcEntityOperations
) : FileDao {
  override fun findPage(
    moduleMatcher: ModuleMatcher,
    search: Optional<String>,
    limit: Int,
    offset: Long
  ): Mono<Page<FileStore>> {
    // create common query
    val select = entityOperations.select(FileStorePo::class.java).from(TABLE_FILE)

    // module condition
    var condition = Criteria.empty()
    condition = when (moduleMatcher) {
      is ModuleEquals -> condition.and("module").`is`(moduleMatcher.module)
      else -> condition.and("module").like(
        if (moduleMatcher.module.endsWith("%")) moduleMatcher.module
        else moduleMatcher.module + "%"
      )
    }

    // search condition
    search.ifPresent {
      condition = condition.and("name").like(
        if (it.contains("%")) it else "%$it%"
      )
    }

    // do page query
    return select.matching(query(condition)).count() // query total count
      .flatMap { totalCount ->
        val sort = Sort.by(DESC, "create_on")
        if (totalCount <= 0) Mono.just(Page.of(
          limit = limit,
          offset = offset,
          total = 0,
          rows = emptyList()
        ))
        else {
          // query real rows
          select.matching(
            query(condition).sort(sort)
              .limit(limit)
              .offset(offset)
          ).all()
            .collectList()
            .map { rows ->
              Page.of(
                limit = limit,
                offset = offset,
                total = totalCount,
                rows = rows as List<FileStore>
              )
            }
        }
      }
  }

  @Suppress("UNCHECKED_CAST")
  override fun findList(
    moduleMatcher: ModuleMatcher,
    search: Optional<String>,
    limit: Optional<Int>
  ): Flux<FileStore> {
    // create common query
    val select = entityOperations.select(FileStorePo::class.java).from(TABLE_FILE)

    // module condition
    var condition = Criteria.empty()
    condition = when (moduleMatcher) {
      is ModuleEquals -> condition.and("module").`is`(moduleMatcher.module)
      else -> condition.and("module").like(
        if (moduleMatcher.module.endsWith("%")) moduleMatcher.module
        else moduleMatcher.module + "%"
      )
    }
    search.ifPresent {
      condition = condition.and("name").like(
        if (it.contains("%")) it else "%$it%"
      )
    }
    var q = query(condition).sort(Sort.by(DESC, "create_on"))
    limit.ifPresent { q = q.limit(it) }
    return select.matching(q).all() as Flux<FileStore>
  }

  override fun create(file: FileStore): Mono<String> {
    return databaseClient.sql("""
      insert into $TABLE_FILE (
        id, module, name, type, size, path, creator, create_on, modifier, modify_on
      ) values (
        :id, :module, :name, :type, :size, :path, :creator, :createOn, :modifier, :modifyOn
      )
    """.trimIndent())
      .bind("module", standardModuleValue(file.module))
      .bind("name", file.name)
      .bind("type", file.type)
      .bind("size", file.size)
      .bind("path", file.path)
      .bind("creator", file.creator)
      .bind("createOn", file.createOn)
      .bind("modifier", file.modifier)
      .bind("modifyOn", file.modifyOn)
      .bind("id", if (file.id.isNotEmpty()) file.id else TODO("Not implemented for auto generate id by database"))
      .fetch()
      .rowsUpdated()
      .map { file.id }
  }

  @Suppress("UNCHECKED_CAST")
  override fun get(id: String): Mono<FileStore> {
    return entityOperations.select(FileStorePo::class.java)
      .from(TABLE_FILE)
      .matching(query(where("id").`is`(id)))
      .one() as Mono<FileStore>
  }

  @Suppress("UNCHECKED_CAST")
  override fun findById(vararg ids: String): Flux<FileStore> {
    return entityOperations.select(FileStorePo::class.java)
      .from(TABLE_FILE)
      .matching(query(where("id").`in`(*ids)))
      .all() as Flux<FileStore>
  }

  override fun delete(vararg ids: String): Mono<Int> {
    return entityOperations.delete(FileStorePo::class.java)
      .from(TABLE_FILE)
      .matching(query(where("id").`in`(*ids)))
      .all()
  }

  override fun delete(moduleMatcher: ModuleMatcher): Mono<Int> {
    // module condition
    var condition = Criteria.empty()
    condition = when (moduleMatcher) {
      is ModuleEquals -> condition.and("module").`is`(moduleMatcher.module)
      else -> condition.and("module").like(
        if (moduleMatcher.module.endsWith("%")) moduleMatcher.module
        else moduleMatcher.module + "%"
      )
    }
    return entityOperations.delete(FileStorePo::class.java)
      .from(TABLE_FILE)
      .matching(query(condition))
      .all()
  }

  override fun update(id: String, info: FileUpdateDescriber): Mono<Boolean> {
    val conditions = mutableListOf<String>()
    val params = mutableMapOf<String, Any>()
    info.module.ifPresent { conditions.add("module = :module"); params["module"] = it; }
    info.name.ifPresent { conditions.add("name = :name"); params["name"] = it; }
    info.type.ifPresent { conditions.add("type = :type"); params["type"] = it; }
    info.size.ifPresent { conditions.add("size = :size"); params["size"] = it; }
    info.path.ifPresent { conditions.add("path = :path"); params["path"] = it; }
    info.modifier.ifPresent { conditions.add("modifier = :modifier"); params["modifier"] = it; }
    info.modifyOn.ifPresent { conditions.add("modify_on = :modifyOn"); params["modifyOn"] = it; }
    if (conditions.isEmpty()) return Mono.just(false)
    var spec = databaseClient
      .sql("update $TABLE_FILE set ${conditions.joinToString(", ")} where id = :id")
      .bind("id", id)
    params.forEach { spec = spec.bind(it.key, it.value)  }
    return spec.fetch().rowsUpdated().map { it > 0 }
  }
}