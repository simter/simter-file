package tech.simter.file.dao.reactive.mongo

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.dto.AttachmentDto4FullPath
import tech.simter.file.po.Attachment
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * @author RJ
 */
@SpringJUnitConfig(ModuleConfiguration::class)
@DataMongoTest
@TestPropertySource(properties = ["simter.file.root=target/files"])
class RecursiveFindTest @Autowired constructor(
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
  fun test() {
    // prepare data
    val po = Attachment(id = UUID.randomUUID().toString(), path = "path0", name = "Sample", type = "png", size = 123, createOn = now, creator = creator, modifyOn = now, modifier = creator)
    val po1 = po.copy(id = "1", path = "p1", type = ":d")
    val po11 = po.copy(id = "1-1", path = "p1-1", type = ":d", upperId = po1.id)
    val po111 = po.copy(id = "1-1-1", path = "p1-1-1", type = "png", upperId = po11.id)
    val po112 = po.copy(id = "1-1-2", path = "p1-1-2", type = "png", upperId = po11.id)
    StepVerifier.create(operations.insert(po1)).expectNextCount(1).verifyComplete()
    StepVerifier.create(operations.insert(po11)).expectNextCount(1).verifyComplete()
    StepVerifier.create(operations.insert(po111)).expectNextCount(1).verifyComplete()
    StepVerifier.create(operations.insert(po112)).expectNextCount(1).verifyComplete()

    StepVerifier.create(
      operations.aggregate(
        newAggregation(
          match(Criteria.where("id").`in`(po111.id)),
          graphLookup("st_attachment")
            .startWith("upperId")
            .connectFrom("upperId")
            .connectTo("_id")
            .`as`("uppers"),
          project("path", "uppers")
        ),
        Attachment::class.java, AttachmentDto4FullPathGetter::class.java
      ).map { it.dto }.collectList()
    ).expectNext(
      listOf(AttachmentDto4FullPath().apply {
        this.id = po111.id
        this.fullPath = "p1/p1-1/p1-1-1"
      })
    ).verifyComplete()
  }
}

data class AttachmentDto4FullPathGetter(private val id: String?,
                                        private val path: String,
                                        private val uppers: List<UpperPath>?) {
  data class UpperPath(val path: String)

  val dto: AttachmentDto4FullPath
    get() {
      return AttachmentDto4FullPath().also { dto ->
        dto.id = id
        dto.fullPath = uppers?.let { it.joinToString(separator = "/", postfix = "/$path") { item -> item.path } }
          ?: path
      }
    }
}