package tech.simter.file.impl.dao.r2dbc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.domain.Attachment
import tech.simter.file.impl.dao.r2dbc.TestHelper.cleanDatabase
import tech.simter.file.impl.dao.r2dbc.po.AttachmentPo
import tech.simter.file.test.TestHelper.randomAttachmentId
import tech.simter.file.test.TestHelper.randomString
import java.time.OffsetDateTime
import java.util.stream.IntStream

/**
 * Test [AttachmentDaoImpl.find].
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class FindListMethodImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val repository: AttachmentRepository
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
    dao.find(puid = puid, upperId = subgroup)
      .test()
      .expectNext(origin.first { it.puid == puid && it.upperId == subgroup })
      .verifyComplete()
  }
}