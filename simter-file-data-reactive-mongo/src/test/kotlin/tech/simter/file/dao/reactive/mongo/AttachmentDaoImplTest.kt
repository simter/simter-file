package tech.simter.file.dao.reactive.mongo

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.util.FileCopyUtils
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.po.Attachment
import java.io.File
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.stream.IntStream

/**
 * @author cjw
 * @author RJ
 */
@SpringJUnitConfig(ModuleConfiguration::class)
@DataMongoTest
@TestPropertySource(properties = ["simter.file.root=target/files"])
class AttachmentDaoImplTest @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val dao: AttachmentDao,
  private val operations: ReactiveMongoOperations
) {
  private val path = "/data"
  private val creator = "Simter"
  private val now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)

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
    val po = Attachment(id, path, "Sample", "png", 123, now, creator, now, creator)
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
    val pos = (1..3).map {
      Attachment(it.toString(), path, "Sample$it", "png", 123, now, creator, now, creator)
    }
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
    val subgroup = "1"
    val now = OffsetDateTime.now()
    val origin = (1..3).map {
      Attachment(id = it.toString(), path = path, name = "Sample$it", type = "png", size = 123,
        createOn = now, creator = creator, puid = puid, upperId = it.toString(), modifyOn = now, modifier = creator
      )
    }
    Collections.reverse(origin)

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

    // 5. found all data in module and upperId
    StepVerifier.create(dao.find(puid, subgroup).collectList())
      .consumeNextWith {
        assertEquals(it[0].id, origin.find { it.puid == puid && it.upperId == subgroup }?.id)
      }.verifyComplete()
  }

  @Test
  fun saveOne() {
    val po = Attachment(UUID.randomUUID().toString(), path, "Sample", "png",
      123, now, creator, now, creator)
    val actual = dao.save(po)

    // verify result
    StepVerifier.create(actual).expectNextCount(0L).verifyComplete()

    // verify saved
    StepVerifier.create(operations.findById(po.id, Attachment::class.java))
      .expectNext(po)
      .verifyComplete()
  }

  @Test
  fun delete() {
    // 1. none
    StepVerifier.create(dao.delete()).expectNextCount(0L).verifyComplete()

    // 2. delete not exists id
    StepVerifier.create(dao.delete(UUID.randomUUID().toString())).expectNextCount(0L).verifyComplete()

    // 3. delete exists id
    // 3.1 prepare data
    val origin = (0..3).map {
      Attachment(id = UUID.randomUUID().toString(), path = "$path/$it.xml", name = "Sample$it", type = "png", size = 123,
        createOn = now, creator = creator, puid = "puid1", upperId = it.toString(), modifier = creator, modifyOn = now)
    }
    val ids = origin.map { it.id }
    StepVerifier.create(operations.insertAll(origin)).expectNextCount(origin.size.toLong()).verifyComplete()
    buildTestFiles(origin)

    // 3.2 verify attachments is deleted
    StepVerifier.create(dao.delete(*ids.toTypedArray())).verifyComplete()

    // 3.3 verify physics files is deleted
    origin.forEach { Assertions.assertTrue(!File("$fileRootDir/${it.path}").exists()) }
  }

  @Test
  fun notFoundFullPath() {
    // invoke and verify
    StepVerifier.create(dao.getFullPath(UUID.randomUUID().toString())).verifyComplete()
  }

  @Test
  fun getFullPath() {
    // prepare data
    val now = OffsetDateTime.now()
    val po1 = Attachment(id = UUID.randomUUID().toString(), path = "data-1", name = "Sample-1", type = ":d", size = 123,
      createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = null)
    val po2 = Attachment(id = UUID.randomUUID().toString(), path = "data-2", name = "Sample-2", type = ":d", size = 123,
      createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = po1.id)
    val po3 = Attachment(id = UUID.randomUUID().toString(), path = "data-3", name = "Sample-3", type = ":d", size = 123,
      createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = po2.id)
    val pos = listOf(po1, po2, po3)
    StepVerifier.create(operations.insertAll(pos)).expectNextCount(3).verifyComplete()

    // invoke and verify
    StepVerifier.create(dao.getFullPath(po3.id))
      .expectNext(pos.joinToString("/") { it.path }).verifyComplete()
  }

  /** build test file method */
  private fun buildTestFiles(attachments: List<Attachment>) {
    attachments.forEach {
      val file = File("$fileRootDir/${it.path}")
      val parentFile = file.parentFile
      if (!parentFile.exists()) parentFile.mkdirs()
      FileCopyUtils.copy(ClassPathResource("banner.txt").file.readBytes(), file)
    }
  }
}