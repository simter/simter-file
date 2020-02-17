package tech.simter.file.impl.service

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.AttachmentService
import tech.simter.file.impl.UnitTestConfiguration
import tech.simter.file.impl.service.AttachmentServiceImpl.OperationType.Read
import tech.simter.util.RandomUtils.randomString
import java.util.*

/**
 * Test [AttachmentService.findDescendants]
 *
 * @author zh
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class FindDescendantsMethodTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val service: AttachmentServiceImpl
) {
  @Test
  fun find() {
    // mock
    val id = randomString()
    val puid = randomString()
    every { service.verifyAuthorize(puid, Read) } returns Mono.empty()
    every { dao.findPuids(id) } returns Flux.just(Optional.ofNullable<String?>(puid))
    every { dao.findDescendants(id) } returns Flux.empty()

    // invoke
    val actual = service.findDescendants(id)

    // verify
    actual.test().verifyComplete()
    verify {
      service.verifyAuthorize(puid, Read)
      dao.findPuids(id)
      dao.findDescendants(id)
    }
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val id = randomString()
    val puid = randomString()
    every { service.verifyAuthorize(puid, Read) } returns Mono.error(PermissionDeniedException())
    every { dao.findPuids(id) } returns Flux.just(Optional.ofNullable<String?>(puid))

    // invoke
    val actual = service.findDescendants(id)

    // verify
    actual.test().verifyError(PermissionDeniedException::class.java)
    verify {
      service.verifyAuthorize(puid, Read)
      dao.findPuids(id)
    }
  }
}