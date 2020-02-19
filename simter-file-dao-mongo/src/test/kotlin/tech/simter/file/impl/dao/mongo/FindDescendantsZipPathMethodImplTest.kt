package tech.simter.file.impl.dao.mongo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.core.domain.AttachmentDto4Zip
import tech.simter.file.core.domain.AttachmentDtoWithChildren
import tech.simter.file.impl.dao.mongo.TestHelper.randomAttachmentId
import tech.simter.file.impl.dao.mongo.po.AttachmentPo
import java.time.OffsetDateTime

fun AttachmentDtoWithChildren.getOwnData(): Map<String, Any?> {
  return data.filter { it.key != "children" }
}

/**
 * Test [AttachmentDaoImpl.findDescendantsZipPath].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@DataMongoTest
class FindDescendantsZipPathMethodImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val repository: AttachmentReactiveRepository
) {
  @Test
  fun notFoundDescendantsZipPath() {
    // nothing
    dao.findDescendantsZipPath().test().verifyComplete()

    // none found
    dao.findDescendantsZipPath(randomAttachmentId()).test().verifyComplete()
    dao.findDescendantsZipPath(*Array(3) { randomAttachmentId() }).test().verifyComplete()
  }

  @Test
  fun findDescendantsZipPath() {
    // prepare data
    //            po100                     po200
    //       /            \
    //    po110          po120
    //   /     \      /    |     \
    // po111 po112  po121 po122 po123
    val now = OffsetDateTime.now()
    val basicPo = AttachmentPo(id = randomAttachmentId(), path = "", name = "", type = "",
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
    val poList = listOf(po100, po110, po111, po112, po120, po121, po122, po123, po200)
    repository.saveAll(poList).test().expectNextCount(poList.size.toLong()).verifyComplete()

    // Invoke and verify

    // verify least-common-ancestor's id is null
    dao.findDescendantsZipPath("111", "200").collectList()
      .test()
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
    dao.findDescendantsZipPath("110", "111").collectList()
      .test()
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
    dao.findDescendantsZipPath("111", "121").collectList()
      .test()
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
    dao.findDescendantsZipPath("111").collectList()
      .test()
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
    dao.findDescendantsZipPath("110").collectList()
      .test()
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
}