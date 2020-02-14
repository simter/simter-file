package tech.simter.file.impl.service

import com.nhaarman.mockito_kotlin.doReturn
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.service.AttachmentServiceImpl.OperationType.Read
import tech.simter.reactive.security.ReactiveSecurityService

/**
 * Test [AttachmentService.find]
 *
 * @author zh
 */
@SpringBootTest(classes = [ModuleConfiguration::class])
@MockBean(AttachmentDao::class, ReactiveSecurityService::class)
@SpyBean(AttachmentServiceImpl::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
class FindMethodTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val service: AttachmentServiceImpl
) {
  @Test
  fun find() {
    // mock
    val pageable = PageRequest.of(1, 25)
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize("admin", Read)
    `when`(dao.find(pageable)).thenReturn(Mono.empty())

    // invoke
    val actual = service.find(pageable)

    // verify
    StepVerifier.create(actual).verifyComplete()
    verify(service).verifyAuthorize("admin", Read)
    verify(dao).find(pageable)
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val pageable = PageRequest.of(1, 25)
    doReturn(Mono.error<Void>(PermissionDeniedException())).`when`(service).verifyAuthorize("admin", Read)

    // invoke
    val actual = service.find(pageable)

    // verify
    StepVerifier.create(actual).verifyError(PermissionDeniedException::class.java)
    verify(service).verifyAuthorize("admin", Read)
  }
}