package tech.simter.file.impl.service

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.OPERATION_READ
import tech.simter.file.core.FileDao
import tech.simter.file.core.FileService
import tech.simter.file.core.FileStore
import tech.simter.file.core.ModuleMatcher
import tech.simter.file.impl.FileAuthorizer
import tech.simter.file.impl.FileServiceImpl
import tech.simter.file.impl.UnitTestConfiguration
import tech.simter.file.test.TestHelper.randomModuleValue
import tech.simter.kotlin.data.Page
import tech.simter.util.RandomUtils.randomString
import java.util.*

/**
 * Test [FileServiceImpl.findPage].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
class FindPageTest @Autowired constructor(
  private val fileAuthorizer: FileAuthorizer,
  private val service: FileService,
  private val dao: FileDao
) {
  private val module = randomModuleValue()
  private val moduleMatcher = ModuleMatcher.ModuleEquals(module)
  private val limit = Optional.of(1)

  init {
    mockkObject(fileAuthorizer)
  }

  @Test
  fun `found something`() {
    // mock
    val page = Page.empty<FileStore>()
    every { fileAuthorizer.verifyHasPermission(module, OPERATION_READ) } returns Mono.empty()
    every { dao.findPage(moduleMatcher = moduleMatcher, limit = limit.get()) } returns Mono.just(page)

    // invoke and verify
    service.findPage(moduleMatcher = moduleMatcher, limit = limit)
      .test().expectNext(page).verifyComplete()
    verify(exactly = 1) {
      fileAuthorizer.verifyHasPermission(module, OPERATION_READ)
      dao.findPage(moduleMatcher = moduleMatcher, limit = limit.get())
    }
  }

  @Test
  fun `found nothing`() {
    // mock
    val page = Page.empty<FileStore>()
    every { fileAuthorizer.verifyHasPermission(module, OPERATION_READ) } returns Mono.empty()
    every { dao.findPage(moduleMatcher = moduleMatcher, limit = limit.get()) } returns Mono.just(page)

    // invoke and verify
    service.findPage(moduleMatcher = moduleMatcher, limit = limit)
      .test().expectNext(page).verifyComplete()
    verify(exactly = 1) {
      fileAuthorizer.verifyHasPermission(module, OPERATION_READ)
      dao.findPage(moduleMatcher = moduleMatcher, limit = limit.get())
    }
  }

  @Test
  fun `failed by permission denied`() {
    // mock
    val msg = randomString()
    every { fileAuthorizer.verifyHasPermission(module, OPERATION_READ) } returns Mono.error(PermissionDeniedException(msg))

    // invoke and verify
    service.findPage(moduleMatcher)
      .test()
      .expectErrorSatisfies {
        assertThat(it).isInstanceOf(PermissionDeniedException::class.java)
          .hasMessage(msg)
      }.verify()
    verify(exactly = 1) {
      fileAuthorizer.verifyHasPermission(module, OPERATION_READ)
    }
    verify(exactly = 0) {
      dao.findPage(moduleMatcher = moduleMatcher, limit = 1)
    }
  }
}