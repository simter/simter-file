package tech.simter.file.impl.service

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
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.service.AttachmentServiceImpl.OperationType.Read
import tech.simter.file.impl.service.TestUtils.randomAttachment
import tech.simter.reactive.security.ReactiveSecurityService

/**
 * Test [AttachmentService.get]
 *
 * @author zh
 */
@SpringBootTest(classes = [ModuleConfiguration::class])
@MockBean(AttachmentDao::class, ReactiveSecurityService::class)
@SpyBean(AttachmentServiceImpl::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
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
    `when`(dao.get(id)).thenReturn(attachment.toMono())
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize(puid, Read)

    // invoke
    val actual = service.get(id)

    // verify
    StepVerifier.create(actual).expectNext(attachment).verifyComplete()
    verify(dao).get(id)
    verify(service).verifyAuthorize(puid, Read)
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val attachment = randomAttachment()
    val id = attachment.id
    val puid = attachment.puid
    `when`(dao.get(id)).thenReturn(attachment.toMono())
    doReturn(Mono.error<Void>(PermissionDeniedException())).`when`(service).verifyAuthorize(puid, Read)

    // invoke
    val actual = service.get(id)

    // verify
    StepVerifier.create(actual).verifyError(PermissionDeniedException::class.java)
    verify(dao).get(id)
    verify(service).verifyAuthorize(puid, Read)
  }
}