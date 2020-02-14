package tech.simter.file.impl.service

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.eq
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.domain.AttachmentDto
import tech.simter.file.impl.service.AttachmentServiceImpl.OperationType.Update
import tech.simter.file.impl.service.TestHelper.randomAuthenticatedUser
import tech.simter.reactive.security.ReactiveSecurityService
import java.io.File
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertTrue

/**
 * Test [AttachmentService.reuploadFile]
 *
 * @author zh
 */
@SpringBootTest(classes = [AttachmentServiceImpl::class, ModuleConfiguration::class])
@MockBean(AttachmentDao::class, ReactiveSecurityService::class)
@SpyBean(AttachmentServiceImpl::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
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
    `when`(dao.findPuids(attachment.id!!)).thenReturn(Flux.just(Optional.ofNullable<String>(null)))
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize(null, Update)
    `when`(dao.getFullPath(attachment.id!!)).thenReturn("test.xml".toMono())
    `when`(dao.update(eq(attachment.id!!), any())).thenReturn(Mono.empty())
    `when`(securityService.getAuthenticatedUser()).thenReturn(Optional.of(user).toMono())

    // invoke
    val actual = service.reuploadFile(attachment, fileDate)

    // 1. verify service.save method invoked
    StepVerifier.create(actual).verifyComplete()
    verify(dao).findPuids(attachment.id!!)
    verify(dao).getFullPath(attachment.id!!)
    verify(dao).update(eq(attachment.id!!), argThat {
      val data = attachment.data
      this.map {
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
    verify(securityService).getAuthenticatedUser()
    verify(service).verifyAuthorize(null, Update)

    // 2. verify the saved file exists
    val testFile = File("$fileRootDir/test.xml")
    assertTrue(testFile.exists())
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val attachment = randomAttachment()
    `when`(dao.findPuids(attachment.id!!)).thenReturn(Flux.just(Optional.ofNullable<String>(null)))
    doReturn(Mono.error<Void>(PermissionDeniedException())).`when`(service).verifyAuthorize(null, Update)

    // invoke
    val actual = service.reuploadFile(attachment, byteArrayOf())

    // verify
    StepVerifier.create(actual).verifyError(PermissionDeniedException::class.java)
    verify(dao).findPuids(attachment.id!!)
    verify(service).verifyAuthorize(null, Update)
  }

  @Test
  fun failedByNotFound() {
    // mock
    val attachment = randomAttachment()
    `when`(dao.findPuids(attachment.id!!)).thenReturn(Flux.empty())

    // invoke
    val actual = service.reuploadFile(attachment, byteArrayOf())

    // verify
    StepVerifier.create(actual).verifyError(NotFoundException::class.java)
    verify(dao).findPuids(attachment.id!!)
  }
}