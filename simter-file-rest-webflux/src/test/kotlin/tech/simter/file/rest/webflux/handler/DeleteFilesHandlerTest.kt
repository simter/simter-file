package tech.simter.file.rest.webflux.handler

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunctions.route
import reactor.core.publisher.Mono
import tech.simter.file.rest.webflux.handler.DeleteFilesHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.service.AttachmentService
import java.util.*

/**
 * Test [DeleteFilesHandler].
 *
 * @author JW
 */
@SpringJUnitConfig(DeleteFilesHandler::class)
@EnableWebFlux
@MockBean(AttachmentService::class)
@TestPropertySource(properties = ["simter.file.root=target/files"])
internal class DeleteFilesHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  @Value("\${simter.file.root}") private val fileRootDir: String,
  handler: DeleteFilesHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, handler)).build()

  @Test
  fun deleteOne() {
    // mock
    val id = UUID.randomUUID().toString()
    `when`(service.delete(id)).thenReturn(Mono.empty())

    // invoke
    client.delete().uri("/$id").exchange().expectStatus().isNoContent

    // verify
    Mockito.verify(service).delete(id)
  }

  @Test
  fun deleteBatch() {
    // mock
    val ids = arrayOf("a0001", "a0002", "a0003", "a0004", "a0005")
    `when`(service.delete(*ids)).thenReturn(Mono.empty())

    // invoke
    client.delete().uri("/${ids.joinToString(",")}").exchange().expectStatus().isNoContent

    // verify
    Mockito.verify(service).delete(*ids)
  }
}