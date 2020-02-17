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

/**
 * Test [AttachmentService.find]
 *
 * @author zh
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class FindByModuleAndUpperIdMethodTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val service: AttachmentServiceImpl
) {
  @Test
  fun find() {
    // mock
    val puid = "puid1"
    every { service.verifyAuthorize(puid, Read) } returns Mono.empty()
    every { dao.find(puid, null) } returns Flux.empty()

    // invoke
    val actual = service.find(puid, null)

    // verify
    actual.test().verifyComplete()
    verify {
      dao.find(puid, null)
      service.verifyAuthorize(puid, Read)
    }
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val puid = randomString()
    every { service.verifyAuthorize(puid, Read) } returns Mono.error(PermissionDeniedException())

    // invoke
    val actual = service.find(puid, null)

    // verify
    actual.test().verifyError(PermissionDeniedException::class.java)
    verify { service.verifyAuthorize(puid, Read) }
  }
}