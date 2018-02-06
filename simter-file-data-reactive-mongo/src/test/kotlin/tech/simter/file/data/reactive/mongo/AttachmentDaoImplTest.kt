package tech.simter.file.data.reactive.mongo

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.createCollection
import org.springframework.data.mongodb.core.dropCollection
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.test.StepVerifier
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.po.Attachment
import java.time.OffsetDateTime

/**
 * See [SimpleReactiveMongoRepository] implementation.
 * @author RJ
 */
@SpringJUnitConfig(classes = [MongoConfiguration::class])
@DataMongoTest
class AttachmentDaoImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val operations: ReactiveMongoOperations
) {
  private val PATH = "/data"              // file save path
  private val UPLOADER = "Simter"         // file uploader
  private val NOW = OffsetDateTime.now()  // instance current date time by OffsetDateTime

  @Test
  fun get() {
    // init
    operations.dropCollection(Attachment::class).block()
    operations.createCollection(Attachment::class).block()

    val id = "1111"
    val po = Attachment(id, PATH, "Sample", "png", 123, NOW, UPLOADER)
    operations.save(po).block()

    // verify
    StepVerifier.create(dao.findById(id).map { it.id })
      .expectNext(id)
      .verifyComplete()
  }

  @Test
  fun save() {
    // init
    operations.dropCollection(Attachment::class).block()
    operations.createCollection(Attachment::class).block()
    val attachment = Attachment("1111", PATH, "Sample", "png", 123, NOW, UPLOADER)

    // verify
    StepVerifier.create(dao.save(attachment))
      .expectNext(attachment)
      .verifyComplete()
  }
}