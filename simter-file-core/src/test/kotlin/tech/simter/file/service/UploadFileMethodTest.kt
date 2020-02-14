package tech.simter.file.service

import com.nhaarman.mockito_kotlin.any
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
import org.springframework.util.FileCopyUtils
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.service.AttachmentServiceImpl.OperationType.Create
import tech.simter.file.service.TestUtils.randomAttachment
import tech.simter.file.service.TestUtils.randomAuthenticatedUser
import tech.simter.reactive.security.ReactiveSecurityService
import java.io.File
import java.util.*
import kotlin.test.assertTrue

/**
 * Test [AttachmentService.uploadFile]
 *
 * @author zh
 */
@SpringBootTest(classes = [ModuleConfiguration::class])
@MockBean(AttachmentDao::class, ReactiveSecurityService::class)
@SpyBean(AttachmentServiceImpl::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
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
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize(puid, Create)
    `when`(dao.getFullPath(attachment.upperId!!)).thenReturn("path".toMono())
    `when`(dao.save(any())).thenReturn(Mono.empty())
    `when`(securityService.getAuthenticatedUser()).thenReturn(Optional.of(user).toMono())

    // invoke
    val actual = service.uploadFile(attachment) { FileCopyUtils.copy(fileDate, it).toMono().then() }

    // 1. verify service.save method invoked
    StepVerifier.create(actual).verifyComplete()
    verify(dao).getFullPath(attachment.upperId!!)
    verify(dao).save(attachment.copy(creator = user.name, modifier = user.name))
    verify(service).verifyAuthorize(puid, Create)
    verify(securityService).getAuthenticatedUser()

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
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize(puid, Create)
    `when`(dao.save(any())).thenReturn(Mono.empty())
    `when`(securityService.getAuthenticatedUser()).thenReturn(Optional.of(user).toMono())

    // invoke
    val actual = service.uploadFile(attachment) { FileCopyUtils.copy(fileDate, it).toMono().then() }

    // 1. verify service.save method invoked
    StepVerifier.create(actual).verifyComplete()
    verify(dao).save(attachment.copy(creator = user.name, modifier = user.name))
    verify(service).verifyAuthorize(puid, Create)
    verify(securityService).getAuthenticatedUser()

    // 2. verify the saved file exists
    val testFile = File("$fileRootDir/test.xml")
    assertTrue(testFile.exists())
  }

  @Test
  fun failedByNotFoundUpper() {
    // mock
    val attachment = randomAttachment()
    val puid = attachment.puid
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize(puid, Create)
    `when`(dao.getFullPath(attachment.upperId!!)).thenReturn(Mono.empty())

    // invoke
    val actual = service.uploadFile(attachment) { Mono.empty() }

    // verify
    StepVerifier.create(actual).verifyError(NotFoundException::class.java)
    verify(dao).getFullPath(attachment.upperId!!)
    verify(service).verifyAuthorize(puid, Create)
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val attachment = randomAttachment()
    val puid = attachment.puid
    doReturn(Mono.error<Void>(PermissionDeniedException())).`when`(service).verifyAuthorize(puid, Create)

    // invoke
    val actual = service.uploadFile(attachment) { Mono.empty() }

    // verify
    StepVerifier.create(actual).verifyError(PermissionDeniedException::class.java)
    verify(service).verifyAuthorize(puid, Create)
  }
}