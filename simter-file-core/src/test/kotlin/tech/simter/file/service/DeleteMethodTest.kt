package tech.simter.file.service

import com.nhaarman.mockito_kotlin.doReturn
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.test.StepVerifier
import tech.simter.exception.ForbiddenException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.service.AttachmentServiceImpl.OperationType.Delete
import tech.simter.reactive.security.ReactiveSecurityService
import tech.simter.util.RandomUtils.randomString
import java.io.File
import java.util.*
import kotlin.test.assertTrue

/**
 * Test [AttachmentService.delete]
 *
 * @author zh
 */
@SpringBootTest(classes = [ModuleConfiguration::class])
@MockBean(AttachmentDao::class, ReactiveSecurityService::class)
@SpyBean(AttachmentServiceImpl::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
class DeleteMethodTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val service: AttachmentServiceImpl,
  @Value("\${simter.file.root}") private val fileRootDir: String
) {
  @AfterEach
  fun clean() {
    File(fileRootDir).deleteRecursively()
  }

  @Test
  fun deleteSomething() {
    // mock data
    val folderPaths = listOf("path1", "path2")
    val filePaths = listOf("path1/file1.txt", "path3/file2.txt")
    val paths = folderPaths.plus(filePaths).toTypedArray()
    val ids = Array(3) { randomString() }
    val puid = randomString()
    `when`(dao.findPuids(*ids)).thenReturn(Flux.just(Optional.of(puid)))
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize(puid, Delete)
    `when`(dao.delete(*ids)).thenReturn(Flux.just(*paths))
    // mock file
    // specified delete: path1, path2, file1 and file2
    // actual delete: path1, path2, file1, file2 and file3
    //          root
    //    /      |        \
    // path1   path2     path3
    //   |       |      /     \
    // file1   file3  file2  file4
    val folders = folderPaths.plus("path3").map { File("$fileRootDir/$it") }
    val files = filePaths.plus("path2/file3.txt")
      .plus("path3/file4.txt").map { File("$fileRootDir/$it") }
    folders.forEach {
      it.delete()
      it.mkdirs()
    }
    files.forEach {
      File(it.parent).mkdirs()
      it.createNewFile()
    }

    // invoke
    val actual = service.delete(*ids)

    // verify
    StepVerifier.create(actual).verifyComplete()
    verify(dao).findPuids(*ids)
    verify(service).verifyAuthorize(puid, Delete)
    verify(dao).delete(*ids)
    assertTrue(folders.map { it.exists() }.run { dropLast(1).any { !it } && last() })
    assertTrue(files.map { it.exists() }.run { dropLast(1).any { !it } && last() })
  }

  @Test
  fun deleteNothing() {
    // mock
    val ids = Array(3) { randomString() }
    val puid = randomString()
    `when`(dao.findPuids(*ids)).thenReturn(Flux.just(Optional.of(puid)))
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize(puid, Delete)
    `when`(dao.delete(*ids)).thenReturn(Flux.empty())

    // invoke
    val actual = service.delete(*ids)

    // verify
    StepVerifier.create(actual).verifyComplete()
    verify(dao).findPuids(*ids)
    verify(service).verifyAuthorize(puid, Delete)
    verify(dao).delete(*ids)
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val ids = Array(3) { randomString() }
    val puid = randomString()
    `when`(dao.findPuids(*ids)).thenReturn(Flux.just(Optional.of(puid)))
    doReturn(Mono.error<Void>(PermissionDeniedException())).`when`(service).verifyAuthorize(puid, Delete)

    // invoke
    val actual = service.delete(*ids)

    // verify
    StepVerifier.create(actual).verifyError(PermissionDeniedException::class.java)
    verify(dao).findPuids(*ids)
    verify(service).verifyAuthorize(puid, Delete)
  }

  @Test
  fun failedByForbidden() {
    // mock
    val ids = Array(3) { randomString() }
    val puids = Array(2) { Optional.of(randomString()) }
    `when`(dao.findPuids(*ids)).thenReturn(puids.toFlux())

    // invoke
    val actual = service.delete(*ids)

    // verify
    StepVerifier.create(actual).verifyError(ForbiddenException::class.java)
    verify(dao).findPuids(*ids)
  }
}