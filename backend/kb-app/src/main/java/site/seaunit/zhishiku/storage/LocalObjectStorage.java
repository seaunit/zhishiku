package site.seaunit.zhishiku.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import site.seaunit.zhishiku.config.AppProperties;

@Component
public class LocalObjectStorage implements ObjectStorage {
  private final Path root;

  public LocalObjectStorage(AppProperties properties) {
    this.root = Path.of(properties.storage().root()).toAbsolutePath().normalize();
    try {
      Files.createDirectories(root);
    } catch (IOException error) {
      throw new IllegalStateException("Could not initialize local object storage", error);
    }
  }

  @Override
  public String store(String storageKey, InputStream input, long size, String contentType)
      throws IOException {
    Path target = resolve(storageKey);
    Files.createDirectories(target.getParent());
    Files.copy(input, target);
    return storageKey;
  }

  @Override
  public Resource read(String storageKey) {
    Path path = resolve(storageKey);
    if (!Files.isRegularFile(path)) {
      throw new IllegalArgumentException("Stored object does not exist.");
    }
    return new FileSystemResource(path);
  }

  @Override
  public void delete(String storageKey) {
    try {
      Files.deleteIfExists(resolve(storageKey));
    } catch (IOException ignored) {
      // Object cleanup is retried by the lifecycle worker.
    }
  }

  private Path resolve(String storageKey) {
    Path path = root.resolve(storageKey).normalize();
    if (!path.startsWith(root)) {
      throw new IllegalArgumentException("Invalid storage key.");
    }
    return path;
  }
}
