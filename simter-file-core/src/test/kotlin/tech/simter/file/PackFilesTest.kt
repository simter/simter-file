package tech.simter.file

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tech.simter.file.core.FilePack
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import java.time.OffsetDateTime

/**
 * Please prepare two txt files to run these test:
 *
 * 1. `/t/file/to-pack/f1.txt`
 * 2. `/t/file/to-pack/f2.txt`
 *
 * After run this test, all zipped files store bellow path `/t/file/packed/`.
 * Need to checked its structure manually.
 */
@Disabled
class PackFilesTest {
  /**
   * the zip file `one.zip` structure should be:
   * ```
   * a/b/f1.txt
   * ```
   */
  @Test
  fun `pack one file`() {
    // clean
    val zipFile = File("/t/file/packed/one.zip")
    if (zipFile.exists()) zipFile.delete()
    assertThat(zipFile.exists()).isFalse()
    zipFile.parentFile.mkdirs()
    zipFile.createNewFile()

    // zip
    packFilesTo(
      outputStream = FileOutputStream(zipFile),
      files = listOf(
        FilePack.Impl(
          module = "/a/b/",
          name = "f1",
          type = "txt",
          size = 2L,
          path = "f1.txt",
          createOn = OffsetDateTime.now(),
          modifyOn = OffsetDateTime.now()
        )
      ),
      basePath = Paths.get("/t/file/to-pack/"),
      moduleMapper = emptyMap(),
      autoClose = true
    )

    // verify zip file created
    assertThat(zipFile.length()).isGreaterThan(0)
  }

  /**
   * the zip file `one-with-mapper.zip` structure should be:
   * ```
   * m/f1.txt
   * ```
   */
  @Test
  fun `pack one file with mapper`() {
    // clean
    val zipFile = File("/t/file/packed/one-with-mapper.zip")
    if (zipFile.exists()) zipFile.delete()
    assertThat(zipFile.exists()).isFalse()
    zipFile.parentFile.mkdirs()
    zipFile.createNewFile()

    // zip
    packFilesTo(
      outputStream = FileOutputStream(zipFile),
      files = listOf(
        FilePack.Impl(
          module = "/a/b/",
          name = "f1",
          type = "txt",
          size = 2L,
          path = "f1.txt",
          createOn = OffsetDateTime.now(),
          modifyOn = OffsetDateTime.now()
        )
      ),
      basePath = Paths.get("/t/file/to-pack/"),
      moduleMapper = mapOf("/a/b/" to "/m/"),
      autoClose = true
    )

    // verify zip file created
    assertThat(zipFile.length()).isGreaterThan(0)
  }

  /**
   * the zip file `two-with-mapper1.zip` structure should be:
   * ```
   * m/f1.txt
   * m/f2.txt
   * ```
   */
  @Test
  fun `pack two file with mapper 1`() {
    // clean
    val zipFile = File("/t/file/packed/two-with-mapper1.zip")
    if (zipFile.exists()) zipFile.delete()
    assertThat(zipFile.exists()).isFalse()
    zipFile.parentFile.mkdirs()
    zipFile.createNewFile()

    // zip
    packFilesTo(
      outputStream = FileOutputStream(zipFile),
      files = listOf(
        FilePack.Impl(
          module = "/a/b/",
          name = "f1",
          type = "txt",
          size = 2L,
          path = "f1.txt",
          createOn = OffsetDateTime.now(),
          modifyOn = OffsetDateTime.now()
        ),
        FilePack.Impl(
          module = "/a/b/",
          name = "f2",
          type = "txt",
          size = 2L,
          path = "f2.txt",
          createOn = OffsetDateTime.now(),
          modifyOn = OffsetDateTime.now()
        )
      ),
      basePath = Paths.get("/t/file/to-pack/"),
      moduleMapper = mapOf("/a/b/" to "/m/"),
      autoClose = true
    )

    // verify zip file created
    assertThat(zipFile.length()).isGreaterThan(0)
  }

  /**
   * the zip file `two-with-mapper2.zip` structure should be:
   * ```
   * m1/f1.txt
   * m2/f2.txt
   * ```
   */
  @Test
  fun `pack two file with mapper 2`() {
    // clean
    val zipFile = File("/t/file/packed/two-with-mapper2.zip")
    if (zipFile.exists()) zipFile.delete()
    assertThat(zipFile.exists()).isFalse()
    zipFile.parentFile.mkdirs()
    zipFile.createNewFile()

    // zip
    packFilesTo(
      outputStream = FileOutputStream(zipFile),
      files = listOf(
        FilePack.Impl(
          module = "/a/b/",
          name = "f1",
          type = "txt",
          size = 2L,
          path = "f1.txt",
          createOn = OffsetDateTime.now(),
          modifyOn = OffsetDateTime.now()
        ),
        FilePack.Impl(
          module = "/b/c/",
          name = "f2",
          type = "txt",
          size = 2L,
          path = "f2.txt",
          createOn = OffsetDateTime.now(),
          modifyOn = OffsetDateTime.now()
        )
      ),
      basePath = Paths.get("/t/file/to-pack/"),
      moduleMapper = mapOf("/a/b/" to "/m1/", "/b/c/" to "/m2/"),
      autoClose = true
    )

    // verify zip file created
    assertThat(zipFile.length()).isGreaterThan(0)
  }
}