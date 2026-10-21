package site.seaunit.zhishiku.storage;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;

public interface ObjectStorage {
  String store(String storageKey, InputStream input, long size, String contentType)
      throws IOException;

  Resource read(String storageKey);

  void delete(String storageKey);
}
