package ch.thn.file.filesystemwatcher;

import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.function.Consumer;

/**
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class FileSystemWatchRunner implements Runnable {

  private final WatchService watcher;

  private boolean isRunning = false;
  private boolean canStop = false;

  private final Consumer<WatchKey> processEventFunction;

  public FileSystemWatchRunner(WatchService watcher, Consumer<WatchKey> processEventFunction) {
    this.watcher = watcher;
    this.processEventFunction = processEventFunction;

  }

  public boolean isRunning() {
    return isRunning;
  }

  public void stop() {
    canStop = true;
  }


  @Override
  public void run() {
    isRunning = true;

    while (!canStop) {

      WatchKey key = null;

      try {
        key = watcher.take();
      } catch (InterruptedException exc) {
        Thread.currentThread().interrupt();
        continue;
      } catch (ClosedWatchServiceException exc) {
        break;
      }

      processEventFunction.accept(key);
      key.reset();
    }

    isRunning = false;
  }

}
