package tech.simter.file.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.dto.AttachmentDto4Update
import tech.simter.reactive.security.ReactiveSecurityService
import java.io.File
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test [AttachmentService.update]
 *
 * @author zh
 */
@SpringBootTest(classes = [AttachmentServiceImpl::class, ModuleConfiguration::class])
@MockBean(AttachmentDao::class, ReactiveSecurityService::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
class UpdateMethodTest @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val dao: AttachmentDao,
  private val service: AttachmentService
) {
  @Test
  fun updateAndMoveFolder() {
    // mock
    val oldPath = "oldPath"
    val newPath = "newPath"
    val id = UUID.randomUUID().toString()
    val dto = AttachmentDto4Update().apply { path = "path1" }
    Mockito.`when`(dao.update(id, dto.data)).thenReturn(Mono.empty())
    Mockito.`when`(dao.getFullPath(id)).thenReturn(oldPath.toMono(), newPath.toMono())
    val oldFolder = File("$fileRootDir/$oldPath").apply {
      delete()
      mkdirs()
      File(this.path, "TestFile.txt").createNewFile()
      File(this.path, "TestFolder").mkdirs()
    }
    val newFolder = File("$fileRootDir/$newPath").apply { deleteRecursively() }
    assertTrue(oldFolder.exists())
    assertFalse(newFolder.exists())

    // invoke
    val actual = service.update(id, dto)

    // verify
    StepVerifier.create(actual).verifyComplete()
    Mockito.verify(dao).update(id, dto.data)
    assertFalse(oldFolder.exists())
    assertTrue(newFolder.exists())
    val expected = listOf("TestFile.txt", "TestFolder")
    val actualList = newFolder.list()
    assertEquals(expected.size, actualList.size)
    actualList.forEach { assertTrue(expected.contains(it)) }
  }

  @Test
  fun updateAndMoveFile() {
    // mock
    val oldPath = "oldFile.txt"
    val newPath = "newFile.txt"
    val id = UUID.randomUUID().toString()
    val dto = AttachmentDto4Update().apply { path = "path1" }
    Mockito.`when`(dao.update(id, dto.data)).thenReturn(Mono.empty())
    Mockito.`when`(dao.getFullPath(id)).thenReturn(oldPath.toMono(), newPath.toMono())
    val oldFolder = File("$fileRootDir/$oldPath").apply { createNewFile() }
    val newFolder = File("$fileRootDir/$newPath").apply { delete() }
    assertTrue(oldFolder.exists())
    assertFalse(newFolder.exists())

    // invoke
    val actual = service.update(id, dto)

    // verify
    StepVerifier.create(actual).verifyComplete()
    Mockito.verify(dao).update(id, dto.data)
    assertFalse(oldFolder.exists())
    assertTrue(newFolder.exists())
  }

  @Test
  fun updateAndNotMoveFile() {
    // mock
    val id = UUID.randomUUID().toString()
    val dto = AttachmentDto4Update().apply { name = "name" }
    Mockito.`when`(dao.update(id, dto.data)).thenReturn(Mono.empty())

    // invoke
    val actual = service.update(id, dto)

    // verify
    StepVerifier.create(actual).verifyComplete()
    Mockito.verify(dao).update(id, dto.data)
  }
}