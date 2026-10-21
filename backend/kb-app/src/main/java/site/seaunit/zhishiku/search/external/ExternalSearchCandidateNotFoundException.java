package site.seaunit.zhishiku.search.external;

public class ExternalSearchCandidateNotFoundException extends RuntimeException {
  public ExternalSearchCandidateNotFoundException() {
    super("Internet search candidate was not found or has expired.");
  }
}
