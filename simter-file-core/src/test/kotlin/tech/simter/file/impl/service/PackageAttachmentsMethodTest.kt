package tech.simter.file.impl.service

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
import reactor.core.publisher.toFlux
import reactor.test.StepVerifier
import tech.simter.exception.ForbiddenException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.domain.AttachmentDto4Zip
import tech.simter.file.impl.service.AttachmentServiceImpl.OperationType.Read
import tech.simter.reactive.security.ReactiveSecurityService
import tech.simter.util.RandomUtils.randomString
import java.io.*
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Test [AttachmentService.packageAttachments]
 *
 * @author zh
 */
@SpringBootTest(classes = [ModuleConfiguration::class])
@MockBean(AttachmentDao::class, ReactiveSecurityService::class)
@SpyBean(AttachmentServiceImpl::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
class PackageAttachmentsMethodTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val service: AttachmentServiceImpl,
  @Value("\${simter.file.root}") private val fileRootDir: String
) {
  @AfterEach
  fun clean() {
    File(fileRootDir).deleteRecursively()
  }

  @Test
  fun packageAttachments() {
    //      physical
    // |__ {fileRootDir}
    //   |__ path1
    //     |__ file1.txt  --context: "path1/file1.txt"
    //     |__ path2
    //   |__ file2.txt    --context: "file2.txt"
    val dtos = listOf(
      randomAttachmentDto4Zip(zipPath = "zip-path1", physicalPath = "path1", type = ":d"),
      randomAttachmentDto4Zip(zipPath = "zip-path1/zip-file1", physicalPath = "path1/file1.txt", type = "txt"),
      randomAttachmentDto4Zip(zipPath = "zip-path1/zip-path2", physicalPath = "path1/path2", type = ":d"),
      randomAttachmentDto4Zip(zipPath = "file2", physicalPath = "file2.txt", type = "txt"),
      randomAttachmentDto4Zip(zipPath = "zip-file1", physicalPath = "path1/file1.txt", type = "txt")
    )
    dtos.subList(0, 4).forEach {
      if (it.type == ":d") {
        File("$fileRootDir/${it.physicalPath!!}").mkdirs()
      } else {
        File("$fileRootDir/${it.physicalPath!!}").run {
          createNewFile()
          val stream = FileOutputStream(this)
          stream.write(it.physicalPath!!.toByteArray())
          stream.close()
        }
      }
    }

    // 1. attachments don't have least-common-ancestors
    // "root.zip"
    // |__ zip-path1
    // |__ zip-file1.txt
    verifyPackage(listOf(dtos[0], dtos[3]), "root.zip")

    // 2. attachments have least-common-ancestors and it is file
    // "zip-file1.txt.zip"
    // |__ zip-file1.txt
    reset(dao)
    reset(service)
    verifyPackage(listOf(dtos[4].apply { origin = terminus }), "zip-file1.txt.zip")

    // 3. attachments have least-common-ancestors and it is folder
    // "zip-path1.zip"
    // |__ zip-path1
    //   |__ zip-file1.txt
    //   |__ zip-path2
    reset(dao)
    reset(service)
    verifyPackage(dtos.subList(0, 3).map { it.apply { this.origin = dtos[0].terminus } }, "zip-path1.zip")
  }

  private fun randomAttachmentDto4Zip(zipPath: String, physicalPath: String, type: String): AttachmentDto4Zip {
    return AttachmentDto4Zip().also {
      it.terminus = randomString()
      it.zipPath = zipPath
      it.physicalPath = physicalPath
      it.type = type
    }
  }

  private fun verifyPackage(dtos: List<AttachmentDto4Zip>, zipName: String) {
    // prepare data and file
    val outputStream = ByteArrayOutputStream()
    val ids = dtos.map { it.terminus!! }.toTypedArray()
    val puid = randomString()
    `when`(dao.findDescendentsZipPath(*ids)).thenReturn(dtos.toFlux())
    `when`(dao.findPuids(*ids)).thenReturn(Flux.just(Optional.of(puid)))
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize(puid, Read)

    // invoke request
    val actual = service.packageAttachments(outputStream, *ids)

    // verify method service.get invoked
    StepVerifier.create(actual).expectNext(zipName).verifyComplete()
    verify(dao).findDescendentsZipPath(*ids)
    verify(dao).findPuids(*ids)
    verify(service).verifyAuthorize(puid, Read)

    // verify zip file
    val zip = ZipInputStream(ByteArrayInputStream(outputStream.toByteArray()))
    val date = BufferedReader(InputStreamReader(zip))
    dtos.forEach {
      if (it.type == ":d") {
        assertEquals("${it.zipPath!!}/", zip.nextEntry.name)
        zip.closeEntry()
      } else {
        assertEquals("${it.zipPath!!}.${it.type}", zip.nextEntry.name)
        assertEquals(it.physicalPath, date.readLine())
        zip.closeEntry()
      }
    }
    assertNull(zip.nextEntry)
    zip.close()
    date.close()
    outputStream.close()
  }

  @Test
  fun packageNoAttachment() {
    // mock
    val outputStream = ByteArrayOutputStream()
    val ids = Array(3) { randomString() }
    val puid = randomString()
    `when`(dao.findDescendentsZipPath(*ids)).thenReturn(Flux.empty())
    `when`(dao.findPuids(*ids)).thenReturn(Flux.just(Optional.of(puid)))
    doReturn(Mono.empty<Void>()).`when`(service).verifyAuthorize(puid, Read)

    // invoke
    val actual = service.packageAttachments(outputStream, *ids)

    // verify
    StepVerifier.create(actual).verifyComplete()
    verify(dao).findDescendentsZipPath(*ids)
    verify(dao).findPuids(*ids)
    verify(service).verifyAuthorize(puid, Read)
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val outputStream = ByteArrayOutputStream()
    val ids = Array(3) { randomString() }
    val puid = randomString()
    `when`(dao.findPuids(*ids)).thenReturn(Flux.just(Optional.of(puid)))
    doReturn(Mono.error<Void>(PermissionDeniedException())).`when`(service).verifyAuthorize(puid, Read)

    // invoke
    val actual = service.packageAttachments(outputStream, *ids)

    // verify
    StepVerifier.create(actual).verifyError(PermissionDeniedException::class.java)
    verify(dao).findPuids(*ids)
    verify(service).verifyAuthorize(puid, Read)
  }

  @Test
  fun failedByForbidden() {
    // mock
    val outputStream = ByteArrayOutputStream()
    val ids = Array(3) { randomString() }
    val puids = Array(2) { Optional.of(randomString()) }
    `when`(dao.findPuids(*ids)).thenReturn(puids.toFlux())

    // invoke
    val actual = service.packageAttachments(outputStream, *ids)

    // verify
    StepVerifier.create(actual).verifyError(ForbiddenException::class.java)
    verify(dao).findPuids(*ids)
  }
}