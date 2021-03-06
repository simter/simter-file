package tech.simter.file.impl.dao.mongo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.mongo.TestHelper.cleanDatabase
import tech.simter.file.impl.dao.mongo.TestHelper.randomAttachmentPo
import tech.simter.file.impl.domain.AttachmentZipInfoImpl
import tech.simter.file.test.TestHelper.randomAttachmentId

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
    // clean
    cleanDatabase(repository)

    // prepare data
    //            po100                     po200
    //       /            \
    //    po110          po120
    //   /     \      /    |     \
    // po111 po112  po121 po122 po123
    val basic = randomAttachmentPo()
    val po100 = basic.copy(id = "100", upperId = null, path = "path100", name = "name100", type = ":d")
    val po110 = basic.copy(id = "110", upperId = "100", path = "path110", name = "name110", type = ":d")
    val po120 = basic.copy(id = "120", upperId = "100", path = "path120", name = "name120", type = ":d")
    val po111 = basic.copy(id = "111", upperId = "110", path = "path111.xml", name = "name111", type = "xml")
    val po112 = basic.copy(id = "112", upperId = "110", path = "path112.xml", name = "name112", type = "xml")
    val po121 = basic.copy(id = "121", upperId = "120", path = "path121.xml", name = "name121", type = "xml")
    val po122 = basic.copy(id = "122", upperId = "120", path = "path122.xml", name = "name122", type = "xml")
    val po123 = basic.copy(id = "123", upperId = "120", path = "path123.xml", name = "name123", type = "xml")
    val po200 = basic.copy(id = "200", upperId = null, path = "path200.xml", name = "name200", type = "xml")
    val poList = listOf(po100, po110, po111, po112, po120, po121, po122, po123, po200)
    repository.saveAll(poList).test().expectNextCount(poList.size.toLong()).verifyComplete()

    // Invoke and verify

    // verify least-common-ancestor's id is null
    dao.findDescendantsZipPath("111", "200").collectList()
      .test()
      .consumeNextWith {
        assertEquals(listOf(
          AttachmentZipInfoImpl(
            terminus = "111",
            physicalPath = "path100/path110/path111.xml",
            zipPath = "name100/name110/name111",
            type = "xml",
            origin = null,
            id = "null-\"111\""
          ),
          AttachmentZipInfoImpl(
            terminus = "200",
            physicalPath = "path200.xml",
            zipPath = "name200",
            type = "xml",
            origin = null,
            id = "null-\"200\""
          )
        ), it)
      }.verifyComplete()
    // verify least-common-ancestor's id in parameter ids
    dao.findDescendantsZipPath("110", "111").collectList()
      .test()
      .consumeNextWith {
        assertEquals(listOf(
          AttachmentZipInfoImpl(
            terminus = "110",
            physicalPath = "path100/path110",
            zipPath = "name110",
            type = ":d",
            origin = "110",
            id = "\"110\"-\"110\""
          ),
          AttachmentZipInfoImpl(
            terminus = "111",
            physicalPath = "path100/path110/path111.xml",
            zipPath = "name110/name111",
            type = "xml",
            origin = "110",
            id = "\"110\"-\"111\""
          ),
          AttachmentZipInfoImpl(
            terminus = "112",
            physicalPath = "path100/path110/path112.xml",
            zipPath = "name110/name112",
            type = "xml",
            origin = "110",
            id = "\"110\"-\"112\""
          )
        ), it)
      }.verifyComplete()
    // verify least-common-ancestor's id not in parameter ids
    dao.findDescendantsZipPath("111", "121").collectList()
      .test()
      .consumeNextWith {
        assertEquals(listOf(
          AttachmentZipInfoImpl(
            terminus = "111",
            physicalPath = "path100/path110/path111.xml",
            zipPath = "name100/name110/name111",
            type = "xml",
            origin = "100",
            id = "\"100\"-\"111\""
          ),
          AttachmentZipInfoImpl(
            terminus = "121",
            physicalPath = "path100/path120/path121.xml",
            zipPath = "name100/name120/name121",
            type = "xml",
            origin = "100",
            id = "\"100\"-\"121\""
          )
        ), it)
      }.verifyComplete()
    // verify parameter ids is single id and it is a file
    dao.findDescendantsZipPath("111").collectList()
      .test()
      .consumeNextWith {
        assertEquals(listOf(
          AttachmentZipInfoImpl(
            terminus = "111",
            physicalPath = "path100/path110/path111.xml",
            zipPath = "name111",
            type = "xml",
            origin = "111",
            id = "\"111\"-\"111\""
          )
        ), it)
      }.verifyComplete()
    // verify parameter ids is single id and it is a folder
    dao.findDescendantsZipPath("110").collectList()
      .test()
      .consumeNextWith {
        assertEquals(listOf(
          AttachmentZipInfoImpl(
            terminus = "110",
            physicalPath = "path100/path110",
            zipPath = "name110",
            type = ":d",
            origin = "110",
            id = "\"110\"-\"110\""
          ),
          AttachmentZipInfoImpl(
            terminus = "111",
            physicalPath = "path100/path110/path111.xml",
            zipPath = "name110/name111",
            type = "xml",
            origin = "110",
            id = "\"110\"-\"111\""
          ),
          AttachmentZipInfoImpl(
            terminus = "112",
            physicalPath = "path100/path110/path112.xml",
            zipPath = "name110/name112",
            type = "xml",
            origin = "110",
            id = "\"110\"-\"112\""
          )
        ), it)
      }.verifyComplete()
  }
}