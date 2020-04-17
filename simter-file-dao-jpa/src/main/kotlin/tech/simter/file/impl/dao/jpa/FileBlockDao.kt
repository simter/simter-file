package tech.simter.file.impl.dao.jpa

import tech.simter.file.core.FileStore
import tech.simter.file.core.ModuleMatcher
import tech.simter.kotlin.data.Page
import java.util.*

/**
 * The [FileStore] block Dao Interface.
 *
 * @author RJ
 */
interface FileBlockDao {
  /**
   * Load the specific file page.
   *
   * @param[moduleMatcher] the module matcher
   * @param[search] the fuzzy search value for file's name
   * @param[offset] the page zero-base start point, must >= 0
   * @param[limit] the max page size, must > 0
   */
  fun findPage(
    moduleMatcher: ModuleMatcher,
    search: Optional<String> = Optional.empty(),
    limit: Int,
    offset: Int = 0
  ): Page<FileStore>

  /**
   * Find all match files.
   *
   * @param[moduleMatcher] the module matcher
   * @param[search] the fuzzy search value for file's name
   * @param[limit] the max return size for avoid too many items to lowdown performance,
   *               empty value means no limits, none-empty value must > 0
   */
  fun findList(
    moduleMatcher: ModuleMatcher,
    search: Optional<String> = Optional.empty(),
    limit: Optional<Int> = Optional.empty()
  ): List<FileStore>

  /**
   * Persistence a new file store information.
   *
   * Return the id.
   */
  fun create(file: FileStore): String

  /**
   * load the specific [id] [FileStore] instance.
   *
   * Return [Optional.empty] if it is not exists.
   */
  fun get(id: String): Optional<FileStore>
}