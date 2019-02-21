package tech.simter.file.service

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.TestPropertySource
import org.springframework.util.FileCopyUtils
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import tech.simter.exception.NotFoundException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.po.Attachment
import tech.simter.reactive.security.ReactiveSecurityService
import java.io.File
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertTrue

/**
 * Test [AttachmentService.uploadFile]
 *
 * @author zh
 */
@SpringBootTest(classes = [AttachmentServiceImpl::class, ModuleConfiguration::class])
@MockBean(AttachmentDao::class, ReactiveSecurityService::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
class UploadFileMethodTest @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val dao: AttachmentDao,
  private val service: AttachmentService
) {
  private fun randomAttachment(): Attachment {
    val now = OffsetDateTime.now()
    return Attachment(UUID.randomUUID().toString(), "/data1", "Sample", "png", 123, now.minusDays(1),
      "Simter", now.minusDays(1), "Simter", "puid", UUID.randomUUID().toString())
  }

  @AfterEach
  fun clean() {
    File(fileRootDir).deleteRecursively()
  }

  @Test
  fun hasUpper() {
    // mock
    val file = ClassPathResource("logback-test.xml")
    val fileDate = file.file.readBytes()
    val attachment = randomAttachment().copy(path = "test.xml")
    `when`(dao.getFullPath(attachment.upperId!!)).thenReturn("path".toMono())
    `when`(dao.save(attachment)).thenReturn(Mono.empty())

    // invoke
    val actual = service.uploadFile(attachment) { FileCopyUtils.copy(fileDate, it).toMono().then() }

    // 1. verify service.save method invoked
    StepVerifier.create(actual).verifyComplete()
    verify(dao).getFullPath(attachment.upperId!!)
    verify(dao).save(attachment)

    // 2. verify the saved file exists
    val testFile = File("$fileRootDir/path/test.xml")
    assertTrue(testFile.exists())
  }

  @Test
  fun noUpper() {
    // mock
    val file = ClassPathResource("logback-test.xml")
    val fileDate = file.file.readBytes()
    val attachment = randomAttachment().copy(path = "test.xml", upperId = null)
    `when`(dao.save(attachment)).thenReturn(Mono.empty())

    // invoke
    val actual = service.uploadFile(attachment) { FileCopyUtils.copy(fileDate, it).toMono().then() }

    // 1. verify service.save method invoked
    StepVerifier.create(actual).verifyComplete()
    verify(dao).save(attachment)

    // 2. verify the saved file exists
    val testFile = File("$fileRootDir/test.xml")
    assertTrue(testFile.exists())
  }

  @Test
  fun hasUpperButNotFoundUpper() {
    // mock
    val attachment = randomAttachment()
    `when`(dao.getFullPath(attachment.upperId!!)).thenReturn(Mono.empty())

    // invoke
    val actual = service.uploadFile(attachment) { Mono.empty() }

    // verify
    StepVerifier.create(actual).verifyError(NotFoundException::class.java)
    verify(dao).getFullPath(attachment.upperId!!)
  }
}