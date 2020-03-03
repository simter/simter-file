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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.test.test
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.FILE_ROOT_DIR_KEY
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.AttachmentService
import tech.simter.file.impl.TestHelper.randomAttachment
import tech.simter.file.impl.TestHelper.randomAuthenticatedUser
import tech.simter.file.impl.UnitTestConfiguration
import tech.simter.file.impl.domain.AttachmentUpdateInfoImpl
import tech.simter.file.impl.service.AttachmentServiceImpl.OperationType.Update
import tech.simter.reactive.security.ReactiveSecurityService
import java.io.File
import java.time.OffsetDateTime
import java.util.*

/**
 * Test [AttachmentService.reuploadFile]
 *
 * @author zh
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class ReuploadFileMethodTest @Autowired constructor(
  @Value("\${$FILE_ROOT_DIR_KEY}") private val fileRootDir: String,
  private val dao: AttachmentDao,
  private val service: AttachmentServiceImpl,
  private val securityService: ReactiveSecurityService
) {
  @AfterEach
  fun clean() {
    File(fileRootDir).deleteRecursively()
  }

  @Test
  fun success() {
    // mock
    val file = ClassPathResource("logback-test.xml")
    val fileDate = file.file.readBytes()
    val attachment = randomAttachment().copy(size = fileDate.size.toLong())
    val user = randomAuthenticatedUser()
    every { dao.findPuids(attachment.id) } returns Flux.just(Optional.ofNullable<String>(null))
    every { service.verifyAuthorize(null, Update) } returns Mono.empty()
    every { dao.getFullPath(attachment.id) } returns "test.xml".toMono()
    every { dao.update(eq(attachment.id), any()) } returns Mono.empty()
    every { securityService.getAuthenticatedUser() } returns Optional.of(user).toMono()

    // invoke
    val updateInfo = AttachmentUpdateInfoImpl.from(attachment)
    val actual = service.reuploadFile(attachment.id, fileDate, updateInfo)

    // 1. verify service.save method invoked
    actual.test().verifyComplete()
    verify {
      dao.findPuids(attachment.id)
      dao.getFullPath(attachment.id)
      dao.update(eq(attachment.id), match { m ->
        val data = updateInfo.data
        m.map {
          val key = it.key
          val value = it.value
          when {
            data.containsKey(key) -> data[key] == value
            key == "modifier" -> user.name == value
            key == "modifyOn" -> !OffsetDateTime.now().isAfter(value as OffsetDateTime)
            key == "id" -> true
            else -> false
          }
        }.any()
      })
      securityService.getAuthenticatedUser()
      service.verifyAuthorize(null, Update)
    }

    // 2. verify the saved file exists
    val testFile = File("$fileRootDir/test.xml")
    assertTrue(testFile.exists())
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val attachment = randomAttachment()
    every { dao.findPuids(attachment.id) } returns Flux.just(Optional.ofNullable<String>(null))
    every { service.verifyAuthorize(null, Update) } returns Mono.error(PermissionDeniedException())

    // invoke
    val updateInfo = AttachmentUpdateInfoImpl.from(attachment)
    val actual = service.reuploadFile(attachment.id, byteArrayOf(), updateInfo)

    // verify
    actual.test().verifyError(PermissionDeniedException::class.java)
    verify {
      dao.findPuids(attachment.id)
      service.verifyAuthorize(null, Update)
    }
  }

  @Test
  fun failedByNotFound() {
    // mock
    val attachment = randomAttachment()
    every { dao.findPuids(attachment.id) } returns Flux.empty()

    // invoke
    val updateInfo = AttachmentUpdateInfoImpl.from(attachment)
    val actual = service.reuploadFile(attachment.id, byteArrayOf(), updateInfo)

    // verify
    actual.test().verifyError(NotFoundException::class.java)
    verify { dao.findPuids(attachment.id) }
  }
}