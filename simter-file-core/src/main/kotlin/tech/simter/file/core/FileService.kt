package tech.simter.file.core

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.kotlin.data.Page
import java.util.*

/**
 * The File Service Interface.
 *
 * @author RJ
 */
interface FileService {
  /**
   * Load the specific file page.
   *
   * @param[moduleMatcher] the module matcher
   * @param[search] the fuzzy search value for file's name
   * @param[offset] the page zero-base start point, must >= 0
   * @param[limit] the max page size. The empty value means use the default
   *               value of property `simter-file.default-find-page-limit`.
   */
  fun findPage(
    moduleMatcher: ModuleMatcher,
    search: Optional<String> = Optional.empty(),
    limit: Optional<Int> = Optional.empty(),
    offset: Optional<Long> = Optional.empty()
  ): Mono<Page<FileStore>>

  /**
   * Find all match files.
   *
   * @param[moduleMatcher] the module matcher
   * @param[search] the fuzzy search value for file's name
   * @param[limit] the max return size for avoid too many items to lowdown performance.
   *               The empty value means use the default value of property `simter-file.default-find-list-limit`
   */
  fun findList(
    moduleMatcher: ModuleMatcher,
    search: Optional<String> = Optional.empty(),
    limit: Optional<Int> = Optional.empty()
  ): Flux<FileStore>

  /** Upload one file from [source] with specific [describer] */
  fun upload(describer: FileDescriber, source: FileUploadSource): Mono<String>

  /** Update the specific [id] file with specific [describer] and optional [source] */
  fun update(
    id: String,
    describer: FileUpdateDescriber,
    source: Optional<FileUploadSource> = Optional.empty()
  ): Mono<Void>

  /**
   * Get the specific [id] file download information.
   *
   * Return [Mono.empty] if the file is not exists.
   */
  fun download(id: String): Mono<FileDownload>

  /**
   * Delete the specific [ids] files.
   *
   * Return the really deleted files count otherwise return 0 if no files deleted.
   */
  fun delete(vararg ids: String): Mono<Int>

  /**
   * Delete files match the specific [moduleMatcher].
   *
   * Return the really deleted files count otherwise return 0 if no files deleted.
   */
  fun delete(moduleMatcher: ModuleMatcher): Mono<Int>
}