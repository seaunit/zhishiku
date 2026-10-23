package site.seaunit.zhishiku.documentimport;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;
import site.seaunit.zhishiku.ocr.TesseractOcrService;
import site.seaunit.zhishiku.richtext.image.ImageInspectionException;
import site.seaunit.zhishiku.richtext.image.ImageInspector;
import site.seaunit.zhishiku.richtext.image.ImageNormalizer;
import site.seaunit.zhishiku.richtext.image.ImportImageCandidate;
import site.seaunit.zhishiku.webimport.WebImportUnavailableException;

@Component
public class DocumentTextExtractor {
  private static final long MAX_FILE_BYTES = 25L * 1024 * 1024;
  private static final int MIN_TEXT_CHARS = 80;
  private static final int MIN_IMAGE_TEXT_CHARS = 30;
  private static final int MAX_PDF_OCR_PAGES = 50;
  private static final int MAX_SOURCE_IMAGES = 12;
  private static final int MAX_IMAGE_EDGE = 8_192;
  private static final long MAX_IMAGE_PIXELS = 40_000_000L;
  private static final long MAX_IMAGE_COLLECTION_BYTES = 50L * 1024 * 1024;
  private static final Set<String> IMAGE_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif");
  private static final Set<String> NORMALIZE_FIRST_IMAGE_TYPES = Set.of("image/heic", "image/heif");
  private static final Set<String> SUPPORTED_TYPES =
      Set.of(
          "application/pdf",
          "application/msword",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "application/rtf",
          "text/plain",
          "text/markdown",
          "image/jpeg",
          "image/png",
          "image/webp",
          "image/heic",
          "image/heif");

  private final int maxTextChars;
  private final int timeoutSeconds;
  private final ImageInspector imageInspector;
  private final ImageNormalizer imageNormalizer;
  private final TesseractOcrService ocr;
  private final Tika tika = new Tika();

  public DocumentTextExtractor(
      @Value("${app.document-import.max-text-chars:60000}") int maxTextChars,
      @Value("${app.document-import.timeout-seconds:25}") int timeoutSeconds,
      ImageInspector imageInspector,
      ImageNormalizer imageNormalizer,
      TesseractOcrService ocr) {
    this.maxTextChars = Math.max(1_000, maxTextChars);
    this.timeoutSeconds = Math.max(5, timeoutSeconds);
    this.imageInspector = imageInspector;
    this.imageNormalizer = imageNormalizer;
    this.ocr = ocr;
  }

