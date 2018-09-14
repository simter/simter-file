package tech.simter.file.dao.jpa

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.util.FileCopyUtils
import reactor.test.StepVerifier
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.po.Attachment
import java.io.File
import java.time.OffsetDateTime
import java.util.*
import java.util.stream.IntStream
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.collections.ArrayList

/**
 * @author RJ
 */
@SpringJUnitConfig(ModuleConfiguration::class)
@DataJpaTest
@TestPropertySource(properties = ["simter.file.root=target/files"])
class AttachmentDaoImplTest @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  @PersistenceContext val em: EntityManager,
  val dao: AttachmentDao
) {
  @Test
  fun get() {
    // verify not exists
    StepVerifier.create(dao.get(UUID.randomUUID().toString()))
      .expectNextCount(0L)
      .verifyComplete()

    // prepare data
    val po = Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png", 123, OffsetDateTime.now(), "Simter")
    em.persist(po)
    em.flush()
    em.clear()

    // verify exists
    StepVerifier.create(dao.get(po.id))
      .expectNext(po)
      .verifyComplete()
  }

  @Test
  fun findByPageable() {
    // 1. not found: empty page
    StepVerifier.create(dao.find(PageRequest.of(0, 25)))
      .consumeNextWith { page ->
        assertTrue(page.content.isEmpty())
        assertEquals(0, page.number)
        assertEquals(25, page.size)
        assertEquals(0, page.totalPages)
        assertEquals(0, page.totalElements)
      }
      .verifyComplete()

    // 2. found: page with content
    // 2.1 prepare data
    val now = OffsetDateTime.now()
    val po1 = Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png", 123, now.minusDays(1), "Simter")
    val po2 = Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png", 123, now, "Simter")
    em.persist(po1)
    em.persist(po2)
    em.flush()
    em.clear()

    // 2.2 invoke
    val actual = dao.find(PageRequest.of(0, 25, Sort.by(DESC, "uploadOn")))

    // 2.3 verify
    StepVerifier.create(actual)
      .consumeNextWith { page ->
        assertEquals(0, page.number)
        assertEquals(25, page.size)
        assertEquals(1, page.totalPages)
        assertEquals(2L, page.totalElements)
        assertEquals(2, page.content.size)
        assertEquals(po2, page.content[0])
        assertEquals(po1, page.content[1])
      }
      .verifyComplete()
  }

  @Test
  fun findByModuleAndSubgroup() {
    // 1. mock
    val puid = "puid1"
    val subgroup: Short = 1
    val now = OffsetDateTime.now()
    val origin = ArrayList<Attachment>()

    // 2. not found: empty list
    StepVerifier.create(dao.find(puid, subgroup).collectList())
      .consumeNextWith { assertTrue(it.isEmpty()) }
      .verifyComplete()

    // 3. init data
    IntStream.range(0, 5).forEach {
      val po = Attachment(id = UUID.randomUUID().toString(), path = "/data", name = "Sample", ext = "png", size = 123,
        uploadOn = now.minusDays(it.toLong()), uploader = "Simter", puid = "puid1", subgroup = it.toShort())
      em.persist(po)
      origin.add(po)
    }
    em.flush()
    em.clear()
    origin.sortBy { it.uploadOn }
    origin.reverse()

    // 4. found all data by module
    StepVerifier.create(dao.find(puid, null).collectList())
      .consumeNextWith { actual ->
        assertEquals(actual.size, origin.size)
        IntStream.range(0, actual.size).forEach {
          assertEquals(actual[it].id, origin[it].id)
        }
      }.verifyComplete()

    // 5. found all data by module and subgroup
    StepVerifier.create(dao.find(puid, subgroup).collectList())
      .consumeNextWith {
        assertEquals(it[0].id, origin.find { it.puid == puid && it.subgroup == subgroup }?.id)
      }.verifyComplete()
  }

  @Test
  fun findByIds() {
    // 1. mock
    val ids = arrayOf("a0001", "a0002", "a0003", "a0004", "a0005")
    val now = OffsetDateTime.now()
    val origin = ArrayList<Attachment>()

    // 2. throw NPE: empty ids
    Assertions.assertThrows(NullPointerException::class.java, { dao.find().subscribe() })

    // 3. not found: empty list
    StepVerifier.create(dao.find(*ids).collectList())
      .consumeNextWith { assertTrue(it.isEmpty()) }
      .verifyComplete()

    // 4. init data
    IntStream.range(0, ids.size).forEach {
      val po = Attachment(id = ids[it], path = "/data", name = "Sample", ext = "png", size = 123,
        uploadOn = now.minusDays(it.toLong()), uploader = "Simter", puid = "puid1", subgroup = it.toShort())
      em.persist(po)
      origin.add(po)
    }
    em.flush()
    em.clear()

    // 5. found all data by ids
    StepVerifier.create(dao.find(*ids).collectList())
      .consumeNextWith { actual ->
        assertEquals(actual.size, origin.size)
        IntStream.range(0, actual.size).forEach {
          assertEquals(actual[it].id, origin[it].id)
        }
      }.verifyComplete()
  }

  @Test
  fun saveNone() {
    StepVerifier.create(dao.save()).expectNextCount(0L).verifyComplete()
  }

  @Test
  fun saveOne() {
    // invoke
    val po = Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png", 123, OffsetDateTime.now(), "Simter")
    val actual = dao.save(po)

    // verify result
    StepVerifier.create(actual).expectNextCount(0L).verifyComplete()

    // verify saved
    assertEquals(
      po,
      em.createQuery("select a from Attachment a where id = :id").setParameter("id", po.id).singleResult
    )
  }

  @Test
  fun saveMulti() {
    val pos = (1..3).map {
      Attachment(UUID.randomUUID().toString(), "/data", "Sample-$it", "png", 123, OffsetDateTime.now(), "Simter")
    }
    val actual = dao.save(*pos.toTypedArray())

    // verify result
    StepVerifier.create(actual).expectNextCount(0L).verifyComplete()

    // verify saved
    pos.forEach {
      assertEquals(
        it,
        em.createQuery("select a from Attachment a where id = :id", Attachment::class.java)
          .setParameter("id", it.id).singleResult
      )
    }
  }

  @Test
  fun delete() {
    // 1. none
    StepVerifier.create(dao.delete()).expectNextCount(0L).verifyComplete()

    // 2. delete not exists id
    StepVerifier.create(dao.delete(UUID.randomUUID().toString())).expectNextCount(0L).verifyComplete()

    // 3. delete exists id
    // 3.1 prepare data
    val pos = (1..3).map {
      Attachment(UUID.randomUUID().toString(), "/data/$it.xml", "Sample-$it", "png", 123,
        OffsetDateTime.now(), "Simter")
    }
    val ids = pos.map { it.id }
    pos.forEach { em.persist(it) }
    em.flush()
    em.clear()
    buildTestFiles(pos)

    // 3.2 verify attachments is deleted
    StepVerifier.create(dao.delete(*ids.toTypedArray())).verifyComplete()
    StepVerifier.create(dao.find(*ids.toTypedArray())).expectNextCount(0L).verifyComplete()

    // 3.3 verify physics files is deleted
    pos.forEach { assertTrue(!File("$fileRootDir/${it.path}").exists()) }
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