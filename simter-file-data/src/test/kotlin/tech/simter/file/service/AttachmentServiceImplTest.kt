package tech.simter.file.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import tech.simter.exception.NotFoundException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.dto.AttachmentDto4Zip
import tech.simter.file.dto.AttachmentDtoWithChildren
import tech.simter.file.po.Attachment
import tech.simter.reactive.security.ReactiveSecurityService
import java.io.*
import java.time.OffsetDateTime
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test [AttachmentService]
 *
 * @author RJ
 * @author zh
 */
@SpringBootTest(classes = [AttachmentServiceImpl::class, ModuleConfiguration::class])
@MockBean(AttachmentDao::class, ReactiveSecurityService::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
class AttachmentServiceImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val service: AttachmentService,
  @Value("\${simter.file.root}") private val fileRootDir: String
) {
  @Test
  fun get() {
    // mock
    val id: String = UUID.randomUUID().toString()
    val now = OffsetDateTime.now()
    val attachment = Attachment(id, "/data", "Sample", "png", 123, now,
      "Simter", now, "Simter")
    val expected = Mono.just(attachment)
    `when`(dao.get(id)).thenReturn(expected)

    // invoke
    val actual = service.get(id)

    // verify
    StepVerifier.create(actual).expectNext(attachment).verifyComplete()
    verify(dao).get(id)
  }

  @Test
  fun findByPageable() {
    // mock
    val pageable: Pageable = PageRequest.of(0, 25)
    val expect: Page<Attachment> = Page.empty()
    `when`(dao.find(pageable)).thenReturn(Mono.just(expect))

    // invoke
    val actual = service.find(pageable)

    // verify
    StepVerifier.create(actual).expectNext(expect).verifyComplete()
    verify(dao).find(pageable)
  }

  @Test
  fun findByModuleAndSubgroup() {
    // mock
    val puid = "puid1"
    val subgroup = "1"
    val expect = Collections.emptyList<Attachment>()
    `when`(dao.find(puid, subgroup)).thenReturn(Flux.fromIterable(expect))

    // invoke
    val actual = service.find(puid, subgroup)

    // verify
    StepVerifier.create(actual.collectList()).expectNext(expect).verifyComplete()
    verify(dao).find(puid, subgroup)
  }

  @Test
  fun getFullPath() {
    // mock
    val id = UUID.randomUUID().toString()
    val fullPath = "level1/level2/level3/level4"
    `when`(dao.getFullPath(id)).thenReturn(fullPath.toMono())

    // invoke
    val actual = service.getFullPath(id)

    // verify
    StepVerifier.create(actual).expectNext(fullPath).verifyComplete()
    verify(dao).getFullPath(id)
  }

  @Test
  fun getFullPathNotFound() {
    // mock
    val id = UUID.randomUUID().toString()
    `when`(dao.getFullPath(id)).thenReturn(Mono.empty())

    // invoke
    val actual = service.getFullPath(id)

    // verify
    StepVerifier.create(actual).verifyError(NotFoundException::class.java)
    verify(dao).getFullPath(id)
  }

  @Test
  fun findDescendents() {
    // mock
    val id = UUID.randomUUID().toString()
    val dtos = List(3) { index ->
      AttachmentDtoWithChildren().apply {
        this.id = UUID.randomUUID().toString()
        name = "name$index"
        type = "type$index"
        size = index.toLong()
        modifyOn = OffsetDateTime.now()
        modifier = "modifier$index"
      }
    }
    `when`(dao.findDescendents(id)).thenReturn(dtos.toFlux())

    // invoke
    val actual = service.findDescendents(id)

    // verify
    StepVerifier.create(actual.collectList()).expectNext(dtos).verifyComplete()
    verify(dao).findDescendents(id)
  }

  @Test
  fun create() {
    // mock
    val now = OffsetDateTime.now()
    val attachments = List(3) {
      Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png",
        123, now, "Simter", now, "Simter", upperId = UUID.randomUUID().toString())
    }
    val ids = attachments.map { it.id }
    `when`(dao.save(*attachments.toTypedArray())).thenReturn(Mono.empty())
    // invoke
    val actual = service.create(*attachments.toTypedArray())

    // verify
    StepVerifier.create(actual.collectList()).expectNext(ids).verifyComplete()
    verify(dao).save(*attachments.toTypedArray())
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
    verifyPackage(listOf(dtos[4].apply { origin = terminus }), "zip-file1.txt.zip")

    // 3. attachments have least-common-ancestors and it is folder
    // "zip-path1.zip"
    // |__ zip-path1
    //   |__ zip-file1.txt
    //   |__ zip-path2
    reset(dao)
    verifyPackage(dtos.subList(0, 3).map { it.apply { this.origin = dtos[0].terminus } }, "zip-path1.zip")
  }

  private fun randomAttachmentDto4Zip(zipPath: String, physicalPath: String, type: String): AttachmentDto4Zip {
    return AttachmentDto4Zip().also {
      it.terminus = UUID.randomUUID().toString()
      it.zipPath = zipPath
      it.physicalPath = physicalPath
      it.type = type
    }
  }

  private fun verifyPackage(dtos: List<AttachmentDto4Zip>, zipName: String) {
    // prepare data and file
    val outputStream = ByteArrayOutputStream()
    val ids = dtos.map { it.terminus!! }
    `when`(dao.findDescendentsZipPath(*ids.toTypedArray())).thenReturn(dtos.toFlux())

    // invoke request
    val actual = service.packageAttachments(outputStream, *ids.toTypedArray())

    // verify method service.get invoked
    StepVerifier.create(actual).expectNext(zipName).verifyComplete()
    verify(dao).findDescendentsZipPath(*ids.toTypedArray())

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
    val id = UUID.randomUUID().toString()
    `when`(dao.findDescendentsZipPath(id)).thenReturn(Flux.empty())

    // invoke
    val actual = service.packageAttachments(outputStream, id)

    // verify
    StepVerifier.create(actual).verifyComplete()
    verify(dao).findDescendentsZipPath(id)
  }

  @Test
  fun deleteNothing() {
    // mock
    val ids = List(3) { UUID.randomUUID().toString() }.toTypedArray()
    `when`(dao.delete(*ids)).thenReturn(Flux.empty())

    // invoke and verify
    StepVerifier.create(service.delete(*ids)).verifyComplete()
    verify(dao).delete(*ids)
  }

  @Test
  fun deleteSomething() {
    // mock data
    val folderPaths = listOf("path1", "path2")
    val filePaths = listOf("path1/file1.txt", "path3/file2.txt")
    val paths = folderPaths.plus(filePaths).toTypedArray()
    val ids = List(3) { UUID.randomUUID().toString() }.toTypedArray()
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

    // invoke and verify
    StepVerifier.create(service.delete(*ids)).verifyComplete()
    verify(dao).delete(*ids)
    assertTrue(folders.map { it.exists() }.run { dropLast(1).any { !it } && last() })
    assertTrue(files.map { it.exists() }.run { dropLast(1).any { !it } && last() })
  }
}