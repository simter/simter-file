package tech.simter.file.rest.webflux.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import tech.simter.file.po.Attachment;
import tech.simter.file.rest.webflux.WebFluxConfiguration;
import tech.simter.file.service.AttachmentService;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test AttachmentFormHandler.
 *
 * @author JF
 */
@SpringJUnitConfig(classes = {
  WebFluxConfiguration.class, AttachmentFormHandler.class
})
@MockBean(AttachmentService.class)
class AttachmentFormHandlerTest {
  private WebTestClient client;
  @SpyBean
  private AttachmentFormHandler handler;
  @Autowired
  private AttachmentService service;

  @BeforeEach
  void setUp() {
    client = WebTestClient.bindToRouterFunction(handler.router()).build();
  }

  @Test
  void attachmentForm() {
    // mock
    String id = UUID.randomUUID().toString();
    Attachment attachment = new Attachment(id, "/path", "name", "ext", 100,
      OffsetDateTime.now(), "Simter");
    when(service.get(id)).thenReturn(Mono.just(attachment));

    // invoke
    client.get().uri("/attachment/" + id)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
      .expectBody().jsonPath("$.id").isEqualTo(id);

    // verify
    verify(service).get(id);
  }
}