package tech.simter.file.impl.dao.mongo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.domain.Attachment
import tech.simter.file.impl.dao.mongo.TestHelper.cleanDatabase
import tech.simter.file.impl.dao.mongo.po.AttachmentPo
import tech.simter.file.test.TestHelper.randomAttachmentId
import tech.simter.file.test.TestHelper.randomString
import java.time.OffsetDateTime
import java.util.stream.IntStream

/**
 * Test [AttachmentDaoImpl.find].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@DataMongoTest
class FindListMethodImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val repository: AttachmentReactiveRepository
) {
  @Test
  fun `found nothing`() {
    dao.find(puid = randomString(), upperId = null)
      .test()
      .verifyComplete()
  }

  @Test
  fun `found something`() {
    // clean
    cleanDatabase(repository)

    // mock
    val puid = "puid1"
    val subgroup = "1"
    val now = OffsetDateTime.now()
    val origin = ArrayList<Attachment>()

    // init data
    IntStream.range(0, 5).forEach {
      val po = AttachmentPo(id = randomAttachmentId(), path = "/data", name = "Sample", type = "png", size = 123,
        createOn = now.minusDays(it.toLong()), creator = "Simter", puid = "puid1", upperId = it.toString(),
        modifyOn = now.minusDays(it.toLong()), modifier = "Simter")
      repository.save(po).test().expectNextCount(1).verifyComplete()
      origin.add(po)
    }
    origin.sortBy { it.createOn }
    origin.reverse()

    // 1. found all data by module
    dao.find(puid = puid, upperId = null).collectList()
      .test()
      .consumeNextWith { actual ->
        assertEquals(actual.size, origin.size)
        IntStream.range(0, actual.size).forEach {
          assertEquals(actual[it].id, origin[it].id)
        }
      }.verifyComplete()

    // 2. found all data by module and upperId
    dao.find(puid = puid, upperId = subgroup).collectList()
      .test()
      .consumeNextWith { list ->
        assertEquals(list[0].id, origin.find { it.puid == puid && it.upperId == subgroup }?.id)
      }.verifyComplete()
  }
}