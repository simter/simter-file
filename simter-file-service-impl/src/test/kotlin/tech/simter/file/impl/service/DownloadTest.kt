package tech.simter.file.impl.service

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.BASE_DATA_DIR
import tech.simter.file.OPERATION_READ
import tech.simter.file.core.FileDao
import tech.simter.file.core.FileDownload
import tech.simter.file.core.FileService
import tech.simter.file.impl.FileAuthorizer
import tech.simter.file.impl.FileServiceImpl
import tech.simter.file.impl.UnitTestConfiguration
import tech.simter.file.test.TestHelper.randomFileStore
import tech.simter.util.RandomUtils.randomString
import java.nio.file.Paths

/**
 * Test [FileServiceImpl.download].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
class DownloadTest @Autowired constructor(
  @Value("\${$BASE_DATA_DIR}")
  private val baseDir: String,
  private val fileAuthorizer: FileAuthorizer,
  private val service: FileService,
  private val dao: FileDao
) {
  private val basePath = Paths.get(baseDir)
  private val module = randomString()
  private val id = randomString()
  private val fileStore = randomFileStore(module = module)
  private val fileDownload = FileDownload.from(fileStore, basePath)

  init {
    mockkObject(fileAuthorizer)
  }

  @Test
  fun `download successful`() {
    // mock
    every { fileAuthorizer.verifyHasPermission(module, OPERATION_READ) } returns Mono.empty()
    every { dao.get(id) } returns Mono.just(fileStore)

    // invoke and verify
    service.download(id).test().expectNext(fileDownload).verifyComplete()
    verify(exactly = 1) {
      dao.get(id)
      fileAuthorizer.verifyHasPermission(module, OPERATION_READ)
    }
  }

  @Test
  fun `download but not exists`() {
    // mock
    every { fileAuthorizer.verifyHasPermission(module, OPERATION_READ) } returns Mono.empty()
    every { dao.get(id) } returns Mono.empty()

    // invoke and verify
    service.download(id).test().verifyComplete()
    verify(exactly = 1) {
      dao.get(id)
    }
    verify(exactly = 0) {
      fileAuthorizer.verifyHasPermission(module, OPERATION_READ)
    }
  }

  @Test
  fun `failed by permission denied`() {
    // mock
    val msg = randomString()
    every { fileAuthorizer.verifyHasPermission(module, OPERATION_READ) } returns Mono.error(PermissionDeniedException(msg))
    every { dao.get(id) } returns Mono.just(fileStore)

    // invoke and verify
    service.download(id)
      .test()
      .expectErrorSatisfies {
        assertThat(it).isInstanceOf(PermissionDeniedException::class.java)
          .hasMessage(msg)
      }.verify()
    verify(exactly = 1) {
      dao.get(id)
      fileAuthorizer.verifyHasPermission(module, OPERATION_READ)
    }
  }
}