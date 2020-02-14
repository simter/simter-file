package tech.simter.file.impl.dao.reactive.mongo

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.util.FileCopyUtils
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import tech.simter.exception.NotFoundException
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.domain.AttachmentDto4Update
import tech.simter.file.core.domain.AttachmentDto4Zip
import tech.simter.file.core.domain.AttachmentDtoWithChildren
import tech.simter.file.core.domain.Attachment
import java.io.File
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.stream.IntStream

fun AttachmentDtoWithChildren.getOwnData(): Map<String, Any?> {
  return data.filter { it.key != "children" }
}

/**
 * Test [AttachmentDaoImpl]
 *
 * @author cjw
 * @author RJ
 * @author zh
 */
@SpringJUnitConfig(ModuleConfiguration::class)
@DataMongoTest
@TestPropertySource(properties = ["simter.file.root=target/files"])
class AttachmentDaoImplTest @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val dao: AttachmentDao,
  private val operations: ReactiveMongoOperations
) {
  private val path = "/data"
  private val creator = "Simter"
  private val now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)

  @BeforeEach
  fun setup() {
    // drop and create a new collection
    StepVerifier.create(
      operations.collectionExists(Attachment::class.java)
        .flatMap { if (it) operations.dropCollection(Attachment::class.java) else Mono.just(it) }
        .then(operations.createCollection(Attachment::class.java))
    ).expectNextCount(1).verifyComplete()
  }

  @Test
  fun get() {
    // verify not exists
    val id = UUID.randomUUID().toString()
    StepVerifier.create(dao.get(id)).expectNextCount(0L).verifyComplete()

    // prepare data
    val po = Attachment(id, path, "Sample", "png", 123, now, creator, now, creator)
    StepVerifier.create(operations.insert(po)).expectNextCount(1).verifyComplete()

    // verify exists
    StepVerifier.create(dao.get(id)).expectNext(po).verifyComplete()
  }

  @Test
  fun findByPageable() {
    val pageable = PageRequest.of(0, 25)

    // 1. not found
    StepVerifier.create(dao.find(pageable))
      .expectNext(Page.empty<Attachment>(pageable))
      .verifyComplete()

    // 2. found
    // 2.1 prepare data
    val pos = (1..3).map {
      Attachment(it.toString(), path, "Sample$it", "png", 123, now, creator, now, creator)
    }
    StepVerifier.create(operations.insertAll(pos)).expectNextCount(pos.size.toLong()).verifyComplete()

    // 2.2 invoke
    val actual = dao.find(pageable)

    // 2.3 verify
    StepVerifier.create(actual)
      .consumeNextWith { page -> assertEquals(pos.size, page.content.size) }
      .verifyComplete()
  }

  @Test
  fun findByModuleAndSubgroup() {
    // 1. mock
    val puid = "puid1"
    val subgroup = "1"
    val now = OffsetDateTime.now()
    val origin = (1..3).map {
      Attachment(id = it.toString(), path = path, name = "Sample$it", type = "png", size = 123,
        createOn = now, creator = creator, puid = puid, upperId = it.toString(), modifyOn = now, modifier = creator
      )
    }
    Collections.reverse(origin)

    // 2. not found: empty list
    StepVerifier.create(dao.find(puid, subgroup).collectList())
      .consumeNextWith { Assertions.assertTrue(it.isEmpty()) }
      .verifyComplete()

    // 3. init data
    StepVerifier.create(operations.insertAll(origin)).expectNextCount(origin.size.toLong()).verifyComplete()

    // 4. found all data in module
    StepVerifier.create(dao.find(puid, null).collectList())
      .consumeNextWith { actual ->
        assertEquals(actual.size, origin.size)
        IntStream.range(0, actual.size).forEach {
          assertEquals(actual[it].id, origin[it].id)
        }
      }.verifyComplete()

    // 5. found all data in module and upperId
    StepVerifier.create(dao.find(puid, subgroup).collectList())
      .consumeNextWith {
        assertEquals(it[0].id, origin.find { it.puid == puid && it.upperId == subgroup }?.id)
      }.verifyComplete()
  }

  @Test
  fun saveOne() {
    val po = Attachment(UUID.randomUUID().toString(), path, "Sample", "png",
      123, now, creator, now, creator)
    val actual = dao.save(po)

    // verify result
    StepVerifier.create(actual).expectNextCount(0L).verifyComplete()

    // verify saved
    StepVerifier.create(operations.findById(po.id, Attachment::class.java))
      .expectNext(po)
      .verifyComplete()
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
    StepVerifier.create(operations.insertAll(listOf(po100, po110, po111, po112, po120, po121, po122, po123)))
      .expectNextCount(8).verifyComplete()

    // invoke and verify
    StepVerifier.create(dao.delete("110", "121"))
      .expectNext("path100/path110")
      .expectNext("path100/path120/path121.xml").verifyComplete()
    StepVerifier.create(operations.findAll(Attachment::class.java).collectList())
      .expectNext(listOf(po100, po120, po122, po123))
      .verifyComplete()
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
    val pos = listOf(po1, po2, po3)
    StepVerifier.create(operations.insertAll(pos)).expectNextCount(3).verifyComplete()

    // invoke and verify
    StepVerifier.create(dao.getFullPath(po3.id))
      .expectNext(pos.joinToString("/") { it.path }).verifyComplete()
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
  fun notFoundDescendents() {
    // prepare data
    val now = OffsetDateTime.now()
    val po = Attachment(UUID.randomUUID().toString(), "/data1", "Sample1", "png",
      123, now, "Simter", now, "Simter")
    StepVerifier.create(operations.insert(po)).expectNextCount(1).verifyComplete()

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
    StepVerifier.create(operations.insertAll(listOf(po100, po110, po120, po111, po112, po121, po122, po123)))
      .expectNextCount(8).verifyComplete()

    // invoke and verify
    StepVerifier.create(dao.findDescendents(po100.id)).consumeNextWith { actual ->
      assertEquals(AttachmentDtoWithChildren().copy(po110).getOwnData(), actual.getOwnData())
      assertEquals(AttachmentDtoWithChildren().copy(po111).getOwnData(), actual.children!![0].getOwnData())
      assertEquals(AttachmentDtoWithChildren().copy(po112).getOwnData(), actual.children!![1].getOwnData())
    }.consumeNextWith { actual ->
      assertEquals(AttachmentDtoWithChildren().copy(po120).getOwnData(), actual.getOwnData())
      assertEquals(AttachmentDtoWithChildren().copy(po121).getOwnData(), actual.children!![0].getOwnData())
      assertEquals(AttachmentDtoWithChildren().copy(po122).getOwnData(), actual.children!![1].getOwnData())
      assertEquals(AttachmentDtoWithChildren().copy(po123).getOwnData(), actual.children!![2].getOwnData())
    }.verifyComplete()
  }

  @Test
  fun update() {
    // prepare data
    val now = OffsetDateTime.now()
    val po = Attachment(UUID.randomUUID().toString(), "/data1", "Sample1", "png",
      123, now, "Simter", now, "Simter")
    StepVerifier.create(operations.insert(po)).expectNextCount(1).verifyComplete()
    val dto = AttachmentDto4Update().apply {
      name = "newName"
      path = "/new-data"
      upperId = null
    }

    // invoke and verify
    StepVerifier.create(dao.update(po.id, dto.data)).verifyComplete()
    StepVerifier.create(operations.find(Query.query(Criteria.where("id").`is`(po.id)), Attachment::class.java))
      .expectNext(po.copy(name = dto.name!!, path = dto.path!!, upperId = null))
      .verifyComplete()
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
    StepVerifier.create(operations.insertAll(listOf(po100, po110, po120, po111, po112, po121, po122, po123, po200)))
      .expectNextCount(9).verifyComplete()

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
            physicalPath = "path100/path110"
            zipPath = "name110"
            type = ":d"
            origin = "110"
            id = "\"110\"-\"110\""
          },
          AttachmentDto4Zip().apply {
            terminus = "111"
            physicalPath = "path100/path110/path111.xml"
            zipPath = "name110/name111"
            type = "xml"
            origin = "110"
            id = "\"110\"-\"111\""
          },
          AttachmentDto4Zip().apply {
            terminus = "112"
            physicalPath = "path100/path110/path112.xml"
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
            physicalPath = "path100/path110/path111.xml"
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
            physicalPath = "path100/path110"
            zipPath = "name110"
            type = ":d"
            origin = "110"
            id = "\"110\"-\"110\""
          },
          AttachmentDto4Zip().apply {
            terminus = "111"
            physicalPath = "path100/path110/path111.xml"
            zipPath = "name110/name111"
            type = "xml"
            origin = "110"
            id = "\"110\"-\"111\""
          },
          AttachmentDto4Zip().apply {
            terminus = "112"
            physicalPath = "path100/path110/path112.xml"
            zipPath = "name110/name112"
            type = "xml"
            origin = "110"
            id = "\"110\"-\"112\""
          }
        ), it)
      }.verifyComplete()
  }

  @Test
  fun findPuids() {
    // prepare data
    val now = OffsetDateTime.now()
    val pos = List(10) {
      Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png",
        123, now, "Simter", now, "Simter", puid = if (it < 2) null else "puid${it / 2}")
    }
    StepVerifier.create(operations.insertAll(pos)).expectNextCount(10).verifyComplete()

    // invoke and verify
    StepVerifier.create(dao.findPuids(*pos.map { it.id }.toTypedArray()).collectList().map { it.toSet() })
      .expectNext(pos.map { Optional.ofNullable(it.puid) }.toSet())
      .verifyComplete()
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