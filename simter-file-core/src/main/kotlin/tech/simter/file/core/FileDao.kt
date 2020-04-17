package tech.simter.file.core

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.kotlin.data.Page
import java.util.*

/**
 * The common file dao.
 *
 * @author RJ
 */
interface FileDao {
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
  ): Mono<Page<FileStore>>

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
  ): Flux<FileStore>

  /**
   * Persistence a new file store information.
   *
   * Return the id.
   */
  fun create(file: FileStore): Mono<String>

  /**
   * load the specific [id] [FileStore] instance.
   *
   * Return [Mono.empty] if it is not exists.
   */
  fun get(id: String): Mono<FileStore>
}
