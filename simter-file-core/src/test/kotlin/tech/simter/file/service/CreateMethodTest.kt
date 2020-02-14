package tech.simter.file.service

import com.nhaarman.mockito_kotlin.anyVararg
import com.nhaarman.mockito_kotlin.doReturn
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import tech.simter.exception.ForbiddenException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.service.AttachmentServiceImpl.OperationType.Create
import tech.simter.file.service.TestUtils.randomAttachment
import tech.simter.file.service.TestUtils.randomAuthenticatedUser
import tech.simter.reactive.security.ReactiveSecurityService
import java.util.*

/**
 * Test [AttachmentService.create]
 *
 * @author zh
 */
@SpringBootTest(classes = [ModuleConfiguration::class])
@MockBean(AttachmentDao::class, ReactiveSecurityService::class)
@SpyBean(AttachmentServiceImpl::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
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
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize(null, Create)
    `when`(dao.save(anyVararg())).thenReturn(Mono.empty())
    `when`(securityService.getAuthenticatedUser()).thenReturn(Optional.of(user).toMono())

    // invoke
    val actual = service.create(*attachments.toTypedArray())

    // verify
    StepVerifier.create(actual.collectList()).expectNext(ids).verifyComplete()
    verify(dao).save(*attachments.map { it.copy(creator = user.name, modifier = user.name) }.toTypedArray())
    verify(service).verifyAuthorize(null, Create)
    verify(securityService).getAuthenticatedUser()
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val attachments = List(3) { randomAttachment().copy(puid = null) }
    doReturn(Mono.error<Void>(PermissionDeniedException())).`when`(service).verifyAuthorize(null, Create)

    // invoke
    val actual = service.create(*attachments.toTypedArray())

    // verify
    StepVerifier.create(actual.collectList()).verifyError(PermissionDeniedException::class.java)
    verify(service).verifyAuthorize(null, Create)
  }

  @Test
  fun failedByForbidden() {
    // mock
    val attachments = List(3) { randomAttachment() }

    // invoke
    val actual = service.create(*attachments.toTypedArray())

    // verify
    StepVerifier.create(actual.collectList()).verifyError(ForbiddenException::class.java)
  }
}