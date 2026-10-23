package site.seaunit.zhishiku.documentimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import site.seaunit.zhishiku.ocr.TesseractOcrService;
import site.seaunit.zhishiku.richtext.image.ImageInspector;
import site.seaunit.zhishiku.richtext.image.ImageNormalizer;

class DocumentTextExtractorTest {
  private static final byte[] PNG =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2nGQAAAAASUVORK5CYII=");

  private final TesseractOcrService ocr = mock(TesseractOcrService.class);
  private final ImageNormalizer normalizer = source -> source;
  private final DocumentTextExtractor extractor =
      new DocumentTextExtractor(60_000, 25, new ImageInspector(), normalizer, ocr);

  @Test
  void extractsTextFromDocxByDetectedContent() throws Exception {
    byte[] bytes;
    try (XWPFDocument document = new XWPFDocument();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      document
          .createParagraph()
          .createRun()
          .setText(
              "This document contains enough extractable text for safe knowledge organization. "
                  + "It also contains a second sentence that should remain searchable.");
      document.write(output);
      bytes = output.toByteArray();
    }

    ExtractedDocument extracted =
        extractor.extract(
            new MockMultipartFile("file", "example.docx", "application/octet-stream", bytes));

    assertThat(extracted.mediaType())
        .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    assertThat(extracted.text()).contains("extractable text").contains("second sentence");
  }

  @Test
  void extractsEmbeddedDocxImagesAsSafeImportCandidates() throws Exception {
    byte[] bytes;
    try (XWPFDocument document = new XWPFDocument();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      document
          .createParagraph()
          .createRun()
          .setText(
              "This document contains a diagram and enough explanatory text for safe import. "
                  + "The accompanying description remains searchable after knowledge organization.");
      document
          .createParagraph()
          .createRun()
          .addPicture(
              new ByteArrayInputStream(PNG), Document.PICTURE_TYPE_PNG, "diagram.png", 120, 80);
      document.write(output);
      bytes = output.toByteArray();
    }

    ExtractedDocument extracted =
        extractor.extract(
            new MockMultipartFile("file", "diagram.docx", "application/octet-stream", bytes));

    assertThat(extracted.images()).hasSize(1);
    assertThat(extracted.images().get(0).mediaType()).isEqualTo("image/png");
    assertThat(extracted.images().get(0).filename()).contains("image1.png");
  }

  @Test
  void extractsTextFromPdfByDetectedContent() throws Exception {
    byte[] bytes;
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      PDPage page = new PDPage();
      document.addPage(page);
      try (PDPageContentStream content = new PDPageContentStream(document, page)) {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        content.newLineAtOffset(72, 700);
        content.showText(
            "This PDF contains enough extractable text for safe knowledge organization. "
                + "It also remains searchable after import.");
        content.endText();
      }
      document.save(output);
      bytes = output.toByteArray();
    }

    ExtractedDocument extracted =
        extractor.extract(
            new MockMultipartFile("file", "example.pdf", "application/octet-stream", bytes));

