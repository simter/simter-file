package tech.simter.file.starter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SampleTest {
  private val logger: Logger = LoggerFactory.getLogger(SampleTest::class.java)

  @Test
  fun test() {
    logger.debug("test log config")
    assertThat(1 + 1).isEqualTo(2)
  }
}