package tech.simter.file.dao.jpa

import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.po.Attachment
import java.util.concurrent.Callable


/**
 * The JPA implementation of [AttachmentDao].
 *
 * @author RJ
 */
@Component
class AttachmentDaoImpl @Autowired constructor(
  val jpaDao: AttachmentJpaDao,
  @Qualifier("jpaScheduler") val scheduler: Scheduler
) : AttachmentDao {
  private fun <T> async(callable: Callable<T>): Mono<T> {
    return Mono.fromCallable(callable).publishOn(scheduler)
  }

  override fun <S : Attachment?> save(entity: S): Mono<S> {
    return async(Callable(function = { jpaDao.save(entity) }))
  }

  override fun findAll(): Flux<Attachment> {
    TODO("not implemented")
  }

  override fun deleteById(id: String?): Mono<Void> {
    TODO("not implemented")
  }

  override fun deleteById(id: Publisher<String>?): Mono<Void> {
    TODO("not implemented")
  }

  override fun deleteAll(entities: MutableIterable<Attachment>?): Mono<Void> {
    TODO("not implemented")
  }

  override fun deleteAll(entityStream: Publisher<out Attachment>?): Mono<Void> {
    TODO("not implemented")
  }

  override fun deleteAll(): Mono<Void> {
    TODO("not implemented")
  }

  override fun <S : Attachment?> saveAll(entities: MutableIterable<S>?): Flux<S> {
    TODO("not implemented")
  }

  override fun <S : Attachment?> saveAll(entityStream: Publisher<S>?): Flux<S> {
    TODO("not implemented")
  }

  override fun count(): Mono<Long> {
    TODO("not implemented")
  }

  override fun findAllById(ids: MutableIterable<String>?): Flux<Attachment> {
    TODO("not implemented")
  }

  override fun findAllById(idStream: Publisher<String>?): Flux<Attachment> {
    TODO("not implemented")
  }

  override fun existsById(id: String?): Mono<Boolean> {
    TODO("not implemented")
  }

  override fun existsById(id: Publisher<String>?): Mono<Boolean> {
    TODO("not implemented")
  }

  override fun findById(id: String?): Mono<Attachment> {
    TODO("not implemented")
  }

  override fun findById(id: Publisher<String>?): Mono<Attachment> {
    TODO("not implemented")
  }

  override fun delete(entity: Attachment?): Mono<Void> {
    TODO("not implemented")
  }
}