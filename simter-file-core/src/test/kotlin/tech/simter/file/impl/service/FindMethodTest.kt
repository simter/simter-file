package tech.simter.file.impl.service

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.AttachmentService
import tech.simter.file.impl.UnitTestConfiguration
import tech.simter.file.impl.service.AttachmentServiceImpl.OperationType.Read

/**
 * Test [AttachmentService.find]
 *
 * @author zh
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class FindMethodTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val service: AttachmentServiceImpl
) {
  @Test
  fun find() {
    // mock
    val pageable = PageRequest.of(1, 25)
    every { service.verifyAuthorize("admin", Read) } returns Mono.empty()
    every { dao.find(pageable) } returns Mono.empty()

    // invoke
    val actual = service.find(pageable)

    // verify
    actual.test().verifyComplete()
    verify {
      service.verifyAuthorize("admin", Read)
      dao.find(pageable)
    }
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val pageable = PageRequest.of(1, 25)
    every { service.verifyAuthorize("admin", Read) } returns Mono.error(PermissionDeniedException())

    // invoke
    val actual = service.find(pageable)

    // verify
    actual.test().verifyError(PermissionDeniedException::class.java)
    verify { service.verifyAuthorize("admin", Read) }
  }
}