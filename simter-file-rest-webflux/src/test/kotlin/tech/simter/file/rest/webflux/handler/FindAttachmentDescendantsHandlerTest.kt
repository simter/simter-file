package tech.simter.file.rest.webflux.handler

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.core.domain.AttachmentTreeNode
import tech.simter.file.impl.domain.AttachmentTreeNodeImpl
import tech.simter.file.rest.webflux.TestHelper.randomAttachmentId
import tech.simter.file.rest.webflux.TestHelper.randomInt
import tech.simter.file.rest.webflux.TestHelper.randomString
import tech.simter.file.rest.webflux.UnitTestConfiguration
import java.time.OffsetDateTime
import java.util.*

/**
 * Test [FindAttachmentDescendantsHandler].
 *
 * @author zh
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
class FindAttachmentDescendantsHandlerTest @Autowired constructor(
  private val client: WebTestClient,
  private val service: AttachmentService
) {
  private val id = UUID.randomUUID().toString()
  private val url = "/attachment/$id/descendant"
  private fun randomAttachmentTreeNode(depth: Int, maxDegree: Int): AttachmentTreeNode {
    return AttachmentTreeNodeImpl(
      id = randomAttachmentId(),
      name = randomString("name"),
      type = randomString("type"),
      path = randomString("path"),
      size = randomInt().toLong(),
      modifier = randomString("modifier"),
      modifyOn = OffsetDateTime.now(),
      children = if (depth > 0) List(randomInt(0, maxDegree)) { randomAttachmentTreeNode(depth - 1, maxDegree) }
      else emptyList()
    )
  }

  @Test
  fun `find something`() {
    // mock
    val dtos = List(randomInt(1, 3)) { randomAttachmentTreeNode(1, 2) }
    every { service.findDescendants(id) } returns dtos.toFlux()

    // invoke and verify
    client.get().uri(url).exchange()
      .expectHeader().contentType(APPLICATION_JSON)
      .expectStatus().isOk
      .expectBody().apply {
        dtos.forEachIndexed { index, dto ->
          jsonPath("$[$index].id").isEqualTo(dto.id)
          jsonPath("$[$index].name").isEqualTo(dto.name)
          jsonPath("$[$index].type").isEqualTo(dto.type)
          jsonPath("$[$index].path").isEqualTo(dto.path)
          jsonPath("$[$index].size").isEqualTo(dto.size)
          jsonPath("$[$index].modifier").isEqualTo(dto.modifier)
          dto.children!!.forEachIndexed { childIndex, childDtos ->
            jsonPath("$[$index].children[$childIndex].id").isEqualTo(childDtos.id)
            jsonPath("$[$index].children[$childIndex].name").isEqualTo(childDtos.name)
            jsonPath("$[$index].children[$childIndex].type").isEqualTo(childDtos.type)
            jsonPath("$[$index].children[$childIndex].size").isEqualTo(childDtos.size)
            jsonPath("$[$index].children[$childIndex].modifier").isEqualTo(childDtos.modifier)
          }
        }
      }
    verify { service.findDescendants(id) }
  }

  @Test
  fun `found nothing`() {
    // mock
    every { service.findDescendants(id) } returns Flux.empty()

    // invoke and verify
    client.get().uri(url).exchange()
      .expectHeader().contentType(APPLICATION_JSON)
      .expectStatus().isOk
      .expectBody().jsonPath("$").isEmpty
    verify { service.findDescendants(id) }
  }

  @Test
  fun `failed by permission denied`() {
    // mock
    every { service.findDescendants(id) } returns Flux.error(PermissionDeniedException())

    // invoke and verify
    client.get().uri(url).exchange().expectStatus().isForbidden
    verify { service.findDescendants(id) }
  }
}