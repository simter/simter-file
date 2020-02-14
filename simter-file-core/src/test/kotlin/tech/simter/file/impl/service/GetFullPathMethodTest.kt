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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.service.AttachmentServiceImpl.OperationType.Read
import tech.simter.reactive.security.ReactiveSecurityService
import tech.simter.util.RandomUtils.randomString
import java.util.*

/**
 * Test [AttachmentService.getFullPath]
 *
 * @author zh
 */
@SpringBootTest(classes = [ModuleConfiguration::class])
@MockBean(AttachmentDao::class, ReactiveSecurityService::class)
@SpyBean(AttachmentServiceImpl::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
class GetFullPathMethodTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val service: AttachmentServiceImpl
) {
  @Test
  fun get() {
    // mock
    val id = randomString()
    val fullPath = randomString()
    val puid = randomString()
    `when`(dao.getFullPath(id)).thenReturn(fullPath.toMono())
    `when`(dao.findPuids(id)).thenReturn(Flux.just(Optional.of(puid)))
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize(puid, Read)

    // invoke
    val actual = service.getFullPath(id)

    // verify
    StepVerifier.create(actual).expectNext(fullPath).verifyComplete()
    verify(dao).getFullPath(id)
    verify(dao).findPuids(id)
    verify(service).verifyAuthorize(puid, Read)
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val id = randomString()
    val fullPath = randomString()
    val puid = randomString()
    `when`(dao.getFullPath(id)).thenReturn(fullPath.toMono())
    `when`(dao.findPuids(id)).thenReturn(Flux.just(Optional.of(puid)))
    doReturn(Mono.error<Void>(PermissionDeniedException())).`when`(service).verifyAuthorize(puid, Read)

    // invoke
    val actual = service.getFullPath(id)

    // verify
    StepVerifier.create(actual).verifyError(PermissionDeniedException::class.java)
    verify(dao).getFullPath(id)
    verify(dao).findPuids(id)
    verify(service).verifyAuthorize(puid, Read)
  }

  @Test
  fun failedByNotFound() {
    // mock
    val id = randomString()
    `when`(dao.getFullPath(id)).thenReturn(Mono.empty())
    `when`(dao.findPuids(id)).thenReturn(Flux.empty())

    // invoke
    val actual = service.getFullPath(id)

    // verify
    StepVerifier.create(actual).verifyError(NotFoundException::class.java)
    verify(dao).getFullPath(id)
    verify(dao).findPuids(id)
  }
}