    assertThat(extracted.mediaType()).isEqualTo("application/pdf");
    assertThat(extracted.text()).contains("safe knowledge organization");
  }

  @Test
  void rejectsPdfWithoutATextLayer() throws Exception {
    byte[] bytes;
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      document.addPage(new PDPage());
      document.save(output);
      bytes = output.toByteArray();
    }

    MockMultipartFile file = new MockMultipartFile("file", "scan.pdf", "application/pdf", bytes);

    assertThatThrownBy(() -> extractor.extract(file))
        .isInstanceOf(DocumentImportException.class)
        .extracting(error -> ((DocumentImportException) error).getCode())
        .isEqualTo("DOCUMENT_NO_TEXT");
  }

  @Test
  void fallsBackToPageOcrForScannedPdf() throws Exception {
    when(ocr.recognize(any())).thenReturn("扫描版 PDF 中的完整识别正文".repeat(20));
    byte[] bytes;
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      document.addPage(new PDPage());
      document.save(output);
      bytes = output.toByteArray();
    }

    ExtractedDocument extracted =
        extractor.extract(new MockMultipartFile("file", "scanned.pdf", "application/pdf", bytes));

    assertThat(extracted.mediaType()).isEqualTo("application/pdf");
    assertThat(extracted.text()).contains("扫描版 PDF 中的完整识别正文");
  }

  @Test
  void rejectsUnsupportedBinaryContent() {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "archive.zip",
            "application/zip",
            "PK\u0003\u0004not-a-document".getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> extractor.extract(file))
        .isInstanceOf(DocumentImportException.class)
        .extracting(error -> ((DocumentImportException) error).getCode())
        .isEqualTo("DOCUMENT_UNSUPPORTED");
  }

  @Test
  void rejectsPlainTextWithoutEnoughContent() {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "empty.txt", "text/plain", "short".getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> extractor.extract(file))
        .isInstanceOf(DocumentImportException.class)
        .extracting(error -> ((DocumentImportException) error).getCode())
        .isEqualTo("DOCUMENT_NO_TEXT");
  }

  @Test
  void rejectsFilesOverTwentyFiveMebibytesBeforeReading() {
    MultipartFile file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(25L * 1024 * 1024 + 1);

    assertThatThrownBy(() -> extractor.extract(file))
        .isInstanceOf(DocumentImportException.class)
        .extracting(error -> ((DocumentImportException) error).getCode())
        .isEqualTo("DOCUMENT_TOO_LARGE");
  }

  @Test
  void rejectsExtractedTextOverTheConfiguredLimit() {
    DocumentTextExtractor limited =
        new DocumentTextExtractor(1_000, 25, new ImageInspector(), normalizer, ocr);
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "long.txt", "text/plain", "A".repeat(2_000).getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> limited.extract(file))
        .isInstanceOf(DocumentImportException.class)
        .extracting(error -> ((DocumentImportException) error).getCode())
        .isEqualTo("DOCUMENT_TOO_LONG");
  }

  @Test
  void recognizesDocumentImageTextWithoutPersistingTheImage() {
    when(ocr.recognize(any())).thenReturn("文档图片中的完整识别正文".repeat(10));
    MockMultipartFile file = new MockMultipartFile("file", "notes.png", "image/png", PNG);

    ExtractedDocument extracted = extractor.extract(file);

    assertThat(extracted.mediaType()).isEqualTo("image/png");
    assertThat(extracted.text()).contains("文档图片中的完整识别正文");
    assertThat(extracted.images()).hasSize(1);
  }

  @Test
  void normalizesHeicBeforeInspectionAndOcr() {
    when(ocr.recognize(any())).thenReturn("HEIC 文档图片中的完整识别正文".repeat(10));
    DocumentTextExtractor heicExtractor =
        new DocumentTextExtractor(60_000, 25, new ImageInspector(), source -> PNG, ocr);
    byte[] heic = new byte[32];
    System.arraycopy("ftypheic".getBytes(StandardCharsets.US_ASCII), 0, heic, 4, 8);
    MockMultipartFile file = new MockMultipartFile("file", "notes.heic", "image/heic", heic);

    ExtractedDocument extracted = heicExtractor.extract(file);

    assertThat(extracted.mediaType()).isEqualTo("image/heic");
    assertThat(extracted.text()).contains("HEIC 文档图片中的完整识别正文");
  }

  @Test
  void rejectsDocumentImagesWithoutEnoughReadableText() {
    when(ocr.recognize(any())).thenReturn("短");
    MockMultipartFile file = new MockMultipartFile("file", "blank.png", "image/png", PNG);

    assertThatThrownBy(() -> extractor.extract(file))
        .isInstanceOf(DocumentImportException.class)
        .extracting(error -> ((DocumentImportException) error).getCode())
        .isEqualTo("DOCUMENT_NO_TEXT");
  }

  @Test
  void rejectsDocumentImagesWithObviousEmbeddedSecrets() {
    byte[] withSecret =
        (new String(PNG, StandardCharsets.ISO_8859_1) + "password=abcdefgh")
            .getBytes(StandardCharsets.ISO_8859_1);
    MockMultipartFile file = new MockMultipartFile("file", "secret.png", "image/png", withSecret);

    assertThatThrownBy(() -> extractor.extract(file))
        .isInstanceOf(DocumentImportException.class)
        .extracting(error -> ((DocumentImportException) error).getCode())
        .isEqualTo("DOCUMENT_IMAGE_INVALID");
  }

  @Test
  void rejectsAnimatedDocumentImages() {
    byte[] animated = Arrays.copyOf(PNG, PNG.length + 4);
    System.arraycopy("acTL".getBytes(StandardCharsets.US_ASCII), 0, animated, PNG.length, 4);
    MockMultipartFile file = new MockMultipartFile("file", "animated.png", "image/png", animated);

    assertThatThrownBy(() -> extractor.extract(file))
        .isInstanceOf(DocumentImportException.class)
        .extracting(error -> ((DocumentImportException) error).getCode())
        .isEqualTo("DOCUMENT_IMAGE_INVALID");
  }
}
