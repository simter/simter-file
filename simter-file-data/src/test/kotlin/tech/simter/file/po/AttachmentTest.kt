package tech.simter.file.po

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
    val attachment1 = Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png", 123, OffsetDateTime.now(), "Simter")
    logger.debug(attachment1.toString())
    assertEquals(attachment1.fileName, "Sample.png")
    assertEquals(attachment1, attachment1.copy())
    assertNotEquals(attachment1, attachment1.copy(name = "OtherName"))
  }
}