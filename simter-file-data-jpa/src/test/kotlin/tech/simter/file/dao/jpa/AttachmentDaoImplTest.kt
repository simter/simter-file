package tech.simter.file.dao.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
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
}