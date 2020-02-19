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
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.AttachmentService
import tech.simter.file.core.domain.AttachmentDto
import tech.simter.file.impl.TestHelper.randomAuthenticatedUser
import tech.simter.file.impl.UnitTestConfiguration
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
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val dao: AttachmentDao,
  private val service: AttachmentServiceImpl,
  private val securityService: ReactiveSecurityService
) {
  private fun randomAttachment(size: Long? = null): AttachmentDto {
    return AttachmentDto().apply {
      size?.let { this.size = it }
      id = UUID.randomUUID().toString()
    }
  }

  @AfterEach
  fun clean() {
    File(fileRootDir).deleteRecursively()
  }

  @Test
  fun success() {
    // mock
    val file = ClassPathResource("logback-test.xml")
    val fileDate = file.file.readBytes()
    val attachment = randomAttachment(size = fileDate.size.toLong())
    val user = randomAuthenticatedUser()
    every { dao.findPuids(attachment.id!!) } returns Flux.just(Optional.ofNullable<String>(null))
    every { service.verifyAuthorize(null, Update) } returns Mono.empty()
    every { dao.getFullPath(attachment.id!!) } returns "test.xml".toMono()
    every { dao.update(eq(attachment.id!!), any()) } returns Mono.empty()
    every { securityService.getAuthenticatedUser() } returns Optional.of(user).toMono()

    // invoke
    val actual = service.reuploadFile(attachment, fileDate)

    // 1. verify service.save method invoked
    actual.test().verifyComplete()
    verify {
      dao.findPuids(attachment.id!!)
      dao.getFullPath(attachment.id!!)
      dao.update(eq(attachment.id!!), match { m ->
        val data = attachment.data
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
    every { dao.findPuids(attachment.id!!) } returns Flux.just(Optional.ofNullable<String>(null))
    every { service.verifyAuthorize(null, Update) } returns Mono.error(PermissionDeniedException())

    // invoke
    val actual = service.reuploadFile(attachment, byteArrayOf())

    // verify
    actual.test().verifyError(PermissionDeniedException::class.java)
    verify {
      dao.findPuids(attachment.id!!)
      service.verifyAuthorize(null, Update)
    }
  }

  @Test
  fun failedByNotFound() {
    // mock
    val attachment = randomAttachment()
    every { dao.findPuids(attachment.id!!) } returns Flux.empty()

    // invoke
    val actual = service.reuploadFile(attachment, byteArrayOf())

    // verify
    actual.test().verifyError(NotFoundException::class.java)
    verify { dao.findPuids(attachment.id!!) }
  }
}