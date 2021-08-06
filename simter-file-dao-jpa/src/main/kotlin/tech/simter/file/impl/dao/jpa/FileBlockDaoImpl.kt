package tech.simter.file.impl.dao.jpa

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import tech.simter.file.TABLE_FILE
import tech.simter.file.core.FileStore
import tech.simter.file.core.FileUpdateDescriber
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
    offset: Long
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
      .setFirstResult(offset.toInt())
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

  @Transactional(readOnly = true)
  override fun findById(vararg ids: String): List<FileStore> {
    return repository.findAllById(ids.toList())
  }

  @Transactional(readOnly = false)
  override fun delete(vararg ids: String): Int {
    val sql = "delete from $TABLE_FILE where id in (:ids)"
    return em.createNativeQuery(sql).setParameter("ids", ids.toList()).executeUpdate()
  }

  @Transactional(readOnly = false)
  override fun delete(moduleMatcher: ModuleMatcher): Int {
    val param: String
    val condition: String

    // module condition
    when (moduleMatcher) {
      is ModuleEquals -> {
        condition = "module = :module"
        param = moduleMatcher.module
      }
      else -> {
        condition = "module like :module"
        val value = if (moduleMatcher.module.endsWith("%")) moduleMatcher.module
        else moduleMatcher.module + "%"
        param = value
      }
    }

    val sql = "delete from $TABLE_FILE where $condition"

    return em.createNativeQuery(sql).setParameter("module", param).executeUpdate()
  }

  @Transactional(readOnly = false)
  override fun update(id: String, info: FileUpdateDescriber): Boolean {
    val conditions = mutableListOf<String>()
    val params = mutableMapOf<String, Any>()
    info.module.ifPresent { conditions.add("module = :module"); params["module"] = it; }
    info.name.ifPresent { conditions.add("name = :name"); params["name"] = it; }
    info.type.ifPresent { conditions.add("type = :type"); params["type"] = it; }
    info.size.ifPresent { conditions.add("size = :size"); params["size"] = it; }
    info.path.ifPresent { conditions.add("path = :path"); params["path"] = it; }
    info.modifier.ifPresent { conditions.add("modifier = :modifier"); params["modifier"] = it; }
    info.modifyOn.ifPresent { conditions.add("modify_on = :modifyOn"); params["modifyOn"] = it; }
    if (conditions.isEmpty()) return false
    var spec = em.createNativeQuery("update $TABLE_FILE set ${conditions.joinToString(", ")} where id = :id")
      .setParameter("id", id)
    params.forEach { spec = spec.setParameter(it.key, it.value)  }
    return spec.executeUpdate() > 0
  }
}