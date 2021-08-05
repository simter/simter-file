package tech.simter.file.impl.dao.jpa

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.file.core.*
import tech.simter.kotlin.data.Page
import tech.simter.reactive.jpa.ReactiveJpaWrapper
import java.util.*

/**
 * The JPA implementation of [FileDao].
 *
 * @author RJ
 */
@Repository
class FileDaoImpl @Autowired constructor(
  private val blockDao: FileBlockDao,
  private val wrapper: ReactiveJpaWrapper
) : FileDao {
  override fun findPage(
    moduleMatcher: ModuleMatcher,
    search: Optional<String>,
    limit: Int,
    offset: Long
  ): Mono<Page<FileStore>> {
    return wrapper.fromCallable {
      blockDao.findPage(
        moduleMatcher = moduleMatcher,
        search = search,
        limit = limit,
        offset = offset
      )
    }
  }

  override fun findList(
    moduleMatcher: ModuleMatcher,
    search: Optional<String>,
    limit: Optional<Int>
  ): Flux<FileStore> {
    return wrapper.fromIterable {
      blockDao.findList(
        moduleMatcher = moduleMatcher,
        search = search,
        limit = limit
      )
    }
  }

  override fun create(file: FileStore): Mono<String> {
    return wrapper.fromCallable { blockDao.create(file) }
  }

  override fun get(id: String): Mono<FileStore> {
    return wrapper.fromCallable { blockDao.get(id) }
      .flatMap { if (it.isPresent) Mono.just(it.get()) else Mono.empty() }
  }

  override fun findById(vararg ids: String): Flux<FileStore> {
    return wrapper.fromCallable { blockDao.findById(*ids) }
      .flatMapMany { if (it.isNotEmpty()) Flux.fromIterable(it) else Flux.empty() }
  }

  override fun delete(vararg ids: String): Mono<Int> {
    return wrapper.fromCallable { blockDao.delete(*ids) }
      .flatMap { Mono.just(it) }
  }

  override fun delete(moduleMatcher: ModuleMatcher): Mono<Int> {
    return wrapper.fromCallable { blockDao.delete(moduleMatcher) }
      .flatMap { Mono.just(it) }
  }

  override fun update(id: String, updateInfo: FileUpdateDescriber): Mono<Boolean> {
    return wrapper.fromCallable { blockDao.update(id, updateInfo) }
  }
}