package site.seaunit.zhishiku.richtext.image;

public record ImageInspection(String mediaType, int width, int height, String extension) {
  public double aspectRatio() {
    return (double) width / height;
  }
}
