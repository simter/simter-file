package tech.simter.file.rest.webflux.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import tech.simter.file.po.Attachment;
import tech.simter.file.rest.webflux.WebFluxConfiguration;
import tech.simter.file.service.AttachmentService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test AttachmentViewHandler.
 *
 * @author JF
 */
@SpringJUnitConfig(classes = {
  WebFluxConfiguration.class, AttachmentViewHandler.class
})
@MockBean(AttachmentService.class)
class AttachmentViewHandlerTest {
  private WebTestClient client;
  @SpyBean
  private AttachmentViewHandler handler;
  @Autowired
  private AttachmentService service;

  @BeforeEach
  void setUp() {
    client = WebTestClient.bindToRouterFunction(handler.router()).build();
  }

  @Test
  @SuppressWarnings("unchecked")
  void fileView() {
    // mock
    int pageNo = 0;
    int pageSize = 25;
    String id = UUID.randomUUID().toString();
    List<Attachment> list = new ArrayList<>();
    list.add(new Attachment(id, "/path", "name", "ext", 100, OffsetDateTime.now(), "Simter"));
    Pageable pageable = PageRequest.of(pageNo, pageSize);
    when(service.find(pageable)).thenReturn(Mono.just(new PageImpl(list, pageable, list.size())));

    // invoke
    client.get().uri("/attachment?page-no=0&page-size=25")
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
      .expectBody()
      .jsonPath("$.count").isEqualTo(list.size()) // verify count
      .jsonPath("$.pageNo").isEqualTo(pageNo)     // verify page-no
      .jsonPath("$.pageSize").isEqualTo(pageSize) // verify page-size
      .jsonPath("$.rows[0].id").isEqualTo(id);    // verify Attachment.id

    // verify
    verify(service).find(pageable);
  }
}