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
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import tech.simter.exception.NotFoundException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.dto.AttachmentDto
import tech.simter.reactive.security.ReactiveSecurityService
import java.io.File
import java.util.*
import kotlin.test.assertTrue

/**
 * Test [AttachmentService.reuploadFile]
 *
 * @author zh
 */
@SpringBootTest(classes = [AttachmentServiceImpl::class, ModuleConfiguration::class])
@MockBean(AttachmentDao::class, ReactiveSecurityService::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
class ReuploadFileMethodTest @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val dao: AttachmentDao,
  private val service: AttachmentService
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
    `when`(dao.getFullPath(attachment.id!!)).thenReturn("test.xml".toMono())
    `when`(dao.update(attachment.id!!, attachment.data.filter { it.key != "id" })).thenReturn(Mono.empty())

    // invoke
    val actual = service.reuploadFile(attachment, fileDate)

    // 1. verify service.save method invoked
    StepVerifier.create(actual).verifyComplete()
    verify(dao).getFullPath(attachment.id!!)
    verify(dao).update(attachment.id!!, attachment.data.filter { it.key != "id" })

    // 2. verify the saved file exists
    val testFile = File("$fileRootDir/test.xml")
    assertTrue(testFile.exists())
  }

  @Test
  fun `found noting`() {
    // mock
    val attachment = randomAttachment()
    `when`(dao.getFullPath(attachment.id!!)).thenReturn(Mono.empty())

    // invoke
    val actual = service.reuploadFile(attachment, byteArrayOf())

    // verify
    StepVerifier.create(actual).verifyError(NotFoundException::class.java)
    verify(dao).getFullPath(attachment.id!!)
  }
}