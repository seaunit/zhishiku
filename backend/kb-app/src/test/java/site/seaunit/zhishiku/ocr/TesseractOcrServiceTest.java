package site.seaunit.zhishiku.ocr;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import site.seaunit.zhishiku.config.AppProperties;
import site.seaunit.zhishiku.webimport.WebImportUnavailableException;

class TesseractOcrServiceTest {
  @Test
  void drainsLargeProcessOutputWithoutDeadlocking() {
    AppProperties properties = mock(AppProperties.class);
    when(properties.webImport())
        .thenReturn(new AppProperties.WebImport("", "tesseract", 35, 30, 30_000, 20));
    TesseractOcrService service = new TesseractOcrService(properties);
    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();

    assertThatThrownBy(
            () ->
                service.runProcess(
                    List.of(
                        java,
                        "-cp",
                        System.getProperty("java.class.path"),
                        LargeOutputProcess.class.getName()),
                    5))
        .isInstanceOf(WebImportUnavailableException.class)
        .hasMessageContaining("exceeded");
  }

  public static final class LargeOutputProcess {
    private LargeOutputProcess() {}

    public static void main(String[] arguments) {
      System.out.print("x".repeat(200_000));
    }
  }
}
