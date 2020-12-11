package ch.thn.file.filesystemwatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public abstract class AbstractWatcher {

  private static final Logger logger = LoggerFactory.getLogger(AbstractWatcher.class);

  private WatchService watcher = null;

  private ThreadFactory threadFactory = null;
  private Thread thread = null;
  private FileSystemWatchRunner runner = null;

  /**
   * Currently watched paths and their watch keys to identify them
   */
  private HashMap<WatchKey, Path> keys = null;

  /**
   * For each path there is a flag which defines if all its children should be added to the watch
   * list or not
   */
  private HashMap<WatchKey, Boolean> allChildren = null;

  /**
  *
  */
  public AbstractWatcher() {
    this(null);
  }

  /**
   *
   *
   * @param threadFactory The factory to use to create threads to run on. May be <code>null</code>.
   */
  public AbstractWatcher(ThreadFactory threadFactory) {
    this.threadFactory = threadFactory;
    keys = new HashMap<>();
    allChildren = new HashMap<>();
  }

  /**
   * Creates and starts a new system watch service (non-polling).
   *
   * @throws IOException If creating and starting the watch service fails
   *
   */
  public void create() throws IOException {
    try {
      watcher = FileSystems.getDefault().newWatchService();
    } catch (IOException exc) {
      throw new IOException("Failed to construct new watch service", exc);
    } catch (UnsupportedOperationException exc) {
      throw new IOException("File system seems not to support file system watching. "
          + "Try the polling functionality.", exc);
    }
  }

  /**
   * Creates and starts a new polling watch service.<br>
   * See {@link #start()} for the preferred use if the non-polling service is available.
   *
   * @param pollingTime The polling frequency
   * @param timeUnit The unit of the <code>pollingTime</code>
   * @throws IOException If creating and starting the watch service fails
   */
  public void create(long pollingTime, TimeUnit timeUnit) throws IOException {
    PollingWatchService polling = new PollingWatchService(pollingTime, timeUnit, threadFactory);
    try {
      polling.start();
    } catch (IOException exc) {
      polling.close();
      throw exc;
    }
    this.watcher = polling;
  }

  protected boolean watchingAllChildren(WatchKey key) {
    return allChildren.get(key);
  }

  /**
   * Returns an unmodifiable collection of all paths which are currently being watched
   *
   * @return
   */
  public Collection<Path> getWatchedPaths() {
    return Collections.unmodifiableCollection(keys.values());
  }

  public boolean isPolling() {
    return watcher instanceof PollingWatchService;
  }

  public boolean isRunning() {
    return runner != null && runner.isRunning();
  }

  /**
   *
   */
  public void clearAllRegisteredPaths() {
    // Cancel all old keys
    for (WatchKey k : keys.keySet()) {
      k.cancel();
    }

    keys.clear();

  }

  /**
   * Adds a new path to the list of watched paths. If a path to a file is given, its parent
   * directory is registered instead because only directories can be watched.
   *
   *
   * @param path
   * @param allChildren If set to <code>true</code>, all child directories are registered too
   * @param allParents If set to <code>true</code>, all parent directories are registered too
   * @return
   */
  public boolean registerPath(Path path, boolean allChildren, boolean allParents) {
    File f = path.toFile();

    if (f.isFile()) {
      // The path exists and it is a file -> get its parent directory
      path = path.getParent();
    } else if (!f.exists()) {
      // The path is not a file and does not exist
      return false;
    }

    if (!allChildren && !allParents) {
      return register(path, false);
    } else {
      if (allChildren) {
        registerAllChildren(path);
      }

      if (allParents) {
        registerAllParents(path);
      }
    }

    return true;
  }

  /**
   * Adds a new path to the list of watched paths. If a path to a file is given, its parent
   * directory is registered instead because only directories can be watched.
   *
   *
   * @param path
   * @param allChildren If set to <code>true</code>, all child directories are registered too
   * @param allParents If set to <code>true</code>, all parent directories are registered too
   * @return
   */
  public boolean registerPath(String path, boolean allChildren, boolean allParents) {
    return registerPath(Paths.get(path), allChildren, allParents);
  }

  /**
   * Adds a new path to the list of watched paths.
   *
   * @param path
   * @return
   */
  public boolean registerPath(String path) {
    return registerPath(path, false, false);
  }

  /**
   * Adds a new path to the list of watched paths.
   *
   * @param path
   * @return
   */
  public boolean registerPath(Path path) {
    return registerPath(path, false, false);
  }

  /**
   * Registers the given path for all the events
   *
   * @param dir
   * @param allChildren
   * @return
   * @throws IOException
   */
  private boolean register(Path dir, boolean allChildren) {
    if (watcher == null) {
      throw new IllegalStateException("No watch service created yet");
    }

    WatchKey key = null;

    try {
      if (isPolling()) {
        key = ((PollingWatchService) watcher).register(dir);
      } else {
        // 'Path' only accepts an sun.nio.fsAbstractWatchService (checked internally with
        // 'instanceof') which contains a 'register' method that will get called
        key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.OVERFLOW);
      }
    } catch (Exception e) {
      throw new PathWatcherError("Failed to register path "
          + dir, e);
    }

    // If its the same one it will just be updated
    keys.put(key, dir);
    this.allChildren.put(key, allChildren);

    return true;
  }

  /**
   * Walks through the file tree and registers all child paths
   *
   * @param path
   * @throws IOException
   */
  protected void registerAllChildren(Path path) {

    try {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
          try {
            register(dir, true);
          } catch (Exception e) {
            logger.warn("Failed to recursively register child path {}", dir, e);
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      logger.warn("Failed to recursively register path and children of {}", path, e);
    }

  }

  /**
   * Walks upwards through the file tree and registers all parent paths
   *
   * @param path
   * @throws IOException
   */
  private void registerAllParents(Path path) {
    Path p = path;

    while (p != null) {
      try {
        register(p, false);
      } catch (Exception e) {
        logger.warn("Failed to recursively register parent path {}", path, e);
      }
      p = p.getParent();
    }

  }

  private Path getPath(WatchKey key) {
    return keys.get(key);
  }

  /**
   *
   *
   * @param key
   */
  private void processEvent(WatchKey key) {

    Path dir = getPath(key);
    process(key, dir);

    boolean valid = key.reset();
    if (!valid) {
      // Directory not accessible any more -> remove it
      keys.remove(key);
    }

  }

  private void process(WatchKey key, Path dir) {
    for (WatchEvent<?> event : key.pollEvents()) {
      Kind<?> kind = event.kind();

      // TODO is overflow handled correctly?
      if (event == StandardWatchEventKinds.OVERFLOW) {
        processEvent(kind, dir, null, true);
        continue;
      }

      @SuppressWarnings("unchecked")
      WatchEvent<Path> ev = (WatchEvent<Path>) event;
      Path name = ev.context();
      Path child = dir.resolve(name);

      processEvent(kind, dir, child, false);

      // Add new directories and their child directories to the watch
      if (watchingAllChildren(key) && kind == StandardWatchEventKinds.ENTRY_CREATE) {
        if (child.toFile().isDirectory()) {
          registerAllChildren(child);
        }
      }

    }
  }

  /**
   *
   *
   * @param eventKind
   * @param registeredPath The path which is registered in the watcher to be watched
   * @param eventPath The path of the event, the same as <code>registeredPath</code> or within
   *        <code>registeredPath</code>
   * @param overflow
   */
  protected abstract void processEvent(Kind<?> eventKind, Path registeredPath, Path eventPath,
      boolean overflow);

  /**
   *
   *
   */
  public void start() {
    if (watcher == null) {
      throw new IllegalStateException("No watch service created yet");
    }
    if (runner != null && runner.isRunning()) {
      throw new IllegalStateException("Watch service is already running");
    }

    runner = new FileSystemWatchRunner(watcher, this::processEvent);
    if (threadFactory != null) {
      thread = threadFactory.newThread(runner);
    } else {
      thread = new Thread(runner);
    }
    thread.start();
  }

  /**
   *
   *
   * @throws IOException
   */
  public void stop() throws IOException {
    if (runner == null || !runner.isRunning()) {
      throw new IllegalStateException("Watch service is not running");
    }

    stopWatchService(0, null);
  }

  /**
   *
   *
   * @param waitTimeout The amount of time to wait until the thread finished
   * @param timeUnit The unit of the provided time
   * @throws IOException
   */
  public void stop(long waitTimeout, TimeUnit timeUnit) throws IOException {
    if (runner == null || !runner.isRunning()) {
      throw new IllegalStateException("Watch service is not running");
    }

    stopWatchService(waitTimeout, timeUnit);
  }

  /**
   *
   *
   * @param waitTimeout The amount of time to wait until the thread finished. Not used if < 0. A
   *        value of 0 (zero) means wait forever.
   * @param timeUnit The unit of the provided time. Can be <code>null</code>.
   * @throws IOException
   */
  private void stopWatchService(long waitTimeout, TimeUnit timeUnit) throws IOException {
    if (watcher == null) {
      throw new IllegalStateException("No watch service created yet");
    }
    if (isPolling()) {
      PollingWatchService polling = (PollingWatchService) watcher;
      if (waitTimeout >= 0 && timeUnit != null) {
        polling.close(waitTimeout, timeUnit);
      } else {
        polling.close();
      }
    } else {
      watcher.close();
    }

    runner.stop();

    try {
      thread.interrupt();
      if (waitTimeout >= 0 && timeUnit != null) {
        thread.join(timeUnit.toMillis(waitTimeout));
      }
    } catch (final InterruptedException exc) {
      Thread.currentThread().interrupt();
    }
  }



}
