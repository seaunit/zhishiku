package site.seaunit.zhishiku.auth.registration;

public class RateLimitedException extends RuntimeException {
  private final long retryAfterSeconds;

  public RateLimitedException(long retryAfterSeconds) {
    super("Too many requests. Try again later.");
    this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
  }

  public long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}
