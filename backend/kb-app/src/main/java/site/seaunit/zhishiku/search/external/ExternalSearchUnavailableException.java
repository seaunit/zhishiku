package site.seaunit.zhishiku.search.external;

public class ExternalSearchUnavailableException extends RuntimeException {
  private final Integer upstreamStatus;

  public ExternalSearchUnavailableException(String message) {
    this(message, null);
  }

  public ExternalSearchUnavailableException(String message, Integer upstreamStatus) {
    super(message);
    this.upstreamStatus = upstreamStatus;
  }

  public Integer upstreamStatus() {
    return upstreamStatus;
  }
}
