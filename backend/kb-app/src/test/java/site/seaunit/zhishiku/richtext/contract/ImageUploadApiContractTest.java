package site.seaunit.zhishiku.richtext.contract;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import site.seaunit.zhishiku.richtext.image.ImageUploadController;
import site.seaunit.zhishiku.richtext.image.ImageUploadResponse;
import site.seaunit.zhishiku.richtext.image.ImageUploadService;
import site.seaunit.zhishiku.security.SecurityConfig;

@WebMvcTest(ImageUploadController.class)
@Import(SecurityConfig.class)
class ImageUploadApiContractTest {
  @Autowired private MockMvc mvc;

  @MockBean private ImageUploadService service;

  @Test
  @WithMockUser("owner@example.com")
  void uploadRequiresCsrfAndReturnsOpenApiFields() throws Exception {
    UUID uploadId = UUID.randomUUID();
    when(service.create(any(), isNull(), any()))
        .thenReturn(
            new ImageUploadResponse(
                uploadId,
                null,
                "ready",
                "clean",
                "none",
                "image/png",
                10,
                10,
                "a".repeat(64),
                1,
                1,
                1.0,
                null,
                Instant.parse("2026-10-23T00:00:00Z")));
    MockMultipartFile file = new MockMultipartFile("file", "safe.png", "image/png", new byte[] {1});

    mvc.perform(multipart("/api/v1/rich-text/image-uploads").file(file))
        .andExpect(status().isForbidden());

    mvc.perform(multipart("/api/v1/rich-text/image-uploads").file(file).with(csrf()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.uploadId").value(uploadId.toString()))
        .andExpect(jsonPath("$.status").value("ready"))
        .andExpect(jsonPath("$.contentHash").value("a".repeat(64)))
        .andExpect(jsonPath("$.expiresAt").exists());
  }
}
