package tech.simter.file.rest.webflux

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * @author RJ
 */
class SampleKotlinTest {
  private val logger = LoggerFactory.getLogger(SampleKotlinTest::class.java)

  @Test
  fun test() {
    logger.debug("test log config")
    assertEquals(2, 1 + 1)
  }
}