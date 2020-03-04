package tech.simter.file.impl.dao.jpa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.domain.Attachment
import tech.simter.file.impl.dao.jpa.po.AttachmentPo
import tech.simter.file.test.TestHelper.randomAttachmentId
import tech.simter.file.test.TestHelper.randomString
import tech.simter.reactive.test.jpa.ReactiveDataJpaTest
import tech.simter.reactive.test.jpa.TestEntityManager
import java.time.OffsetDateTime
import java.util.stream.IntStream

/**
 * Test [AttachmentDao.find].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@ReactiveDataJpaTest
class FindListMethodImplTest @Autowired constructor(
  val rem: TestEntityManager,
  val dao: AttachmentDao
) {
  @Test
  fun `found nothing`() {
    // clean
    rem.executeUpdate { it.createQuery("delete from AttachmentPo") }

    // invoke
    dao.find(puid = randomString(), upperId = null)
      .test()
      .verifyComplete()
  }

  @Test
  fun `found something`() {
    // clean
    rem.executeUpdate { it.createQuery("delete from AttachmentPo") }

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
      rem.persist(po)
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