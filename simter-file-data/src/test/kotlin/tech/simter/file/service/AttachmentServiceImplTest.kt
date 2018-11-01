package tech.simter.file.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import tech.simter.exception.NotFoundException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.dto.AttachmentDtoWithChildren
import tech.simter.file.po.Attachment
import java.time.OffsetDateTime
import java.util.*

/**
 * Test [AttachmentService]
 *
 * @author RJ
 * @author zh
 */
@SpringJUnitConfig(AttachmentServiceImpl::class)
@MockBean(AttachmentDao::class)
class AttachmentServiceImplTest @Autowired constructor(
  private val dao: AttachmentDao,
  private val service: AttachmentService
) {
  @Test
  fun get() {
    // mock
    val id: String = UUID.randomUUID().toString()
    val now = OffsetDateTime.now()
    val attachment = Attachment(id, "/data", "Sample", "png", 123, now,
      "Simter", now, "Simter")
    val expected = Mono.just(attachment)
    `when`(dao.get(id)).thenReturn(expected)

    // invoke
    val actual = service.get(id)

    // verify
    StepVerifier.create(actual).expectNext(attachment).verifyComplete()
    verify(dao).get(id)
  }

  @Test
  fun findByPageable() {
    // mock
    val pageable: Pageable = PageRequest.of(0, 25)
    val expect: Page<Attachment> = Page.empty()
    `when`(dao.find(pageable)).thenReturn(Mono.just(expect))

    // invoke
    val actual = service.find(pageable)

    // verify
    StepVerifier.create(actual).expectNext(expect).verifyComplete()
    verify(dao).find(pageable)
  }

  @Test
  fun findByModuleAndSubgroup() {
    // mock
    val puid = "puid1"
    val subgroup = "1"
    val expect = Collections.emptyList<Attachment>()
    `when`(dao.find(puid, subgroup)).thenReturn(Flux.fromIterable(expect))

    // invoke
    val actual = service.find(puid, subgroup)

    // verify
    StepVerifier.create(actual.collectList()).expectNext(expect).verifyComplete()
    verify(dao).find(puid, subgroup)
  }

  @Test
  fun save() {
    // mock
    val now = OffsetDateTime.now()
    val attachment = Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png",
      123, now, "Simter", now, "Simter")
    `when`(dao.save(attachment)).thenReturn(Mono.empty())

    // invoke
    val actual = service.save(attachment)

    // verify
    StepVerifier.create(actual).expectNext().verifyComplete()
    verify(dao).save(attachment)
  }

  @Test
  fun getFullPath() {
    // mock
    val id = UUID.randomUUID().toString()
    val fullPath = "level1/level2/level3/level4"
    `when`(dao.getFullPath(id)).thenReturn(fullPath.toMono())

    // invoke
    val actual = service.getFullPath(id)

    // verify
    StepVerifier.create(actual).expectNext(fullPath).verifyComplete()
    verify(dao).getFullPath(id)
  }

  @Test
  fun getFullPathNotFound() {
    // mock
    val id = UUID.randomUUID().toString()
    `when`(dao.getFullPath(id)).thenReturn(Mono.empty())

    // invoke
    val actual = service.getFullPath(id)

    // verify
    StepVerifier.create(actual).verifyError(NotFoundException::class.java)
    verify(dao).getFullPath(id)
  }

  @Test
  fun findDescendents() {
    // mock
    val id = UUID.randomUUID().toString()
    val dtos = List(3) { index ->
      AttachmentDtoWithChildren().apply {
        this.id = UUID.randomUUID().toString()
        name = "name$index"
        type = "type$index"
        size = index.toLong()
        modifyOn = OffsetDateTime.now()
        modifier = "modifier$index"
      }
    }
    `when`(dao.findDescendents(id)).thenReturn(dtos.toFlux())

    // invoke
    val actual = service.findDescendents(id)

    // verify
    StepVerifier.create(actual.collectList()).expectNext(dtos).verifyComplete()
    verify(dao).findDescendents(id)
  }
}