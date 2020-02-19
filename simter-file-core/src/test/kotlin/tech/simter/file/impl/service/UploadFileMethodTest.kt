package tech.simter.file.impl.service

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.util.FileCopyUtils
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.test.test
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.AttachmentService
import tech.simter.file.impl.TestHelper.randomAttachment
import tech.simter.file.impl.TestHelper.randomAuthenticatedUser
import tech.simter.file.impl.UnitTestConfiguration
import tech.simter.file.impl.service.AttachmentServiceImpl.OperationType.Create
import tech.simter.reactive.security.ReactiveSecurityService
import java.io.File
import java.util.*

/**
 * Test [AttachmentService.uploadFile]
 *
 * @author zh
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class UploadFileMethodTest @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val dao: AttachmentDao,
  private val service: AttachmentServiceImpl,
  private val securityService: ReactiveSecurityService
) {
  @AfterEach
  fun clean() {
    File(fileRootDir).deleteRecursively()
  }

  @Test
  fun successByHasUpper() {
    // mock
    val file = ClassPathResource("logback-test.xml")
    val fileDate = file.file.readBytes()
    val attachment = randomAttachment().copy(path = "test.xml")
    val puid = attachment.puid
    val user = randomAuthenticatedUser()
    every { service.verifyAuthorize(puid, Create) } returns Mono.empty()
    every { dao.getFullPath(attachment.upperId!!) } returns "path".toMono()
    every { dao.save(any()) } returns Mono.empty()
    every { securityService.getAuthenticatedUser() } returns Optional.of(user).toMono()

    // invoke
    val actual = service.uploadFile(attachment) { FileCopyUtils.copy(fileDate, it).toMono().then() }

    // 1. verify service.save method invoked
    actual.test().verifyComplete()
    verify {
      dao.getFullPath(attachment.upperId!!)
      dao.save(attachment.copy(creator = user.name, modifier = user.name))
      service.verifyAuthorize(puid, Create)
      securityService.getAuthenticatedUser()
    }

    // 2. verify the saved file exists
    val testFile = File("$fileRootDir/path/test.xml")
    assertTrue(testFile.exists())
  }

  @Test
  fun successByNoUpper() {
    // mock
    val file = ClassPathResource("logback-test.xml")
    val fileDate = file.file.readBytes()
    val attachment = randomAttachment().copy(path = "test.xml", upperId = null)
    val puid = attachment.puid
    val user = randomAuthenticatedUser()
    every { service.verifyAuthorize(puid, Create) } returns Mono.empty()
    every { dao.save(any()) } returns Mono.empty()
    every { securityService.getAuthenticatedUser() } returns Optional.of(user).toMono()

    // invoke
    val actual = service.uploadFile(attachment) { FileCopyUtils.copy(fileDate, it).toMono().then() }

    // 1. verify service.save method invoked
    actual.test().verifyComplete()
    verify {
      dao.save(attachment.copy(creator = user.name, modifier = user.name))
      service.verifyAuthorize(puid, Create)
      securityService.getAuthenticatedUser()
    }

    // 2. verify the saved file exists
    val testFile = File("$fileRootDir/test.xml")
    assertTrue(testFile.exists())
  }

  @Test
  fun failedByNotFoundUpper() {
    // mock
    val attachment = randomAttachment()
    val puid = attachment.puid
    every { service.verifyAuthorize(puid, Create) } returns Mono.empty()
    every { dao.getFullPath(attachment.upperId!!) } returns Mono.empty()

    // invoke
    val actual = service.uploadFile(attachment) { Mono.empty() }

    // verify
    actual.test().verifyError(NotFoundException::class.java)
    verify {
      dao.getFullPath(attachment.upperId!!)
      service.verifyAuthorize(puid, Create)
    }
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val attachment = randomAttachment()
    val puid = attachment.puid
    every { service.verifyAuthorize(puid, Create) } returns Mono.error(PermissionDeniedException())

    // invoke
    val actual = service.uploadFile(attachment) { Mono.empty() }

    // verify
    actual.test().verifyError(PermissionDeniedException::class.java)
    verify { service.verifyAuthorize(puid, Create) }
  }
}