package tech.simter.file.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.po.Attachment
import java.time.OffsetDateTime
import java.util.*

/**
 * @author RJ
 */
@SpringJUnitConfig(classes = [(AttachmentServiceImpl::class)])
@MockBean(AttachmentDao::class)
class AttachmentServiceImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val service: AttachmentService
) {
  @Test
  fun get() {
    // mock
    var id: String = UUID.randomUUID().toString()
    val attachment = Attachment(id, "/data", "Sample", "png", 123, OffsetDateTime.now(), "Simter")
    val expected = Mono.just(attachment)
    `when`(dao.findById(id)).thenReturn(expected)

    // invoke
    val actual = service.get(id)

    // verify
    StepVerifier.create(actual)
      .expectNext(attachment)
      .verifyComplete()
  }

  @Test
  fun find() {
    // mock
    var pageable: Pageable = PageRequest.of(1, 25).first()
    var expect: Page<Attachment> = Page.empty()
    `when`(dao.findAll(pageable)).thenReturn(Mono.just(expect))

    // invoke
    var actual = service.find(pageable)

    // verify
    StepVerifier.create(actual)
      .expectNext(expect)
      .verifyComplete()
  }

  @Test
  fun create() {
    // mock
    val attachment = Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png", 123, OffsetDateTime.now(), "Simter")
    val expected = Mono.just(attachment)
    `when`(dao.save(attachment)).thenReturn(expected)

    // invoke
    val actual = service.create(expected)

    // verify
    StepVerifier.create(actual)
      .expectNext(attachment)
      .verifyComplete()
  }
}