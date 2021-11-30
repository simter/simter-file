package tech.simter.file.impl.service

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.OPERATION_CREATE
import tech.simter.file.core.FileDao
import tech.simter.file.core.FileStore
import tech.simter.file.core.FileUploadSource
import tech.simter.file.impl.*
import tech.simter.file.standardModuleValue
import tech.simter.file.test.TestHelper.randomFileStore
import tech.simter.util.RandomUtils.randomString
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Test [FileServiceImpl.upload].
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class UploadTest @Autowired constructor(
  private val fileAuthorizer: FileAuthorizer,
  private val filePathGenerator: FilePathGeneratorImpl,
  private val fileIdGenerator: FileIdGeneratorImpl,
  private val service: FileServiceImpl,
  private val dao: FileDao
) {
  private val module = standardModuleValue(randomString())
  private val id = randomString(6)
  private val source = FileUploadSource.FromResource(ClassPathResource("application.yml"))
  private val filePath = Paths.get("application.yml")
  private val currentUserName = randomString(6)
  private val ts = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)
  private val uuid = UUID.randomUUID()
  private val describer = randomFileStore(module = module)
  private val fileStore = FileStore.Impl(
    id = id,
    module = describer.module,
    name = describer.name,
    type = describer.type,
    size = describer.size,
    path = filePath.toString(),
    creator = currentUserName,
    createOn = ts,
    modifier = currentUserName,
    modifyOn = ts
  )

  init {
    mockkObject(fileAuthorizer)
    mockkObject(filePathGenerator)
    mockkObject(fileIdGenerator)
    mockkObject(service)
    println(fileStore)
  }

  @Test
  fun `upload successful`() {
    // mock
    every { fileAuthorizer.verifyHasPermission(module, OPERATION_CREATE) } returns Mono.empty()
    every { service.getCurrentTimestamp() } returns ts
    every { service.generateUUID() } returns uuid
    every { service.getCurrentUser() } returns Mono.just(currentUserName)
    every { filePathGenerator.resolve(describer = describer, ts = Optional.of(ts), uuid = Optional.of(uuid)) } returns filePath
    every { fileIdGenerator.nextId(ts = Optional.of(ts), uuid = Optional.of(uuid)) } returns Mono.just(id)
    every { dao.create(fileStore) } returns Mono.just(id)

    // invoke and verify
    service.upload(describer, source).test().expectNext(id).verifyComplete()
    verify(exactly = 1) {
      service.getCurrentTimestamp()
      service.generateUUID()
      fileAuthorizer.verifyHasPermission(module, OPERATION_CREATE)
      service.getCurrentUser()
      filePathGenerator.resolve(describer = describer, ts = Optional.of(ts), uuid = Optional.of(uuid))
      fileIdGenerator.nextId(ts = Optional.of(ts), uuid = Optional.of(uuid))
      dao.create(fileStore)
    }

    // verify physical file
    // TODO
  }

  @Test
  fun `failed by permission denied`() {
    // mock
    val msg = randomString()
    every { fileAuthorizer.verifyHasPermission(module, OPERATION_CREATE) } returns Mono.error(PermissionDeniedException(msg))

    // invoke and verify
    service.upload(describer, source)
      .test()
      .expectErrorSatisfies {
        assertThat(it).isInstanceOf(PermissionDeniedException::class.java)
          .hasMessage(msg)
      }.verify()
    verify(exactly = 1) {
      service.getCurrentTimestamp()
      service.generateUUID()
      fileAuthorizer.verifyHasPermission(module, OPERATION_CREATE)
    }
    verify(exactly = 0) {
      service.getCurrentUser()
      filePathGenerator.resolve(describer = describer, ts = Optional.of(ts), uuid = Optional.of(uuid))
      fileIdGenerator.nextId(ts = Optional.of(ts), uuid = Optional.of(uuid))
      dao.create(fileStore)
    }
  }
}