package tech.simter.file.impl.service

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.OPERATION_DELETE
import tech.simter.file.core.FileDao
import tech.simter.file.core.FileService
import tech.simter.file.core.ModuleMatcher
import tech.simter.file.impl.FileAuthorizer
import tech.simter.file.impl.FileServiceImpl
import tech.simter.file.impl.UnitTestConfiguration
import tech.simter.file.test.TestHelper.randomFileStore
import tech.simter.file.test.TestHelper.randomModuleValue
import tech.simter.util.RandomUtils.randomString

/**
 * Test [FileServiceImpl.delete] (moduleMatcher).
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
class DeleteByModuleMatcherTest @Autowired constructor(
  private val fileAuthorizer: FileAuthorizer,
  private val service: FileService,
  private val dao: FileDao
) {
  private val module = randomModuleValue()
  private val moduleMatcher = ModuleMatcher.ModuleEquals(module)
  private val fileStore = randomFileStore(module = module)

  init {
    mockkObject(fileAuthorizer)
  }

  @Test
  fun `delete successful`() {
    // mock
    val count = 1
    every { fileAuthorizer.verifyHasPermission(module, OPERATION_DELETE) } returns Mono.empty()
    every { dao.findList(moduleMatcher) } returns Flux.just(fileStore)
    every { dao.delete(moduleMatcher) } returns Mono.just(count)

    // invoke and verify
    service.delete(moduleMatcher).test().expectNext(count).verifyComplete()
    verify(exactly = 1) {
      dao.findList(moduleMatcher)
      fileAuthorizer.verifyHasPermission(module, OPERATION_DELETE)
      dao.delete(moduleMatcher)
    }
  }

  @Test
  fun `delete but not exists`() {
    // mock
    every { fileAuthorizer.verifyHasPermission(module, OPERATION_DELETE) } returns Mono.empty()
    every { dao.findList(moduleMatcher) } returns Flux.empty()

    // invoke and verify
    service.delete(moduleMatcher).test().expectNext(0).verifyComplete()
    verify(exactly = 1) {
      fileAuthorizer.verifyHasPermission(module, OPERATION_DELETE)
      dao.findList(moduleMatcher)
    }
    verify(exactly = 0) {
      dao.delete(moduleMatcher)
    }
  }

  @Test
  fun `failed by permission denied`() {
    // mock
    val msg = randomString()
    every { fileAuthorizer.verifyHasPermission(module, OPERATION_DELETE) } returns Mono.error(PermissionDeniedException(msg))

    // invoke and verify
    service.delete(moduleMatcher)
      .test()
      .expectErrorSatisfies {
        assertThat(it).isInstanceOf(PermissionDeniedException::class.java)
          .hasMessage(msg)
      }.verify()
    verify(exactly = 1) {
      fileAuthorizer.verifyHasPermission(module, OPERATION_DELETE)
    }
    verify(exactly = 0) {
      dao.findList(moduleMatcher)
      dao.delete(moduleMatcher)
    }
  }
}