package tech.simter.file.impl.service

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.test.test
import tech.simter.exception.ForbiddenException
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.FILE_ROOT_DIR_KEY
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.AttachmentService
import tech.simter.file.impl.TestHelper.randomAuthenticatedUser
import tech.simter.file.impl.UnitTestConfiguration
import tech.simter.file.impl.domain.AttachmentUpdateInfoImpl
import tech.simter.file.impl.service.AttachmentServiceImpl.OperationType.Update
import tech.simter.reactive.security.ReactiveSecurityService
import tech.simter.util.RandomUtils.randomString
import java.io.File
import java.time.OffsetDateTime
import java.util.*

/**
 * Test [AttachmentService.update]
 *
 * @author zh
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class UpdateMethodTest @Autowired constructor(
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
  fun updateAndMoveFolder() {
    // mock
    val oldPath = "oldPath"
    val newPath = "newPath"
    val id = randomString()
    val dto = AttachmentUpdateInfoImpl().apply { path = "path1" }
    val user = randomAuthenticatedUser()
    every { dao.findPuids(id) } returns Flux.just(Optional.ofNullable<String>(null))
    every { service.verifyAuthorize(null, Update) } returns Mono.empty()
    every { dao.update(eq(id), any()) } returns Mono.empty()
    every { securityService.getAuthenticatedUser() } returns Optional.of(user).toMono()
    every { dao.getFullPath(id) } returnsMany listOf(oldPath.toMono(), newPath.toMono())
    File(fileRootDir).mkdirs()
    val oldFolder = File("$fileRootDir/$oldPath").apply {
      deleteRecursively()
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
    actual.test().verifyComplete()
    verify {
      dao.findPuids(id)
      service.verifyAuthorize(null, Update)
      dao.update(eq(id), match { m ->
        val data = dto.data
        m.map {
          val key = it.key
          val value = it.value
          when {
            data.containsKey(key) -> data[key] == value
            key == "modifier" -> user.name == value
            key == "modifyOn" -> !OffsetDateTime.now().isAfter(value as OffsetDateTime)
            else -> false
          }
        }.any()
      })
      securityService.getAuthenticatedUser()
    }
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
    val id = randomString()
    val dto = AttachmentUpdateInfoImpl().apply { path = "path1" }
    val user = randomAuthenticatedUser()
    every { dao.findPuids(id) } returns Flux.just(Optional.ofNullable<String>(null))
    every { service.verifyAuthorize(null, Update) } returns Mono.empty()
    every { dao.update(eq(id), any()) } returns Mono.empty()
    every { securityService.getAuthenticatedUser() } returns Optional.of(user).toMono()
    every { dao.getFullPath(id) } returnsMany listOf(oldPath.toMono(), newPath.toMono())
    File(fileRootDir).mkdirs()
    val oldFolder = File("$fileRootDir/$oldPath").apply { createNewFile() }
    val newFolder = File("$fileRootDir/$newPath").apply { delete() }
    assertTrue(oldFolder.exists())
    assertFalse(newFolder.exists())

    // invoke
    val actual = service.update(id, dto)

    // verify
    actual.test().verifyComplete()
    verify {
      dao.findPuids(id)
      service.verifyAuthorize(null, Update)
      dao.update(eq(id), match { m ->
        val data = dto.data
        m.map {
          val key = it.key
          val value = it.value
          when {
            data.containsKey(key) -> data[key] == value
            key == "modifier" -> user.name == value
            key == "modifyOn" -> !OffsetDateTime.now().isAfter(value as OffsetDateTime)
            else -> false
          }
        }.any()
      })
      securityService.getAuthenticatedUser()
    }
    assertFalse(oldFolder.exists())
    assertTrue(newFolder.exists())
  }

  @Test
  fun updateAndNotMoveFile() {
    // mock
    val id = randomString()
    val dto = AttachmentUpdateInfoImpl().apply { name = "name" }
    val user = randomAuthenticatedUser()
    every { dao.findPuids(id) } returns Flux.just(Optional.ofNullable<String>(null))
    every { service.verifyAuthorize(null, Update) } returns Mono.empty()
    every { dao.update(eq(id), any()) } returns Mono.empty()
    every { securityService.getAuthenticatedUser() } returns Optional.of(user).toMono()

    // invoke
    val actual = service.update(id, dto)

    // verify
    actual.test().verifyComplete()
    verify {
      dao.findPuids(id)
      service.verifyAuthorize(null, Update)
      dao.update(eq(id), match { m ->
        val data = dto.data
        m.map {
          val key = it.key
          val value = it.value
          when {
            data.containsKey(key) -> data[key] == value
            key == "modifier" -> user.name == value
            key == "modifyOn" -> !OffsetDateTime.now().isAfter(value as OffsetDateTime)
            else -> false
          }
        }.any()
      })
      securityService.getAuthenticatedUser()
    }
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val id = randomString()
    val dto = AttachmentUpdateInfoImpl().apply { name = "name" }
    every { dao.findPuids(id) } returns Flux.just(Optional.ofNullable<String>(null))
    every { service.verifyAuthorize(null, Update) } returns Mono.error(PermissionDeniedException())

    // invoke
    val actual = service.update(id, dto)

    // verify
    actual.test().verifyError(PermissionDeniedException::class.java)
    verify {
      dao.findPuids(id)
      service.verifyAuthorize(null, Update)
    }
  }

  @Test
  fun failedByForbidden() {
    // mock
    val id = randomString()
    val dto = AttachmentUpdateInfoImpl().apply {
      name = "name"
      puid = "puid"
    }
    every { dao.findPuids(id) } returns Flux.just(Optional.of("puid1"))

    // invoke
    val actual = service.update(id, dto)

    // verify
    actual.test().verifyError(ForbiddenException::class.java)
    verify { dao.findPuids(id) }
  }

  @Test
  fun failedByNotFound() {
    // mock
    val id = randomString()
    val dto = AttachmentUpdateInfoImpl().apply { name = "name" }
    every { dao.findPuids(id) } returns Flux.empty()

    // invoke
    val actual = service.update(id, dto)

    // verify
    actual.test().verifyError(NotFoundException::class.java)
    verify { dao.findPuids(id) }
  }
}