package tech.simter.file.dao.jpa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.util.FileCopyUtils
import reactor.test.StepVerifier
import tech.simter.exception.NotFoundException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.dto.AttachmentDto4Update
import tech.simter.file.dto.AttachmentDto4Zip
import tech.simter.file.dto.AttachmentDtoWithChildren
import tech.simter.file.po.Attachment
import java.io.File
import java.time.OffsetDateTime
import java.util.*
import java.util.stream.IntStream
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.collections.ArrayList
import kotlin.test.assertFalse

/**
 * @author RJ
 */
@SpringJUnitConfig(ModuleConfiguration::class)
@DataJpaTest
@TestPropertySource(properties = ["simter.file.root=target/files"])
class AttachmentDaoImplTest @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  @PersistenceContext val em: EntityManager,
  val dao: AttachmentDao
) {
  @Test
  fun get() {
    // verify not exists
    StepVerifier.create(dao.get(UUID.randomUUID().toString()))
      .expectNextCount(0L)
      .verifyComplete()

    // prepare data
    val now = OffsetDateTime.now()
    val po = Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png",
      123, now, "Simter", now, "Simter")
    em.persist(po)
    em.flush()
    em.clear()

    // verify exists
    StepVerifier.create(dao.get(po.id))
      .expectNext(po)
      .verifyComplete()
  }

  @Test
  fun findByPageable() {
    // 1. not found: empty page
    StepVerifier.create(dao.find(PageRequest.of(0, 25)))
      .consumeNextWith { page ->
        assertTrue(page.content.isEmpty())
        assertEquals(0, page.number)
        assertEquals(25, page.size)
        assertEquals(0, page.totalPages)
        assertEquals(0, page.totalElements)
      }
      .verifyComplete()

    // 2. found: page with content
    // 2.1 prepare data
    val now = OffsetDateTime.now()
    val po1 = Attachment(UUID.randomUUID().toString(), "/data1", "Sample", "png", 123,
      now.minusDays(1), "Simter", now.minusDays(1), "Simter")
    val po2 = Attachment(UUID.randomUUID().toString(), "/data2", "Sample", "png", 123,
      now, "Simter", now, "Simter")
    em.persist(po1)
    em.persist(po2)
    em.flush()
    em.clear()

    // 2.2 invoke
    val actual = dao.find(PageRequest.of(0, 25, Sort.by(DESC, "createOn")))

    // 2.3 verify
    StepVerifier.create(actual)
      .consumeNextWith { page ->
        assertEquals(0, page.number)
        assertEquals(25, page.size)
        assertEquals(1, page.totalPages)
        assertEquals(2L, page.totalElements)
        assertEquals(2, page.content.size)
        assertEquals(po2, page.content[0])
        assertEquals(po1, page.content[1])
      }
      .verifyComplete()
  }

  @Test
  fun findByModuleAndSubgroup() {
    // 1. mock
    val puid = "puid1"
    val subgroup = "1"
    val now = OffsetDateTime.now()
    val origin = ArrayList<Attachment>()

    // 2. not found: empty list
    StepVerifier.create(dao.find(puid, subgroup).collectList())
      .consumeNextWith { assertTrue(it.isEmpty()) }
      .verifyComplete()

    // 3. init data
    IntStream.range(0, 5).forEach {
      val po = Attachment(id = UUID.randomUUID().toString(), path = "/data", name = "Sample", type = "png", size = 123,
        createOn = now.minusDays(it.toLong()), creator = "Simter", puid = "puid1", upperId = it.toString(),
        modifyOn = now.minusDays(it.toLong()), modifier = "Simter")
      em.persist(po)
      origin.add(po)
    }
    em.flush()
    em.clear()
    origin.sortBy { it.createOn }
    origin.reverse()

    // 4. found all data by module
    StepVerifier.create(dao.find(puid, null).collectList())
      .consumeNextWith { actual ->
        assertEquals(actual.size, origin.size)
        IntStream.range(0, actual.size).forEach {
          assertEquals(actual[it].id, origin[it].id)
        }
      }.verifyComplete()

    // 5. found all data by module and upperId
    StepVerifier.create(dao.find(puid, subgroup).collectList())
      .consumeNextWith {
        assertEquals(it[0].id, origin.find { it.puid == puid && it.upperId == subgroup }?.id)
      }.verifyComplete()
  }

  @Test
  fun saveNone() {
    StepVerifier.create(dao.save()).expectNextCount(0L).verifyComplete()
  }

  @Test
  fun saveOne() {
    // invoke
    val now = OffsetDateTime.now()
    val po = Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png",
      123, now, "Simter", now, "Simter")
    val actual = dao.save(po)

    // verify result
    StepVerifier.create(actual).expectNextCount(0L).verifyComplete()

    // verify saved
    assertEquals(
      po,
      em.createQuery("select a from Attachment a where id = :id").setParameter("id", po.id).singleResult
    )
  }

  @Test
  fun saveMulti() {
    val now = OffsetDateTime.now()
    val pos = (1..3).map {
      Attachment(UUID.randomUUID().toString(), "/data$it", "Sample-$it", "png",
        123, now, "Simter", now, "Simter")
    }
    val actual = dao.save(*pos.toTypedArray())

    // verify result
    StepVerifier.create(actual).expectNextCount(0L).verifyComplete()

    // verify saved
    pos.forEach {
      assertEquals(
        it,
        em.createQuery("select a from Attachment a where id = :id", Attachment::class.java)
          .setParameter("id", it.id).singleResult
      )
    }
  }

  @Test
  fun delete() {
    // prepare data
    //            po100
    //       /            \
    //    po110          po120
    //   /     \      /    |     \
    // po111 po112  po121 po122 po123
    val now = OffsetDateTime.now()
    val basicPo = Attachment(id = UUID.randomUUID().toString(), path = "", name = "Sample100", type = ":d",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = null)
    val po100 = basicPo.copy(id = "100", upperId = null, path = "path100")
    val po110 = basicPo.copy(id = "110", upperId = "100", path = "path110")
    val po120 = basicPo.copy(id = "120", upperId = "100", path = "path120")
    val po111 = basicPo.copy(id = "111", upperId = "110", path = "path111.xml")
    val po112 = basicPo.copy(id = "112", upperId = "110", path = "path112.xml")
    val po121 = basicPo.copy(id = "121", upperId = "120", path = "path121.xml")
    val po122 = basicPo.copy(id = "122", upperId = "120", path = "path122.xml")
    val po123 = basicPo.copy(id = "123", upperId = "120", path = "path123.xml")
    listOf(po100, po110, po111, po112, po120, po121, po122, po123).forEach {
      em.persist(it)
    }
    em.flush()

    // prepare file
    File(fileRootDir).deleteRecursively()
    val file100 = File("$fileRootDir/${po100.path}").apply { mkdir() }
    val file110 = File("$fileRootDir/${po100.path}/${po110.path}").apply { mkdirs() }
    val file120 = File("$fileRootDir/${po100.path}/${po120.path}").apply { mkdirs() }
    val file111 = File("$fileRootDir/${po100.path}/${po110.path}/${po111.path}").apply { createNewFile() }
    val file112 = File("$fileRootDir/${po100.path}/${po110.path}/${po112.path}").apply { createNewFile() }
    val file121 = File("$fileRootDir/${po100.path}/${po120.path}/${po121.path}").apply { createNewFile() }
    val file122 = File("$fileRootDir/${po100.path}/${po120.path}/${po122.path}").apply { createNewFile() }
    val file123 = File("$fileRootDir/${po100.path}/${po120.path}/${po123.path}").apply { createNewFile() }


    StepVerifier.create(dao.delete("110", "121")).verifyComplete()
    // 1. Verify attachments and all its descendants
    val newPo = em.createQuery("select a from Attachment a", Attachment::class.java).resultList
    assertEquals(4, newPo.size)
    assertEquals(listOf("100", "120", "122", "123"), newPo.map { it.id })

    // 2. Verify delete physical file
    assertTrue(file100.exists())
    assertFalse(file110.exists())
    assertTrue(file120.exists())
    assertFalse(file111.exists())
    assertFalse(file112.exists())
    assertFalse(file121.exists())
    assertTrue(file122.exists())
    assertTrue(file123.exists())
  }

  @Test
  fun deleteNoneAndNotExists() {
    // none
    StepVerifier.create(dao.delete()).verifyComplete()

    // delete not exists id
    StepVerifier.create(dao.delete(UUID.randomUUID().toString())).verifyComplete()
  }

  @Test
  fun notFoundFullPath() {
    // invoke and verify
    StepVerifier.create(dao.getFullPath(UUID.randomUUID().toString())).verifyComplete()
  }

  @Test
  fun getFullPath() {
    // prepare data
    val now = OffsetDateTime.now()
    val po1 = Attachment(id = UUID.randomUUID().toString(), path = "data-1", name = "Sample-1", type = ":d", size = 123,
      createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = null)
    val po2 = Attachment(id = UUID.randomUUID().toString(), path = "data-2", name = "Sample-2", type = ":d", size = 123,
      createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = po1.id)
    val po3 = Attachment(id = UUID.randomUUID().toString(), path = "data-3", name = "Sample-3", type = ":d", size = 123,
      createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = po2.id)
    em.persist(po1)
    em.persist(po2)
    em.persist(po3)
    em.flush()

    // invoke and verify
    StepVerifier.create(dao.getFullPath(po3.id))
      .expectNext(listOf(po1, po2, po3).joinToString("/") { it.path }).verifyComplete()
  }

  @Test
  fun notFoundDescendentsZipPath() {
    // nothing
    StepVerifier.create(dao.findDescendentsZipPath()).verifyComplete()

    // none found
    StepVerifier.create(dao.findDescendentsZipPath(UUID.randomUUID().toString())).verifyComplete()
    StepVerifier.create(dao.findDescendentsZipPath(*Array(3) { UUID.randomUUID().toString() })).verifyComplete()
  }

  @Test
  fun findDescendentsZipPath() {
    // prepare data
    //            po100                     po200
    //       /            \
    //    po110          po120
    //   /     \      /    |     \
    // po111 po112  po121 po122 po123
    val now = OffsetDateTime.now()
    val basicPo = Attachment(id = UUID.randomUUID().toString(), path = "", name = "", type = "",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = null)
    val po100 = basicPo.copy(id = "100", upperId = null, path = "path100", name = "name100", type = ":d")
    val po110 = basicPo.copy(id = "110", upperId = "100", path = "path110", name = "name110", type = ":d")
    val po120 = basicPo.copy(id = "120", upperId = "100", path = "path120", name = "name120", type = ":d")
    val po111 = basicPo.copy(id = "111", upperId = "110", path = "path111.xml", name = "name111", type = "xml")
    val po112 = basicPo.copy(id = "112", upperId = "110", path = "path112.xml", name = "name112", type = "xml")
    val po121 = basicPo.copy(id = "121", upperId = "120", path = "path121.xml", name = "name121", type = "xml")
    val po122 = basicPo.copy(id = "122", upperId = "120", path = "path122.xml", name = "name122", type = "xml")
    val po123 = basicPo.copy(id = "123", upperId = "120", path = "path123.xml", name = "name123", type = "xml")
    val po200 = basicPo.copy(id = "200", upperId = null, path = "path200.xml", name = "name200", type = "xml")
    listOf(po100, po110, po111, po112, po120, po121, po122, po123, po200).forEach {
      em.persist(it)
    }
    em.flush()

    // Invoke and verify

    // verify least-common-ancestor's id is null
    StepVerifier.create(dao.findDescendentsZipPath("111", "200").collectList())
      .consumeNextWith {
        assertEquals(listOf(
          AttachmentDto4Zip().apply {
            terminus = "111"
            physicalPath = "path100/path110/path111.xml"
            zipPath = "name100/name110/name111"
            type = "xml"
            origin = null
            id = "null-\"111\""
          },
          AttachmentDto4Zip().apply {
            terminus = "200"
            physicalPath = "path200.xml"
            zipPath = "name200"
            type = "xml"
            origin = null
            id = "null-\"200\""
          }
        ), it)
      }.verifyComplete()
    // verify least-common-ancestor's id in parameter ids
    StepVerifier.create(dao.findDescendentsZipPath("110", "111").collectList())
      .consumeNextWith {
        assertEquals(listOf(
          AttachmentDto4Zip().apply {
            terminus = "110"
            physicalPath = "path110"
            zipPath = "name110"
            type = ":d"
            origin = "110"
            id = "\"110\"-\"110\""
          },
          AttachmentDto4Zip().apply {
            terminus = "111"
            physicalPath = "path110/path111.xml"
            zipPath = "name110/name111"
            type = "xml"
            origin = "110"
            id = "\"110\"-\"111\""
          },
          AttachmentDto4Zip().apply {
            terminus = "112"
            physicalPath = "path110/path112.xml"
            zipPath = "name110/name112"
            type = "xml"
            origin = "110"
            id = "\"110\"-\"112\""
          }
        ), it)
      }.verifyComplete()
    // verify least-common-ancestor's id not in parameter ids
    StepVerifier.create(dao.findDescendentsZipPath("111", "121").collectList())
      .consumeNextWith {
        assertEquals(listOf(
          AttachmentDto4Zip().apply {
            terminus = "111"
            physicalPath = "path100/path110/path111.xml"
            zipPath = "name100/name110/name111"
            type = "xml"
            origin = "100"
            id = "\"100\"-\"111\""
          },
          AttachmentDto4Zip().apply {
            terminus = "121"
            physicalPath = "path100/path120/path121.xml"
            zipPath = "name100/name120/name121"
            type = "xml"
            origin = "100"
            id = "\"100\"-\"121\""
          }
        ), it)
      }.verifyComplete()
    // verify parameter ids is single id and it is a file
    StepVerifier.create(dao.findDescendentsZipPath("111").collectList())
      .consumeNextWith {
        assertEquals(listOf(
          AttachmentDto4Zip().apply {
            terminus = "111"
            physicalPath = "path111.xml"
            zipPath = "name111"
            type = "xml"
            origin = "111"
            id = "\"111\"-\"111\""
          }
        ), it)
      }.verifyComplete()
    // verify parameter ids is single id and it is a folder
    StepVerifier.create(dao.findDescendentsZipPath("110").collectList())
      .consumeNextWith {
        assertEquals(listOf(
          AttachmentDto4Zip().apply {
            terminus = "110"
            physicalPath = "path110"
            zipPath = "name110"
            type = ":d"
            origin = "110"
            id = "\"110\"-\"110\""
          },
          AttachmentDto4Zip().apply {
            terminus = "111"
            physicalPath = "path110/path111.xml"
            zipPath = "name110/name111"
            type = "xml"
            origin = "110"
            id = "\"110\"-\"111\""
          },
          AttachmentDto4Zip().apply {
            terminus = "112"
            physicalPath = "path110/path112.xml"
            zipPath = "name110/name112"
            type = "xml"
            origin = "110"
            id = "\"110\"-\"112\""
          }
        ), it)
      }.verifyComplete()
  }

  @Test
  fun updateByNone() {
    // prepare data
    val dto = AttachmentDto4Update().apply {
      name = "newName"
      path = "/new-data"
    }

    // invoke and verify
    StepVerifier.create(dao.update(UUID.randomUUID().toString(), dto.data)).verifyError(NotFoundException::class.java)
  }

  @Test
  fun update() {
    // prepare data
    val now = OffsetDateTime.now()
    val po = Attachment(UUID.randomUUID().toString(), "/data1", "Sample1", "png",
      123, now, "Simter", now, "Simter")
    em.persist(po)
    em.flush()
    val dto = AttachmentDto4Update().apply {
      name = "newName"
      path = "/new-data"
    }

    // invoke and verify
    StepVerifier.create(dao.update(po.id, dto.data)).verifyComplete()
    assertEquals(po.copy(name = dto.name!!, path = dto.path!!), em.find(Attachment::class.java, po.id))
  }

  @Test
  fun notFoundDescendents() {
    // prepare data
    val now = OffsetDateTime.now()
    val po = Attachment(UUID.randomUUID().toString(), "/data1", "Sample1", "png",
      123, now, "Simter", now, "Simter")
    em.persist(po)
    em.flush()

    // invoke and verify
    // none attachment
    StepVerifier.create(dao.findDescendents(UUID.randomUUID().toString())).verifyComplete()
    // none descendents
    StepVerifier.create(dao.findDescendents(po.id)).verifyComplete()
  }

  @Test
  fun findDescendents() {
    // prepare data
    //            po100
    //       /            \
    //    po110          po120
    //   /     \      /    |     \
    // po111 po112  po121 po122 po123
    val now = OffsetDateTime.now()
    val po100 = Attachment(id = "100", path = "data100", name = "Sample100", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = null)
    val po110 = Attachment(id = "110", path = "data110", name = "Sample110", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = "100")
    val po120 = Attachment(id = "120", path = "data120", name = "Sample120", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = "100")
    val po111 = Attachment(id = "111", path = "data111", name = "Sample111", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = "110")
    val po112 = Attachment(id = "112", path = "data112", name = "Sample112", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = "110")
    val po121 = Attachment(id = "121", path = "data121", name = "Sample121", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = "120")
    val po122 = Attachment(id = "122", path = "data122", name = "Sample122", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = "120")
    val po123 = Attachment(id = "123", path = "data123", name = "Sample123", type = "png",
      size = 123, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", upperId = "120")
    listOf(po100, po110, po111, po112, po120, po121, po122, po123).forEach {
      em.persist(it)
    }
    em.flush()

    // invoke and verify
    StepVerifier.create(dao.findDescendents(po100.id)).consumeNextWith { actual ->
      val expected = AttachmentDtoWithChildren().copy(po110).also { it.children = actual.children }
      assertEquals(expected, actual)
      assertEquals(AttachmentDtoWithChildren().copy(po111), actual.children!![0])
      assertEquals(AttachmentDtoWithChildren().copy(po112), actual.children!![1])
    }.consumeNextWith { actual ->
      val expected = AttachmentDtoWithChildren().copy(po120).also { it.children = actual.children }
      assertEquals(expected, actual)
      assertEquals(AttachmentDtoWithChildren().copy(po121), actual.children!![0])
      assertEquals(AttachmentDtoWithChildren().copy(po122), actual.children!![1])
      assertEquals(AttachmentDtoWithChildren().copy(po123), actual.children!![2])
    }.verifyComplete()
  }

  /** build test file method */
  private fun buildTestFiles(attachments: List<Attachment>) {
    attachments.forEach {
      val file = File("$fileRootDir/${it.path}")
      val parentFile = file.parentFile
      if (!parentFile.exists()) parentFile.mkdirs()
      FileCopyUtils.copy(ClassPathResource("banner.txt").file.readBytes(), file)
    }
  }
}