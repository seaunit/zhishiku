package site.seaunit.zhishiku.richtext.image;

public record ImportImageCandidate(byte[] bytes, String filename, String mediaType) {
  public ImportImageCandidate {
    bytes = bytes == null ? new byte[0] : bytes.clone();
    filename = filename == null ? "" : filename;
    mediaType = mediaType == null ? "" : mediaType;
  }
}
