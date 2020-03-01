package tech.simter.file.test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class SampleTest {
  private val logger = LoggerFactory.getLogger(SampleTest::class.java)

  @Test
  fun test() {
    logger.debug("test log config")
    assertEquals(2, 1 + 1)
  }
}