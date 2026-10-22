package site.seaunit.zhishiku.textimport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.SourceSummary;
import site.seaunit.zhishiku.security.SecurityConfig;
import site.seaunit.zhishiku.textimport.TextImportDtos.TextImportResponse;

@WebMvcTest(TextImportController.class)
@Import({SecurityConfig.class, site.seaunit.zhishiku.common.ApiExceptionHandler.class})
class TextImportControllerTest {
  @Autowired private MockMvc mvc;

  @MockBean private TextImportService service;
  @MockBean private AuthenticationManager authenticationManager;

  @Test
  @WithMockUser(username = "alice@example.com")
  void importsTextAndReturnsCreatedKnowledge() throws Exception {
    when(service.importText(any(), eq("text-controller"), any()))
        .thenReturn(new TextImportResponse(List.of(response())));

    mvc.perform(
            post("/api/v1/text-imports")
                .with(csrf())
                .header("Idempotency-Key", "text-controller")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "text": "这是一段足够长的 AI 辅助文字内容，用来验证服务端整理和入库流程。",
                      "externalAiConsent": true,
                      "note": "我的备注"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].source.kind").value("text"))
        .andExpect(jsonPath("$.items[0].processingStatus").value("succeeded"));

    verify(service).importText(any(), eq("text-controller"), any());
  }

  private static KnowledgeItemResponse response() {
    Instant now = Instant.parse("2026-10-22T00:00:00Z");
    return new KnowledgeItemResponse(
        UUID.randomUUID(),
        "AI 文字知识",
        "整理后的文字",
        null,
        1,
        "completed",
        "来源格式：AI 文字",
        "摘要",
        "succeeded",
        "active",
        false,
        new SourceSummary("text", "text", null, now),
        null,
        List.of(),
        1,
        now,
        now);
  }
}
