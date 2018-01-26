package tech.simter.file.rest.webflux.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import tech.simter.file.po.Attachment;
import tech.simter.file.rest.webflux.WebFluxConfiguration;
import tech.simter.file.service.AttachmentService;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

/**
 * Test DownloadFileHandler.
 *
 * @author JF
 */
@SpringJUnitConfig(classes = {
  WebFluxConfiguration.class, DownloadFileHandler.class
})
@MockBean(AttachmentService.class)
@TestPropertySource(properties = "app.file.root=src/test")
class DownloadFileHandlerTest {
  private WebTestClient client;
  @SpyBean
  private DownloadFileHandler handler;
  @Autowired
  private AttachmentService service;
  @Value("${app.file.root}")
  private String fileRootDir;

  @BeforeEach
  void setUp() {
    client = WebTestClient.bindToRouterFunction(handler.router()).build();
  }

  @Test
  void download() throws IOException {
    // mock service return value
    String name = "logback-test";
    String ext = "xml";
    String fileName = name + "." + ext;
    String id = UUID.randomUUID().toString();
    long fileSize = new FileSystemResource(fileRootDir + "/resources/" + fileName).contentLength();
    Attachment attachment = new Attachment(id, "resources/" + name + "." + ext, name, ext, fileSize,
      OffsetDateTime.now(), "Simter");
    Mono<Attachment> expected = Mono.just(attachment);
    when(service.get(id)).thenReturn(expected);

    // invoke request
    EntityExchangeResult<byte[]> result = client.get().uri("/" + id)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().valueEquals("Content-Type", APPLICATION_OCTET_STREAM_VALUE)
      .expectHeader().valueEquals("Content-Length", String.valueOf(fileSize))
      .expectHeader().valueEquals("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
      .expectBody().returnResult();

    // verify response body
    assertNotNull(result.getResponseBody());
    assertEquals(result.getResponseBody().length, fileSize);

    // verify method service.get invoked
    verify(service).get(id);
  }
}