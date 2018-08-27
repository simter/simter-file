package tech.simter.file.dao.reactive.mongo

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.po.Attachment
import java.time.OffsetDateTime
import java.util.*
import java.util.stream.IntStream

/**
 * @author cjw
 * @author RJ
 */
@SpringJUnitConfig(ModuleConfiguration::class)
@DataMongoTest
class AttachmentDaoImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val operations: ReactiveMongoOperations
) {
  private val path = "/data"
  private val uploader = "Simter"
  private val now = OffsetDateTime.now()

  @BeforeEach
  fun setup() {
    // drop and create a new collection
    StepVerifier.create(
      operations.collectionExists(Attachment::class.java)
        .flatMap { if (it) operations.dropCollection(Attachment::class.java) else Mono.just(it) }
        .then(operations.createCollection(Attachment::class.java))
    ).expectNextCount(1).verifyComplete()
  }

  @Test
  fun get() {
    // verify not exists
    val id = UUID.randomUUID().toString()
    StepVerifier.create(dao.get(id)).expectNextCount(0L).verifyComplete()

    // prepare data
    val po = Attachment(id, path, "Sample", "png", 123, now, uploader)
    StepVerifier.create(operations.insert(po)).expectNextCount(1).verifyComplete()

    // verify exists
    StepVerifier.create(dao.get(id)).expectNext(po).verifyComplete()
  }

  @Test
  fun findByPageable() {
    val pageable = PageRequest.of(0, 25)

    // 1. not found
    StepVerifier.create(dao.find(pageable))
      .expectNext(Page.empty<Attachment>(pageable))
      .verifyComplete()

    // 2. found
    // 2.1 prepare data
    val pos = (1..3).map { Attachment(it.toString(), path, "Sample$it", "png", 123, now, uploader) }
    StepVerifier.create(operations.insertAll(pos)).expectNextCount(pos.size.toLong()).verifyComplete()

    // 2.2 invoke
    val actual = dao.find(pageable)

    // 2.3 verify
    StepVerifier.create(actual)
      .consumeNextWith { page -> assertEquals(pos.size, page.content.size) }
      .verifyComplete()
  }

  @Test
  fun findByModuleAndSubgroup() {
    // 1. mock
    val puid = "puid1"
    val subgroup: Short = 1
    val now = OffsetDateTime.now()
    val origin = (1..3).map {
      Attachment(id = it.toString(), path = path, name = "Sample$it", ext = "png", size = 123,
        uploadOn = now, uploader = uploader, puid = puid, subgroup = it.toShort())
    }

    // 2. not found: empty list
    StepVerifier.create(dao.find(puid, subgroup).collectList())
      .consumeNextWith { Assertions.assertTrue(it.isEmpty()) }
      .verifyComplete()

    // 3. init data
    StepVerifier.create(operations.insertAll(origin)).expectNextCount(origin.size.toLong()).verifyComplete()

    // 4. found all data in module
    StepVerifier.create(dao.find(puid, null).collectList())
      .consumeNextWith { actual ->
        assertEquals(actual.size, origin.size)
        IntStream.range(0, actual.size).forEach {
          assertEquals(actual[it].id, origin[it].id)
        }
      }.verifyComplete()

    // 5. found all data in module and subgroup
    StepVerifier.create(dao.find(puid, subgroup).collectList())
      .consumeNextWith {
        assertEquals(it[0].id, origin.find { it.puid == puid && it.subgroup == subgroup }?.id)
      }.verifyComplete()
  }

  @Test
  fun saveOne() {
    val po = Attachment(UUID.randomUUID().toString(), path, "Sample", "png", 123, now, uploader)
    val actual = dao.save(po)

    // verify result
    StepVerifier.create(actual).expectNextCount(0L).verifyComplete()

    // verify saved
    StepVerifier.create(operations.findById(po.id, Attachment::class.java))
      .expectNext(po)
      .verifyComplete()
  }
}