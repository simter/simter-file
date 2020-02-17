package tech.simter.file.impl.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.*

/**
 * @author RJ
 */
class AttachmentTest {
  private val logger = LoggerFactory.getLogger(AttachmentTest::class.java)

  @Test
  fun test() {
    val attachment1 = AttachmentImpl(
      id = UUID.randomUUID().toString(),
      path = "/data",
      name = "Sample",
      type = "png",
      size = 123,
      createOn = OffsetDateTime.now(),
      creator = "Simter",
      modifyOn = OffsetDateTime.now(),
      modifier = "Simter"
    )
    logger.debug(attachment1.toString())
    assertEquals(attachment1.fileName, "Sample.png")
    assertEquals(attachment1, attachment1.copy())
    assertNotEquals(attachment1, attachment1.copy(name = "OtherName"))
    assertEquals(attachment1.upperId, null)
    assertEquals(attachment1.puid, null)
  }
}