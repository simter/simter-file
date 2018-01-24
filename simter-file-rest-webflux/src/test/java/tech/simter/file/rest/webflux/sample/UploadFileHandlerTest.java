package tech.simter.file.rest.webflux.sample;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.ExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import tech.simter.file.po.Attachment;
import tech.simter.file.rest.webflux.WebFluxConfiguration;
import tech.simter.file.rest.webflux.handler.UploadFileHandler;
import tech.simter.file.service.AttachmentService;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

/**
 * see <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#webtestclient">WebTestClient</a>
 */
@SpringJUnitConfig(classes = {
  WebFluxConfiguration.class, UploadFileHandler.class
})
@MockBean({AttachmentService.class})
@TestPropertySource(properties = "app.file.root=/data/files")
class UploadFileHandlerTest {
  @Autowired
  private UploadFileHandler handler;
  private WebTestClient client;

  @Autowired
  private AttachmentService service;

  @Value("${app.file.root}")
  private String fileRootDir;

  @BeforeEach
  void setUp() {
    client = WebTestClient.bindToRouterFunction(handler.router()).build();
  }

  @Test
  void test() throws IOException {
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("fileData", new FileSystemResource("src/test/resources/a.png"));
    MultiValueMap<String, HttpEntity<?>> parts = builder.build();

    // mock
    Attachment attachment = new Attachment(UUID.randomUUID().toString(), "/data", "Sample", "png",
      123, OffsetDateTime.now(), "Simter");
    Mono<Attachment> expected = Mono.just(attachment);
    when(service.create(expected)).thenReturn(expected);

    // invoke
    ExchangeResult exchangeResult = client.post().uri("/")
      .contentType(MULTIPART_FORM_DATA)
      .contentLength(50000L)
      .syncBody(parts)
      .exchange()
      .expectStatus().isNoContent()
      .returnResult(String.class);

    // verify
    URI uri = exchangeResult.getResponseHeaders().getLocation();
    assert uri != null;
    String uuid = uri.toString();
    String yyyyMM = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
    String localDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm"));
    File[] files = new File(fileRootDir + "/" + yyyyMM).listFiles();
    assert files != null;
    assert files[0].getName().matches(localDateTime + "\\d{2}-" + uuid + ".png");
    assert files[0].delete();
    verify(service).create(any());
  }
}