package tech.simter.file.starter

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * @author RJ
 */
@Disabled
class ZipFilesTest {
  @Test
  fun test() {
    val buffer = ByteArray(1024)
    val zos = ZipOutputStream(FileOutputStream("target/test.zip"))

    // test generates a zip file with directory structure
    val source = "application.yml"
    addToZip(javaClass.classLoader.getResourceAsStream(source), source, zos, buffer)
    addToZip(javaClass.classLoader.getResourceAsStream(source), "dir/$source", zos, buffer)
    addToZip(javaClass.classLoader.getResourceAsStream(source), "dir/subDir/$source", zos, buffer)

    // close the ZipOutputStream
    zos.close()
  }

  private fun addToZip(source: InputStream, target: String, zos: ZipOutputStream, buffer: ByteArray) {
    // begin writing a new ZIP entry, positions the stream to the start of the entry data
    zos.putNextEntry(ZipEntry(target))
    var length: Int
    while (true) {
      length = source.read(buffer)
      if (length > 0) zos.write(buffer, 0, length)
      else break
    }
    zos.closeEntry()
    // close the InputStream
    source.close()
  }
}