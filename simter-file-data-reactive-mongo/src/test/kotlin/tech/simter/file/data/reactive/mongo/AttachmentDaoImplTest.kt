package tech.simter.file.data.reactive.mongo

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.test.StepVerifier
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.po.Attachment
import java.time.OffsetDateTime
import java.util.*

/**
 * See [SimpleReactiveMongoRepository] implementation.
 * @author RJ
 */
@SpringJUnitConfig(classes = [MongoConfiguration::class])
@DataMongoTest
class AttachmentDaoImplTest @Autowired constructor(
  private val dao: AttachmentDao
) {
  @Test
  fun save() {
    // data
    val attachment = Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png", 123, OffsetDateTime.now(), "Simter")

    // invoke
    val save = dao.save(attachment)

    // verify
    StepVerifier.create(save)
      .expectNext(attachment)
      .verifyComplete()
  }
}