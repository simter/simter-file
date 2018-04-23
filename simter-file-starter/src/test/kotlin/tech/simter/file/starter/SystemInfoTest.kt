package tech.simter.file.starter

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.info.GitProperties
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.PropertiesLoaderUtils
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import kotlin.test.assertEquals

@SpringJUnitConfig(classes = [ProjectInfoAutoConfiguration::class])
class SystemInfoTest @Autowired constructor(
  private val gitProperties: GitProperties,
  private val buildProperties: BuildProperties
) {
  @Test
  fun gitProperties() {
    val source = PropertiesLoaderUtils.loadProperties(ClassPathResource("git.properties"))
    assertEquals(source.getProperty("git.branch"), gitProperties.branch)
    assertEquals(source.getProperty("git.commit.id"), gitProperties.commitId)
    assertEquals(source.getProperty("git.commit.id.abbrev"), gitProperties.shortCommitId)
    //gitProperties.forEach { println("${it.key}=${it.value}") }
  }

  @Test
  fun buildProperties() {
    val source = PropertiesLoaderUtils.loadProperties(ClassPathResource("META-INF/build-info.properties"))
    assertEquals(source.getProperty("build.group"), buildProperties.group)
    assertEquals(source.getProperty("build.artifact"), buildProperties.artifact)
    assertEquals(source.getProperty("build.version"), buildProperties.version)
    //assertEquals(source.getProperty("build.time"), OffsetDateTime.ofInstant(buildProperties.time, ZoneId.systemDefault())
    //  .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))
    assertEquals(source.getProperty("build.java.target"), buildProperties.get("java.target"))
    assertEquals(source.getProperty("build.simter.version"), buildProperties.get("simter.version"))
    //buildProperties.forEach { println("${it.key}=${it.value}") }
  }
}