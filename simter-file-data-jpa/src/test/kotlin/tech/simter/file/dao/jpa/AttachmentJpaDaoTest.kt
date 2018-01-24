package tech.simter.file.dao.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import tech.simter.file.po.Attachment
import java.time.OffsetDateTime
import java.util.*
import javax.persistence.EntityManager

/**
 * @author RJ
 */
@SpringJUnitConfig(classes = [JpaConfiguration::class])
@DataJpaTest
class AttachmentJpaDaoTest @Autowired constructor(val dao: AttachmentJpaDao, val em: EntityManager) {
  @Test
  fun test() {
    val attachment = Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png", 123, OffsetDateTime.now(), "Simter")
    dao.save(attachment)
    assertThat(
      em.createQuery("select name from Attachment where id = :id")
        .setParameter("id", attachment.id).singleResult
    ).isEqualTo(attachment.name)
  }
}