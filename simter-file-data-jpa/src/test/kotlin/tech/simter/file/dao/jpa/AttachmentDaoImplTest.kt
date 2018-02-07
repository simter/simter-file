package tech.simter.file.dao.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.test.StepVerifier
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.po.Attachment
import java.time.OffsetDateTime
import java.util.*
import javax.persistence.EntityManager

/**
 * @author RJ
 */
@SpringJUnitConfig(classes = [JpaConfiguration::class])
@DataJpaTest
class AttachmentDaoImplTest @Autowired constructor(
  @Qualifier("attachmentDaoImpl") val dao: AttachmentDao,
  val em: EntityManager
) {
  @Test
  fun save() {
    val attachment = Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png", 123, OffsetDateTime.now(), "Simter")
    val save = dao.save(attachment)

    // verify return value
    StepVerifier.create(save)
      .expectNext(attachment)
      .verifyComplete()

    // verify persistence
    assertThat(
      em.createQuery("select a from Attachment a where id = :id")
        .setParameter("id", attachment.id).singleResult
    ).isEqualTo(attachment)
  }

  @Test
  fun findById() {
    // prepare data
    val attachment = Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png", 123, OffsetDateTime.now(), "Simter")
    em.persist(attachment)
    em.flush()
    em.clear()

    // verify exists
    StepVerifier.create(dao.findById(attachment.id))
      .expectNext(attachment)
      .verifyComplete()

    // verify not exists
    StepVerifier.create(dao.findById(UUID.randomUUID().toString()))
      .verifyComplete()
  }

  @Test
  fun findAll() {
    // verify empty page
    var page = dao.findAll(PageRequest.of(0, 25)).block()
    assertNotNull(page)
    assertTrue(page!!.content.isEmpty())
    assertEquals(0, page.number)
    assertEquals(25, page.size)
    assertEquals(0, page.totalPages)
    assertEquals(0, page.totalElements)

    // prepare data
    val now = OffsetDateTime.now()
    val attachment1 = Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png", 123, now.minusDays(1), "Simter")
    val attachment2 = Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png", 123, now, "Simter")
    em.persist(attachment1)
    em.persist(attachment2)
    em.flush()
    em.clear()

    // verify page with items
    page = dao.findAll(PageRequest.of(0, 25, Sort.by(DESC, "uploadOn"))).block()
    assertNotNull(page)
    assertEquals(0, page!!.number)
    assertEquals(25, page.size)
    assertEquals(1, page.totalPages)
    assertEquals(2L, page.totalElements)
    assertEquals(2, page.content.size)
    assertEquals(attachment2, page.content[0])
    assertEquals(attachment1, page.content[1])
  }
}