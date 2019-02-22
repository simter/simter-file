package tech.simter.file.service

import com.nhaarman.mockito_kotlin.doReturn
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.service.AttachmentServiceImpl.OperationType.Read
import tech.simter.reactive.security.ReactiveSecurityService
import tech.simter.util.RandomUtils.randomString
import java.util.*

/**
 * Test [AttachmentService.findDescendents]
 *
 * @author zh
 */
@SpringBootTest(classes = [ModuleConfiguration::class])
@MockBean(AttachmentDao::class, ReactiveSecurityService::class)
@SpyBean(AttachmentServiceImpl::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
class FindDescendentsMethodTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val service: AttachmentServiceImpl
) {
  @Test
  fun find() {
    // mock
    val id = randomString()
    val puid = randomString()
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize(puid, Read)
    `when`(dao.findPuids(id)).thenReturn(Flux.just(Optional.ofNullable<String?>(puid)))
    `when`(dao.findDescendents(id)).thenReturn(Flux.empty())

    // invoke
    val actual = service.findDescendents(id)

    // verify
    StepVerifier.create(actual).verifyComplete()
    verify(service).verifyAuthorize(puid, Read)
    verify(dao).findPuids(id)
    verify(dao).findDescendents(id)
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val id = randomString()
    val puid = randomString()
    doReturn(Mono.error<Void>(PermissionDeniedException())).`when`(service).verifyAuthorize(puid, Read)
    `when`(dao.findPuids(id)).thenReturn(Flux.just(Optional.ofNullable<String?>(puid)))

    // invoke
    val actual = service.findDescendents(id)

    // verify
    StepVerifier.create(actual).verifyError(PermissionDeniedException::class.java)
    verify(service).verifyAuthorize(puid, Read)
    verify(dao).findPuids(id)
  }
}