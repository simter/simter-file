package tech.simter.file.rest.webflux.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import tech.simter.file.po.Attachment;
import tech.simter.file.rest.webflux.WebFluxConfiguration;
import tech.simter.file.service.AttachmentService;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

/**
 * Test UploadFileHandler.
 *
 * @author JF
 * @author RJ
 */
@SpringJUnitConfig(classes = {
  WebFluxConfiguration.class, UploadFileHandler.class
})
@MockBean({AttachmentService.class})
@TestPropertySource(properties = "app.file.root=target/files")
class UploadFileHandlerTest {
  private WebTestClient client;
  @SpyBean
  private UploadFileHandler handler;
  @Autowired
  private AttachmentService service;
  @Value("${app.file.root}")
  private String fileRootDir;

  @BeforeEach
  void setUp() {
    client = WebTestClient.bindToRouterFunction(handler.router()).build();
  }

  @Test
  void upload() throws IOException {
    // mock MultipartBody
    String name = "logback-test";
    String ext = "xml";
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    ClassPathResource file = new ClassPathResource(name + "." + ext);
    builder.part("fileData", file);
    builder.part("puid", "puid");
    builder.part("subgroup", "1");
    MultiValueMap<String, HttpEntity<?>> parts = builder.build();

    // mock service.create return value
    String id = UUID.randomUUID().toString();
    long fileSize = file.contentLength();
    Attachment attachment = new Attachment(id, "/data", name, ext,
      fileSize, OffsetDateTime.now(), "Simter", "puid", new Short("1"));
    when(service.create(any())).thenReturn(Mono.just(attachment));

    // mock handler.newId return value
    when(handler.newId()).thenReturn(id);

    // invoke request
    LocalDateTime now = LocalDateTime.now().truncatedTo(SECONDS);
    client.post().uri("/")
      .contentType(MULTIPART_FORM_DATA)
      .contentLength(fileSize)
      .syncBody(parts)
      .exchange()
      .expectStatus().isNoContent()
      .expectHeader().valueEquals("Location", "/" + id);

    // 1. verify service.create method invoked
    verify(service).create(any());

    // 2. verify the saved file exists
    String yyyyMM = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
    File[] files = new File(fileRootDir + "/" + yyyyMM).listFiles();
    assertNotNull(files);
    assertTrue(files.length > 0);
    File actualFile = null;
    for (File f : files) {
      // extract dateTime and id from fileName: yyyyMMddTHHmmss-{id}.{ext}
      int index = f.getName().indexOf("-");
      LocalDateTime dateTime = LocalDateTime.parse(f.getName().substring(0, index),
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
      String uuid = f.getName().substring(index + 1, f.getName().lastIndexOf("."));
      if (id.equals(uuid) && !dateTime.isBefore(now)) {
        actualFile = f;
        break;
      }
    }
    assertNotNull(actualFile);

    // 3. verify the saved file size
    assertEquals(actualFile.length(), fileSize);

    // 4. TODO verify the attachment
  }
}