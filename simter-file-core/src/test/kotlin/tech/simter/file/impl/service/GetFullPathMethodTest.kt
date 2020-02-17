package tech.simter.file.impl.service

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.kotlin.test.test
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.AttachmentService
import tech.simter.file.impl.UnitTestConfiguration
import tech.simter.file.impl.service.AttachmentServiceImpl.OperationType.Read
import tech.simter.util.RandomUtils.randomString
import java.util.*

/**
 * Test [AttachmentService.getFullPath]
 *
 * @author zh
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
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
    every { dao.getFullPath(id) } returns fullPath.toMono()
    every { dao.findPuids(id) } returns Flux.just(Optional.of(puid))
    every { service.verifyAuthorize(puid, Read) } returns Mono.empty()

    // invoke
    val actual = service.getFullPath(id)

    // verify
    actual.test().expectNext(fullPath).verifyComplete()
    verify {
      dao.getFullPath(id)
      dao.findPuids(id)
      service.verifyAuthorize(puid, Read)
    }
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val id = randomString()
    val fullPath = randomString()
    val puid = randomString()
    every { dao.getFullPath(id) } returns fullPath.toMono()
    every { dao.findPuids(id) } returns Flux.just(Optional.of(puid))
    every { service.verifyAuthorize(puid, Read) } returns Mono.error(PermissionDeniedException())

    // invoke
    val actual = service.getFullPath(id)

    // verify
    actual.test().verifyError(PermissionDeniedException::class.java)
    verify {
      dao.getFullPath(id)
      dao.findPuids(id)
      service.verifyAuthorize(puid, Read)
    }
  }

  @Test
  fun failedByNotFound() {
    // mock
    val id = randomString()
    every { dao.getFullPath(id) } returns Mono.empty()
    every { dao.findPuids(id) } returns Flux.empty()

    // invoke
    val actual = service.getFullPath(id)

    // verify
    actual.test().verifyError(NotFoundException::class.java)
    verify {
      dao.getFullPath(id)
      dao.findPuids(id)
    }
  }
}