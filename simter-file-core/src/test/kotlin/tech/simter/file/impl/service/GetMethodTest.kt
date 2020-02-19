package tech.simter.file.impl.service

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.test.test
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.AttachmentService
import tech.simter.file.impl.TestHelper.randomAttachment
import tech.simter.file.impl.UnitTestConfiguration
import tech.simter.file.impl.service.AttachmentServiceImpl.OperationType.Read

/**
 * Test [AttachmentService.get]
 *
 * @author zh
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class GetMethodTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val service: AttachmentServiceImpl
) {
  @Test
  fun get() {
    // mock
    val attachment = randomAttachment()
    val id = attachment.id
    val puid = attachment.puid
    every { dao.get(id) } returns attachment.toMono()
    every { service.verifyAuthorize(puid, Read) } returns Mono.empty()

    // invoke
    val actual = service.get(id)

    // verify
    actual.test().expectNext(attachment).verifyComplete()
    verify {
      dao.get(id)
      service.verifyAuthorize(puid, Read)
    }
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val attachment = randomAttachment()
    val id = attachment.id
    val puid = attachment.puid
    every { dao.get(id) } returns attachment.toMono()
    every { service.verifyAuthorize(puid, Read) } returns Mono.error(PermissionDeniedException())

    // invoke
    val actual = service.get(id)

    // verify
    actual.test().verifyError(PermissionDeniedException::class.java)
    verify {
      dao.get(id)
      service.verifyAuthorize(puid, Read)
    }
  }
}