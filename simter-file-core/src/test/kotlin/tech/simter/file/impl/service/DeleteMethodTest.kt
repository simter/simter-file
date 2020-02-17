package tech.simter.file.impl.service

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.test.test
import tech.simter.exception.ForbiddenException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.AttachmentService
import tech.simter.file.impl.UnitTestConfiguration
import tech.simter.file.impl.service.AttachmentServiceImpl.OperationType.Delete
import tech.simter.util.RandomUtils.randomString
import java.io.File
import java.util.*

/**
 * Test [AttachmentService.delete]
 *
 * @author zh
 * @author Rj
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
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
    every { dao.findPuids(*ids) } returns Flux.just(Optional.of(puid))
    every { service.verifyAuthorize(puid, Delete) } returns Mono.empty()
    every { dao.delete(*ids) } returns Flux.just(*paths)
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
    actual.test().verifyComplete()
    verify {
      dao.findPuids(*ids)
      service.verifyAuthorize(puid, Delete)
      dao.delete(*ids)
    }
    assertTrue(folders.map { it.exists() }.run { dropLast(1).any { !it } && last() })
    assertTrue(files.map { it.exists() }.run { dropLast(1).any { !it } && last() })
  }

  @Test
  fun deleteNothing() {
    // mock
    val ids = Array(3) { randomString() }
    val puid = randomString()
    every { dao.findPuids(*ids) } returns Flux.just(Optional.of(puid))
    every { service.verifyAuthorize(puid, Delete) } returns Mono.empty()
    every { dao.delete(*ids) } returns Flux.empty()

    // invoke
    val actual = service.delete(*ids)

    // verify
    actual.test().verifyComplete()
    verify {
      dao.findPuids(*ids)
      service.verifyAuthorize(puid, Delete)
      dao.delete(*ids)
    }
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val ids = Array(3) { randomString() }
    val puid = randomString()
    every { dao.findPuids(*ids) } returns Flux.just(Optional.of(puid))
    every { service.verifyAuthorize(puid, Delete) } returns Mono.error(PermissionDeniedException())

    // invoke
    val actual = service.delete(*ids)

    // verify
    actual.test().verifyError(PermissionDeniedException::class.java)
    verify {
      dao.findPuids(*ids)
      service.verifyAuthorize(puid, Delete)
    }
  }

  @Test
  fun failedByForbidden() {
    // mock
    val ids = Array(3) { randomString() }
    val puids = Array(2) { Optional.of(randomString()) }
    every { dao.findPuids(*ids) } returns puids.toFlux()

    // invoke
    val actual = service.delete(*ids)

    // verify
    actual.test().verifyError(ForbiddenException::class.java)
    verify { dao.findPuids(*ids) }
  }
}