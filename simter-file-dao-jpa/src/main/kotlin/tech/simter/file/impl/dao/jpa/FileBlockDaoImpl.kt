package tech.simter.file.impl.dao.jpa

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import tech.simter.file.TABLE_FILE
import tech.simter.file.core.FileStore
import tech.simter.file.core.ModuleMatcher
import tech.simter.file.core.ModuleMatcher.ModuleEquals
import tech.simter.file.impl.dao.jpa.po.FileStorePo
import tech.simter.kotlin.data.Page
import java.util.*
import javax.persistence.EntityManager

/**
 * The JPA implementation of [FileBlockDao].
 *
 * @author RJ
 */
@Repository
class FileBlockDaoImpl @Autowired constructor(
  private val em: EntityManager,
  private val repository: FileRepository
) : FileBlockDao {
  override fun findPage(
    moduleMatcher: ModuleMatcher,
    search: Optional<String>,
    limit: Int,
    offset: Int
  ): Page<FileStore> {
    val params = mutableMapOf<String, Any>()
    val conditions = mutableListOf<String>()

    // module condition
    when (moduleMatcher) {
      is ModuleEquals -> {
        conditions.add("module = :module")
        params["module"] = moduleMatcher.module
      }
      else -> {
        conditions.add("module like :module")
        val value = if (moduleMatcher.module.endsWith("%")) moduleMatcher.module
        else moduleMatcher.module + "%"
        params["module"] = value
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
    }

    val conditionSql = conditions.joinToString(" and ")
    val rowsSql = """
      select * from $TABLE_FILE
      where $conditionSql
      order by create_on desc
    """.trimIndent()
    val rowsQuery = em.createNativeQuery(rowsSql, FileStorePo::class.java)
      .setMaxResults(limit)
      .setFirstResult(offset)
    params.forEach { rowsQuery.setParameter(it.key, it.value) }

    val countSql = """
      select count(*) from $TABLE_FILE
      where $conditionSql
    """.trimIndent()
    val countQuery = em.createNativeQuery(countSql)
    params.forEach { countQuery.setParameter(it.key, it.value) }

    @Suppress("UNCHECKED_CAST")
    return Page.of(
      rows = rowsQuery.resultList as List<FileStore>,
      total = (countQuery.singleResult as Number).toLong(),
      offset = offset,
      limit = limit
    )
  }

  override fun findList(
    moduleMatcher: ModuleMatcher,
    search: Optional<String>,
    limit: Optional<Int>
  ): List<FileStore> {
    val params = mutableMapOf<String, Any>()
    val conditions = mutableListOf<String>()

    // module condition
    when (moduleMatcher) {
      is ModuleEquals -> {
        conditions.add("module = :module")
        params["module"] = moduleMatcher.module
      }
      else -> {
        conditions.add("module like :module")
        val value = if (moduleMatcher.module.endsWith("%")) moduleMatcher.module
        else moduleMatcher.module + "%"
        params["module"] = value
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
    }

    val sql = """
      select * from $TABLE_FILE
      where ${conditions.joinToString(" and ")}
      order by create_on desc
    """.trimIndent()
    val query = em.createNativeQuery(sql, FileStorePo::class.java)
    params.forEach { query.setParameter(it.key, it.value) }

    @Suppress("UNCHECKED_CAST")
    return (if (limit.isPresent) query.setMaxResults(limit.get()).resultList
    else query.resultList) as List<FileStore>
  }

  @Transactional(readOnly = false)
  override fun create(file: FileStore): String {
    // do not use 'repository.save(po)' because it will select it first.
    // directly use 'EntityManager.persist(po)'.
    em.persist(FileStorePo.from(file))
    return file.id
  }

  @Suppress("unchecked_cast")
  @Transactional(readOnly = true)
  override fun get(id: String): Optional<FileStore> {
    return repository.findById(id) as Optional<FileStore>
  }
}