package tech.simter.file.service

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
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import tech.simter.exception.ForbiddenException
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.dto.AttachmentDto4Update
import tech.simter.file.service.AttachmentServiceImpl.OperationType.Update
import tech.simter.file.service.TestUtils.randomAuthenticatedUser
import tech.simter.reactive.security.ReactiveSecurityService
import tech.simter.util.RandomUtils.randomString
import java.io.File
import java.time.OffsetDateTime
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
@SpyBean(AttachmentServiceImpl::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
class UpdateMethodTest @Autowired constructor(
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
  fun updateAndMoveFolder() {
    // mock
    val oldPath = "oldPath"
    val newPath = "newPath"
    val id = randomString()
    val dto = AttachmentDto4Update().apply { path = "path1" }
    val user = randomAuthenticatedUser()
    `when`(dao.findPuids(id)).thenReturn(Flux.just(Optional.ofNullable<String>(null)))
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize(null, Update)
    `when`(dao.update(eq(id), any())).thenReturn(Mono.empty())
    `when`(securityService.getAuthenticatedUser()).thenReturn(Optional.of(user).toMono())
    `when`(dao.getFullPath(id)).thenReturn(oldPath.toMono(), newPath.toMono())
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
    StepVerifier.create(actual).verifyComplete()
    verify(dao).findPuids(id)
    verify(service).verifyAuthorize(null, Update)
    verify(dao).update(eq(id), argThat {
      val data = dto.data
      this.map {
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
    verify(securityService).getAuthenticatedUser()
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
    val dto = AttachmentDto4Update().apply { path = "path1" }
    val user = randomAuthenticatedUser()
    `when`(dao.findPuids(id)).thenReturn(Flux.just(Optional.ofNullable<String>(null)))
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize(null, Update)
    `when`(dao.update(eq(id), any())).thenReturn(Mono.empty())
    `when`(securityService.getAuthenticatedUser()).thenReturn(Optional.of(user).toMono())
    `when`(dao.getFullPath(id)).thenReturn(oldPath.toMono(), newPath.toMono())
    File(fileRootDir).mkdirs()
    val oldFolder = File("$fileRootDir/$oldPath").apply { createNewFile() }
    val newFolder = File("$fileRootDir/$newPath").apply { delete() }
    assertTrue(oldFolder.exists())
    assertFalse(newFolder.exists())

    // invoke
    val actual = service.update(id, dto)

    // verify
    StepVerifier.create(actual).verifyComplete()
    verify(dao).findPuids(id)
    verify(service).verifyAuthorize(null, Update)
    verify(dao).update(eq(id), argThat {
      val data = dto.data
      this.map {
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
    verify(securityService).getAuthenticatedUser()
    assertFalse(oldFolder.exists())
    assertTrue(newFolder.exists())
  }

  @Test
  fun updateAndNotMoveFile() {
    // mock
    val id = randomString()
    val dto = AttachmentDto4Update().apply { name = "name" }
    val user = randomAuthenticatedUser()
    `when`(dao.findPuids(id)).thenReturn(Flux.just(Optional.ofNullable<String>(null)))
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize(null, Update)
    `when`(dao.update(eq(id), any())).thenReturn(Mono.empty())
    `when`(securityService.getAuthenticatedUser()).thenReturn(Optional.of(user).toMono())

    // invoke
    val actual = service.update(id, dto)

    // verify
    StepVerifier.create(actual).verifyComplete()
    verify(dao).findPuids(id)
    verify(service).verifyAuthorize(null, Update)
    verify(dao).update(eq(id), argThat {
      val data = dto.data
      this.map {
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
    verify(securityService).getAuthenticatedUser()
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val id = randomString()
    val dto = AttachmentDto4Update().apply { name = "name" }
    `when`(dao.findPuids(id)).thenReturn(Flux.just(Optional.ofNullable<String>(null)))
    doReturn(Mono.error<Void>(PermissionDeniedException())).`when`(service).verifyAuthorize(null, Update)

    // invoke
    val actual = service.update(id, dto)

    // verify
    StepVerifier.create(actual).verifyError(PermissionDeniedException::class.java)
    verify(dao).findPuids(id)
    verify(service).verifyAuthorize(null, Update)
  }

  @Test
  fun failedByForbidden() {
    // mock
    val id = randomString()
    val dto = AttachmentDto4Update().apply {
      name = "name"
      puid = "puid"
    }
    `when`(dao.findPuids(id)).thenReturn(Flux.just(Optional.of("puid1")))

    // invoke
    val actual = service.update(id, dto)

    // verify
    StepVerifier.create(actual).verifyError(ForbiddenException::class.java)
    verify(dao).findPuids(id)
  }

  @Test
  fun failedByNotFound() {
    // mock
    val id = randomString()
    val dto = AttachmentDto4Update().apply { name = "name" }
    `when`(dao.findPuids(id)).thenReturn(Flux.empty())

    // invoke
    val actual = service.update(id, dto)

    // verify
    StepVerifier.create(actual).verifyError(NotFoundException::class.java)
    verify(dao).findPuids(id)
  }
}