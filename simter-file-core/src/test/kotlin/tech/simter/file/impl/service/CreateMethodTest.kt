package tech.simter.file.impl.service

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.test.test
import tech.simter.exception.ForbiddenException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.AttachmentService
import tech.simter.file.impl.TestHelper.randomAttachment
import tech.simter.file.impl.TestHelper.randomAuthenticatedUser
import tech.simter.file.impl.UnitTestConfiguration
import tech.simter.file.impl.service.AttachmentServiceImpl.OperationType.Create
import tech.simter.reactive.security.ReactiveSecurityService
import java.util.*

/**
 * Test [AttachmentService.create]
 *
 * @author zh
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class CreateMethodTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val service: AttachmentServiceImpl,
  private val securityService: ReactiveSecurityService
) {
  @Test
  fun success() {
    // mock
    val attachments = List(3) { randomAttachment().copy(puid = null) }
    val ids = attachments.map { it.id }
    val user = randomAuthenticatedUser()
    every { service.verifyAuthorize(null, Create) } returns Mono.empty()
    every { dao.save(*anyVararg()) } returns Mono.empty()
    every { securityService.getAuthenticatedUser() } returns Optional.of(user).toMono()

    // invoke
    val actual = service.create(*attachments.toTypedArray())

    // verify
    actual.collectList().test().expectNext(ids).verifyComplete()
    verify {
      dao.save(*attachments.map { it.copy(creator = user.name, modifier = user.name) }.toTypedArray())
      service.verifyAuthorize(null, Create)
      securityService.getAuthenticatedUser()
    }
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val attachments = List(3) { randomAttachment().copy(puid = null) }
    every { service.verifyAuthorize(null, Create) } returns Mono.error(PermissionDeniedException())

    // invoke
    val actual = service.create(*attachments.toTypedArray())

    // verify
    actual.collectList().test().verifyError(PermissionDeniedException::class.java)
    verify { service.verifyAuthorize(null, Create) }
  }

  @Test
  fun failedByForbidden() {
    // mock
    val attachments = List(3) { randomAttachment() }

    // invoke
    val actual = service.create(*attachments.toTypedArray())

    // verify
    actual.collectList().test().verifyError(ForbiddenException::class.java)
  }
}