  public ExtractedDocument extract(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw failure("DOCUMENT_EMPTY", HttpStatus.UNPROCESSABLE_ENTITY, "The document is empty.");
    }
    if (file.getSize() > MAX_FILE_BYTES) {
      throw failure(
          "DOCUMENT_TOO_LARGE",
          HttpStatus.PAYLOAD_TOO_LARGE,
          "The document exceeds the 25 MiB limit.");
    }
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException error) {
      throw failure(
          "DOCUMENT_IMPORT_UNAVAILABLE",
          HttpStatus.SERVICE_UNAVAILABLE,
          "The document could not be read.");
    }
    String mediaType;
    try (TikaInputStream stream = TikaInputStream.get(bytes)) {
      mediaType =
          tika.detect(stream, safeFileName(file.getOriginalFilename())).toLowerCase(Locale.ROOT);
    } catch (IOException error) {
      throw failure(
          "DOCUMENT_IMPORT_UNAVAILABLE",
          HttpStatus.SERVICE_UNAVAILABLE,
          "The document format could not be detected.");
    }
    if (!SUPPORTED_TYPES.contains(mediaType)) {
      throw failure(
          "DOCUMENT_UNSUPPORTED",
          HttpStatus.UNSUPPORTED_MEDIA_TYPE,
          "Only PDF, Word, RTF, TXT, Markdown, JPEG, PNG, and WebP files are supported.");
    }
    if (IMAGE_TYPES.contains(mediaType)) {
      return extractImageText(bytes, mediaType, safeFileName(file.getOriginalFilename()));
    }

    String text = parseWithTimeout(bytes, mediaType);
    if ("application/pdf".equals(mediaType)
        && text.codePoints().filter(Character::isLetterOrDigit).count() < MIN_TEXT_CHARS) {
      text = extractScannedPdfText(bytes);
    }
    if (text.length() > maxTextChars) {
      throw documentTooLong();
    }
    if (text.codePoints().filter(Character::isLetterOrDigit).count() < MIN_TEXT_CHARS) {
      throw failure(
          "DOCUMENT_NO_TEXT",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "The document did not contain enough extractable text.");
    }
    return new ExtractedDocument(
        text, mediaType, text.length(), extractEmbeddedImages(bytes, mediaType));
  }

  private String extractScannedPdfText(byte[] bytes) {
    var executor =
        Executors.newSingleThreadExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "zhishiku-pdf-ocr");
              thread.setDaemon(true);
              return thread;
            });
    Future<String> future = executor.submit(() -> ocrPdf(bytes));
    try {
      return future.get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (TimeoutException error) {
      future.cancel(true);
      throw failure(
          "DOCUMENT_PARSE_TIMEOUT",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Scanned PDF OCR took too long.");
    } catch (InterruptedException error) {
      future.cancel(true);
      Thread.currentThread().interrupt();
      throw failure(
          "DOCUMENT_IMPORT_UNAVAILABLE",
          HttpStatus.SERVICE_UNAVAILABLE,
          "Scanned PDF OCR was interrupted.");
    } catch (ExecutionException error) {
      if (error.getCause() instanceof DocumentImportException documentError) {
        throw documentError;
      }
      if (error.getCause() instanceof WebImportUnavailableException) {
        throw failure(
            "DOCUMENT_IMPORT_UNAVAILABLE",
            HttpStatus.SERVICE_UNAVAILABLE,
            "Scanned PDF OCR is unavailable.");
      }
      throw parseFailure();
    } finally {
      executor.shutdownNow();
    }
  }

  private String ocrPdf(byte[] bytes) throws IOException {
    try (PDDocument document = Loader.loadPDF(bytes)) {
      if (document.getNumberOfPages() > MAX_PDF_OCR_PAGES) {
        throw documentTooLong();
      }
      PDFRenderer renderer = new PDFRenderer(document);
      StringBuilder text = new StringBuilder();
      for (int page = 0; page < document.getNumberOfPages(); page++) {
        BufferedImage image = renderer.renderImageWithDPI(page, 200, ImageType.RGB);
        Path imageFile = Files.createTempFile("zhishiku-pdf-ocr-", ".png");
        try {
          ImageIO.write(image, "png", imageFile.toFile());
          String pageText = ocr.recognize(imageFile);
          if (pageText != null && !pageText.isBlank()) {
            if (!text.isEmpty()) {
              text.append("\n\n");
            }
            text.append(pageText.strip());
          }
          if (text.length() > maxTextChars) {
            throw documentTooLong();
          }
        } finally {
          Files.deleteIfExists(imageFile);
        }
      }
      return normalize(text.toString());
    }
  }

  private ExtractedDocument extractImageText(byte[] bytes, String mediaType, String filename) {
    byte[] safeBytes = bytes;
    if (NORMALIZE_FIRST_IMAGE_TYPES.contains(mediaType)) {
      try {
        safeBytes = imageNormalizer.normalize(bytes);
      } catch (ImageInspectionException error) {
        throw failure(
            "DOCUMENT_IMAGE_INVALID",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "The HEIC or HEIF image could not be normalized safely.");
      }
    }
    try {
      imageInspector.inspect(safeBytes);
    } catch (ImageInspectionException error) {
      throw failure(
          "DOCUMENT_IMAGE_INVALID",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "The image was rejected by the document image safety checks.");
    }
    if ("image/png".equals(mediaType) && containsAscii(bytes, "acTL")) {
      throw failure(
          "DOCUMENT_IMAGE_INVALID",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Animated images are not accepted.");
    }

    byte[] normalized = safeBytes;
    if (!NORMALIZE_FIRST_IMAGE_TYPES.contains(mediaType)) {
      try {
        normalized = imageNormalizer.normalize(bytes);
      } catch (ImageInspectionException error) {
        throw failure(
            "DOCUMENT_IMPORT_UNAVAILABLE",
            HttpStatus.SERVICE_UNAVAILABLE,
            "Document image normalization is unavailable.");
      }
    }

    Path image = null;
    try {
      image = Files.createTempFile("zhishiku-document-image-", ".png");
      Files.write(image, normalized);
      String text = ocr.recognize(image);
      if (text.length() > maxTextChars) {
        throw documentTooLong();
      }
      if (text.codePoints().filter(Character::isLetterOrDigit).count() < MIN_IMAGE_TEXT_CHARS) {
        throw failure(
            "DOCUMENT_NO_TEXT",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "The document image did not contain enough readable text.");
      }
      return new ExtractedDocument(
          text,
          mediaType,
          text.length(),
          List.of(new ImportImageCandidate(normalized, filename, mediaType)));
    } catch (WebImportUnavailableException error) {
      throw failure(
          "DOCUMENT_IMPORT_UNAVAILABLE",
          HttpStatus.SERVICE_UNAVAILABLE,
          "Document image text recognition is unavailable.");
    } catch (IOException error) {
      throw failure(
          "DOCUMENT_IMPORT_UNAVAILABLE",
          HttpStatus.SERVICE_UNAVAILABLE,
          "Document image processing is unavailable.");
    } finally {
      if (image != null) {
        try {
          Files.deleteIfExists(image);
        } catch (IOException ignored) {
          image.toFile().deleteOnExit();
        }
      }
    }
  }

  private List<ImportImageCandidate> extractEmbeddedImages(byte[] bytes, String mediaType) {
    if ("application/pdf".equals(mediaType)) {
      return extractPdfImages(bytes);
    }
    if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        .equals(mediaType)) {
      return extractDocxImages(bytes);
    }
    return List.of();
  }

  private static List<ImportImageCandidate> extractPdfImages(byte[] bytes) {
    List<ImportImageCandidate> images = new ArrayList<>();
    Set<String> hashes = new HashSet<>();
    try (PDDocument document = Loader.loadPDF(bytes)) {
      int pageIndex = 0;
      for (PDPage page : document.getPages()) {
        pageIndex++;
        collectPdfImages(page.getResources(), "pdf-page-" + pageIndex, images, hashes, 0);
        if (images.size() >= MAX_SOURCE_IMAGES) {
          break;
        }
      }
    } catch (IOException | RuntimeException ignored) {
      // Missing embedded images must not discard usable document text.
    }
    return List.copyOf(images);
  }

  private static void collectPdfImages(
      PDResources resources,
      String prefix,
      List<ImportImageCandidate> images,
      Set<String> hashes,
      int depth) {
    if (resources == null || depth > 3 || images.size() >= MAX_SOURCE_IMAGES) {
      return;
    }
    int index = 0;
    for (COSName name : resources.getXObjectNames()) {
      if (images.size() >= MAX_SOURCE_IMAGES) {
        return;
      }
      index++;
      try {
        PDXObject object = resources.getXObject(name);
        if (object instanceof PDImageXObject image) {
          addPdfImage(image, prefix + "-image-" + index, images, hashes);
        } else if (object instanceof PDFormXObject form) {
          collectPdfImages(
              form.getResources(), prefix + "-form-" + index, images, hashes, depth + 1);
        }
      } catch (IOException | RuntimeException ignored) {
        // Continue with other resources in the same page.
      }
    }
  }

  private static void addPdfImage(
      PDImageXObject image,
      String filename,
      List<ImportImageCandidate> images,
      Set<String> hashes) {
    int width = image.getWidth();
    int height = image.getHeight();
    if (width < 80
        || height < 80
        || width > MAX_IMAGE_EDGE
        || height > MAX_IMAGE_EDGE
        || (long) width * height > MAX_IMAGE_PIXELS) {
      return;
    }
    try {
      BufferedImage rendered = image.getImage();
      if (rendered == null) {
        return;
      }
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      if (!ImageIO.write(rendered, "png", output)) {
        return;
      }
      byte[] png = output.toByteArray();
      addImageCandidate(images, hashes, png, filename + ".png", "image/png");
    } catch (IOException | RuntimeException ignored) {
      // Skip images that PDFBox cannot decode safely.
    }
  }

  private static List<ImportImageCandidate> extractDocxImages(byte[] bytes) {
    List<ImportImageCandidate> images = new ArrayList<>();
    Set<String> hashes = new HashSet<>();
    try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null && images.size() < MAX_SOURCE_IMAGES) {
        String name = entry.getName().toLowerCase(Locale.ROOT);
        if (!name.startsWith("word/media/")) {
          continue;
        }
        String mediaType = imageMediaType(name);
        if (mediaType.isBlank()) {
          continue;
        }
        byte[] image = zip.readNBytes((int) ImageInspector.MAX_SOURCE_BYTES + 1);
        if (image.length == 0 || image.length > ImageInspector.MAX_SOURCE_BYTES) {
          continue;
        }
        addImageCandidate(
            images, hashes, image, name.substring(name.lastIndexOf('/') + 1), mediaType);
      }
    } catch (IOException | RuntimeException ignored) {
      // Invalid embedded media is skipped while the extracted text remains usable.
    }
    return List.copyOf(images);
  }

  private static void addImageCandidate(
      List<ImportImageCandidate> images,
      Set<String> hashes,
      byte[] bytes,
      String filename,
      String mediaType) {
    if (images.size() >= MAX_SOURCE_IMAGES
        || totalImageBytes(images) + bytes.length > MAX_IMAGE_COLLECTION_BYTES) {
      return;
    }
    String hash = sha256(bytes);
    if (hash != null && hashes.add(hash)) {
      images.add(new ImportImageCandidate(bytes, filename, mediaType));
    }
  }

  private static long totalImageBytes(List<ImportImageCandidate> images) {
    return images.stream().mapToLong(image -> image.bytes().length).sum();
  }

  private static String imageMediaType(String name) {
    if (name.endsWith(".png")) {
      return "image/png";
    }
    if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
      return "image/jpeg";
    }
    if (name.endsWith(".webp")) {
      return "image/webp";
    }
    return "";
  }

  private static String sha256(byte[] bytes) {
    try {
      return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException error) {
      return null;
    }
  }

  private String parseWithTimeout(byte[] bytes, String mediaType) {
    var executor =
        Executors.newSingleThreadExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "zhishiku-document-parser");
              thread.setDaemon(true);
              return thread;
            });
    Future<String> future = executor.submit(() -> parseDocument(bytes, mediaType));
    try {
      return future.get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (TimeoutException error) {
      future.cancel(true);
      throw failure(
          "DOCUMENT_PARSE_TIMEOUT",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "The document took too long to parse.");
    } catch (InterruptedException error) {
      future.cancel(true);
      Thread.currentThread().interrupt();
      throw failure(
          "DOCUMENT_IMPORT_UNAVAILABLE",
          HttpStatus.SERVICE_UNAVAILABLE,
          "Document parsing was interrupted.");
    } catch (ExecutionException error) {
      Throwable cause = error.getCause();
      if (cause instanceof DocumentImportException documentError) {
        throw documentError;
      }
      if (cause instanceof WriteLimitReachedException) {
        throw documentTooLong();
      }
      throw parseFailure();
    } finally {
      executor.shutdownNow();
    }
  }

  private String parseDocument(byte[] bytes, String mediaType)
      throws IOException, SAXException, TikaException {
    BodyContentHandler handler = new BodyContentHandler(maxTextChars + 1);
    Metadata metadata = new Metadata();
    metadata.set(Metadata.CONTENT_TYPE, mediaType);
    ParseContext context = new ParseContext();
    Parser parser = new AutoDetectParser();
    context.set(Parser.class, parser);
    try (TikaInputStream stream = TikaInputStream.get(bytes)) {
      parser.parse(stream, handler, metadata, context);
    }
    return normalize(handler.toString());
  }

  private static String safeFileName(String value) {
    if (value == null || value.isBlank()) {
      return "document";
    }
    return value.replaceAll("[\\r\\n\\u0000]", " ").strip();
  }

  private static String normalize(String text) {
    return text.replace('\u0000', ' ')
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replaceAll("[\\t\\x0B\\f]+", " ")
        .replaceAll(" +\\n", "\n")
        .replaceAll("\\n{3,}", "\n\n")
        .strip();
  }

  private static boolean containsAscii(byte[] bytes, String value) {
    byte[] target = value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    outer:
    for (int index = 0; index <= bytes.length - target.length; index++) {
      for (int offset = 0; offset < target.length; offset++) {
        if (bytes[index + offset] != target[offset]) {
          continue outer;
        }
      }
      return true;
    }
    return false;
  }

  private static DocumentImportException documentTooLong() {
    return failure(
        "DOCUMENT_TOO_LONG",
        HttpStatus.UNPROCESSABLE_ENTITY,
        "The extracted document text exceeds the configured limit.");
  }

  private static DocumentImportException parseFailure() {
    return failure(
        "DOCUMENT_PARSE_FAILED",
        HttpStatus.UNPROCESSABLE_ENTITY,
        "The document could not be parsed safely.");
  }

  private static DocumentImportException failure(String code, HttpStatus status, String message) {
    return new DocumentImportException(code, status, message);
  }
}
