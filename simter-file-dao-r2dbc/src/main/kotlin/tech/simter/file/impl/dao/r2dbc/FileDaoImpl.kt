package tech.simter.file.impl.dao.r2dbc

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.core.DatabaseClient
import org.springframework.data.r2dbc.query.Criteria.where
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.file.TABLE_FILE
import tech.simter.file.core.FileDao
import tech.simter.file.core.FileStore
import tech.simter.file.core.ModuleMatcher
import tech.simter.file.core.ModuleMatcher.ModuleEquals
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
  private val databaseClient: DatabaseClient
) : FileDao {
  override fun findPage(
    moduleMatcher: ModuleMatcher,
    search: Optional<String>,
    limit: Int,
    offset: Int
  ): Mono<Page<FileStore>> {
    val params = mutableMapOf<String, Any>()
    val conditions = mutableListOf<String>()

    // module condition
    var criteria = when (moduleMatcher) {
      is ModuleEquals -> {
        conditions.add("module = :module")
        params["module"] = moduleMatcher.module

        where("module").`is`(moduleMatcher.module)
      }
      else -> {
        conditions.add("module like :module")
        val value = if (moduleMatcher.module.endsWith("%")) moduleMatcher.module
        else moduleMatcher.module + "%"
        params["module"] = value

        where("module").like(value)
      }
    }

    // search condition
    search.ifPresent {
      val value = if (it.startsWith("%")) {
        if (it.endsWith("%")) it
        else "$it%"
      } else {
        if (it.endsWith("%")) "%$it"
        else "%$it%"
      }
      conditions.add("name like :search")
      params["search"] = value

      criteria = criteria.and("name").like(value)
    }

    val rowsQuery = databaseClient.select()
      .from(TABLE_FILE)
      .matching(criteria)
      .`as`(FileStore.Impl::class.java)
      .orderBy(Sort.by(Sort.Direction.DESC, "create_on"))
      .page(PageRequest.of(offset, limit))

    var countSpec = databaseClient.execute(
      "select count(*) from $TABLE_FILE" +
        " where ${conditions.joinToString(" and ")}"
    )
    params.forEach { countSpec = countSpec.bind(it.key, it.value) }
    val countQuery = countSpec.map { row -> row[0] as Long }

    return rowsQuery.fetch().all().collectList()
      .flatMap { files ->
        countQuery.one().map { total ->
          Page.of(rows = files as List<FileStore>, total = total, offset = offset, limit = limit)
        }
      }
  }

  @Suppress("UNCHECKED_CAST")
  override fun findList(
    moduleMatcher: ModuleMatcher,
    search: Optional<String>,
    limit: Optional<Int>
  ): Flux<FileStore> {
    var criteria = when (moduleMatcher) {
      is ModuleEquals -> where("module").`is`(moduleMatcher.module)
      else -> where("module").like(
        if (moduleMatcher.module.endsWith("%")) moduleMatcher.module
        else moduleMatcher.module + "%"
      )
    }
    search.ifPresent {
      criteria = criteria.and("name").like(
        if (it.startsWith("%")) {
          if (it.endsWith("%")) it
          else "$it%"
        } else {
          if (it.endsWith("%")) "%$it"
          else "%$it%"
        }
      )
    }
    var spec = databaseClient.select()
      .from(TABLE_FILE)
      .matching(criteria)
      .`as`(FileStore.Impl::class.java)
      .orderBy(Sort.by(Sort.Direction.DESC, "create_on"))
    limit.ifPresent { spec = spec.page(PageRequest.of(0, limit.get())) }
    return spec.fetch().all() as Flux<FileStore>
  }

  override fun create(file: FileStore): Mono<String> {
    return databaseClient.insert()
      .into(TABLE_FILE)
      .value("module", standardModuleValue(file.module))
      .value("name", file.name)
      .value("type", file.type)
      .value("size", file.size)
      .value("path", file.path)
      .value("creator", file.creator)
      .value("create_on", file.createOn)
      .value("modifier", file.modifier)
      .value("modify_on", file.modifyOn)
      .value("id", if (file.id.isNotEmpty()) file.id else TODO("Not implemented for auto generate id by database"))
      .fetch()
      .rowsUpdated()
      .map { file.id }
  }

  @Suppress("UNCHECKED_CAST")
  override fun get(id: String): Mono<FileStore> {
    return databaseClient.select()
      .from(TABLE_FILE)
      .matching(where("id").`is`(id))
      .`as`(FileStore.Impl::class.java)
      .fetch()
      .one() as Mono<FileStore>
  }
}