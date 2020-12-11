package ch.thn.file.filesystemwatcher;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;

/**
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class PathWatchEvent {

  private final Path registeredPath;
  private final Path path;
  private final Kind<?> kind;

  public PathWatchEvent(Path registeredPath, Path path, Kind<?> kind) {
    this.registeredPath = registeredPath;
    this.path = path;
    this.kind = kind;
  }

  /**
   * The path which is registered in the watcher to trigger events.
   *
   * @return The path
   */
  public Path getRegisteredPath() {
    return registeredPath;
  }

  /**
   * The path of the event, same or within the {@link #registeredPath}.
   *
   * @return The path
   */
  public Path getPath() {
    return path;
  }

  /**
   * The event kind.<br>
   * Usually as {@link StandardWatchEventKinds}, but can depend on the underlying watch service
   * implementation.
   *
   * @return The kind of event that got triggered
   */
  public Kind<?> getKind() {
    return kind;
  }

}
