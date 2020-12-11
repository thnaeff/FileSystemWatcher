/**
 * Copyright 2014 Thomas Naeff (github.com/thnaeff)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package ch.thn.file.filesystemwatcher;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of a {@link WatchService} which periodically checks the registered paths for
 * changes.
 *
 *
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class PollingWatchService implements WatchService {

  private static final Logger logger = LoggerFactory.getLogger(PollingWatchService.class);

  private Set<PollingWatchKey> registeredAndActiveKeys = null;

  /**
   * A FIFO list of all the watch keys which have events pending
   */
  private LinkedBlockingQueue<PollingWatchKey> keysWithEvents = null;

  private final FileAlterationMonitor monitor;
  private final FileAlterationListener listener;

  private boolean isReady = false;

  /**
   *
   *
   * @param pollTimeout The timeout to wait between two file checks (the polling interval).
   * @param timeUnit The unit of the poll timeout
   */
  public PollingWatchService(long pollTimeout, TimeUnit timeUnit) {
    this(pollTimeout, timeUnit, null);
  }

  /**
   *
   *
   * @param pollTimeout The timeout to wait between two file checks (the polling interval).
   * @param timeUnit The unit of the poll timeout
   * @param threadFactory The factory to use for creating new threads to run the polling on. Can be
   *        <code>null</code>.
   */
  public PollingWatchService(long pollTimeout, TimeUnit timeUnit, ThreadFactory threadFactory) {
    long millis = timeUnit.toMillis(pollTimeout);

    monitor = new FileAlterationMonitor(millis);
    if (threadFactory != null) {
      monitor.setThreadFactory(threadFactory);
    }

    listener = new PollingFileAlterationListener(this::isReady, this::changeHappened);
    keysWithEvents = new LinkedBlockingQueue<>();
    registeredAndActiveKeys = Collections.synchronizedSet(new HashSet<>());
  }

  public void start() throws IOException {
    try {
      monitor.start();
    } catch (Exception exc) {
      throw new IOException("Failed to start monitor", exc);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      monitor.stop();
      isReady = false;
    } catch (Exception exc) {
      throw new IOException("Error when stopping monitor", exc);
    }
  }

  public void close(long timeout, TimeUnit timeUnit) throws IOException {
    try {
      monitor.stop(timeUnit.toMillis(timeout));
      isReady = false;
    } catch (Exception exc) {
      throw new IOException("Error when stopping monitor", exc);
    }
  }

  @Override
  public PollingWatchKey poll() {
    PollingWatchKey key = keysWithEvents.poll();
    registeredAndActiveKeys.remove(key);
    return key;
  }

  @Override
  public PollingWatchKey poll(long timeout, TimeUnit unit) throws InterruptedException {
    PollingWatchKey key = keysWithEvents.poll(timeout, unit);
    registeredAndActiveKeys.remove(key);
    return key;
  }


  @Override
  public PollingWatchKey take() throws InterruptedException {
    PollingWatchKey key = keysWithEvents.take();
    registeredAndActiveKeys.remove(key);
    return key;
  }

  /**
   *
   *
   * @param key
   */
  private void resetKey(PollingWatchKey key) {
    registeredAndActiveKeys.add(key);

    // TODO after resetting a key, look through events to see if another event has happened for this
    // key
  }

  /**
   * Gets the keys which were used to register the given changed path (directory or file). A path
   * can be registered by:<br>
   * - The direct path, triggering changes to that file/directory itself<br>
   * - Its parent path, triggering changes within the directory<br>
   * <br>
   * This lookup will check the provided <code>path</code> and the parent of the provided
   * <code>path</code>.
   *
   * @param path The file or directory to check
   * @return
   */
  private Set<PollingWatchKey> getKeys(Path path) {
    Path parentPath = path.getParent();
    Set<PollingWatchKey> keys = new HashSet<>();
    for (PollingWatchKey key : registeredAndActiveKeys) {
      Path keyPath = key.getRegisteredPath();
      // Check against path itself and against the parent path.
      // The changed path could be a file/directory within a registered directory.
      if (keyPath.equals(path) || keyPath.equals(parentPath)) {
        keys.add(key);
      }
    }
    return keys;
  }

  /**
   *
   *
   * @param event
   */
  private void changeHappened(PollingWatchEvent event) {
    Path path = event.context();
    Set<PollingWatchKey> keys = getKeys(path);
    for (PollingWatchKey key : keys) {
      key.addWatchEvent(event);
      keysWithEvents.add(key);
    }
  }

  private void isReady(boolean ready) {
    logger.debug("Is ready: {}", ready);
    this.isReady = ready;
  }

  public boolean isReady() {
    return isReady;
  }

  /**
   *
   *
   * @param path
   * @return
   */
  public PollingWatchKey register(Path path) {
    return register(path, null);
  }


  /**
   *
   *
   * @param path
   * @param fileFilter
   * @return
   */
  public PollingWatchKey register(Path path, FileFilter fileFilter) {
    FileAlterationObserver observer = new FileAlterationObserver(path.toFile(), fileFilter);
    observer.addListener(listener);
    monitor.addObserver(observer);
    PollingWatchKey key = new PollingWatchKey(path, this::resetKey);
    registeredAndActiveKeys.add(key);
    return key;
  }


  /*******************************************************************************************
   *
   *
   * @author Thomas Naeff (github.com/thnaeff)
   *
   */
  protected class PollingFileAlterationListener implements FileAlterationListener {

    private final Consumer<Boolean> readyFunction;
    private final Consumer<PollingWatchEvent> eventFunction;

    private final Set<PollingWatchEvent> events;

    private volatile boolean isReady = false;

    public PollingFileAlterationListener(Consumer<Boolean> readyFunction,
        Consumer<PollingWatchEvent> eventFunction) {
      this.readyFunction = readyFunction;
      this.eventFunction = eventFunction;

      this.events = new HashSet<>();

    }

    @Override
    public void onStart(FileAlterationObserver observer) {
      logger.debug("Checking for changes: {}", observer);
    }

    @Override
    public void onDirectoryCreate(File directory) {
      logger.debug("onDirectoryCreate: {}", directory);
      // TODO Should the event happen on the parent directory? Signalling that this directory was
      // created as new entry in the parent?
      Path path = directory.toPath();
      PollingWatchEvent event = new PollingWatchEvent(path, StandardWatchEventKinds.ENTRY_CREATE);
      events.add(event);
    }

    @Override
    public void onDirectoryChange(File directory) {
      logger.debug("onDirectoryChange: {}", directory);
      // TODO Should the event happen on the parent directory? Signalling that this directory was
      // created as new entry in the parent?
      Path path = directory.toPath();
      PollingWatchEvent event = new PollingWatchEvent(path, StandardWatchEventKinds.ENTRY_MODIFY);
      events.add(event);
    }

    @Override
    public void onDirectoryDelete(File directory) {
      logger.debug("onDirectoryDelete: {}", directory);
      // TODO Should the event happen on the parent directory? Signalling that this directory was
      // created as new entry in the parent?
      Path path = directory.toPath();
      PollingWatchEvent event = new PollingWatchEvent(path, StandardWatchEventKinds.ENTRY_DELETE);
      events.add(event);
    }

    @Override
    public void onFileCreate(File file) {
      logger.debug("onFileCreate: {}", file);
      // TODO Should the event happen on the parent directory? Signalling that this file was
      // created as new entry in the parent?
      Path path = file.toPath();
      PollingWatchEvent event = new PollingWatchEvent(path, StandardWatchEventKinds.ENTRY_CREATE);
      events.add(event);
    }

    @Override
    public void onFileChange(File file) {
      logger.debug("onFileChange: {}", file);
      // TODO Should the event happen on the parent directory? Signalling that this file was
      // created as new entry in the parent?
      Path path = file.toPath();
      PollingWatchEvent event = new PollingWatchEvent(path, StandardWatchEventKinds.ENTRY_MODIFY);
      events.add(event);
    }

    @Override
    public void onFileDelete(File file) {
      logger.debug("onFileDelete: {}", file);
      // TODO Should the event happen on the parent directory? Signalling that this file was
      // created as new entry in the parent?
      Path path = file.toPath();
      PollingWatchEvent event = new PollingWatchEvent(path, StandardWatchEventKinds.ENTRY_DELETE);
      events.add(event);
    }

    @Override
    public void onStop(FileAlterationObserver observer) {
      logger.debug("Done checking for changes: {}", observer);


      // The first time it checks for differences is when the service is ready
      if (!isReady) {
        isReady = true;
        readyFunction.accept(true);
      }

      for (PollingWatchEvent event : events) {
        // TODO send as set of events, so that same events for a key can be added to the same key
        eventFunction.accept(event);
      }

      events.clear();

    }


  }


  /*************************************************************************
   *
   *
   *
   * @author Thomas Naeff (github.com/thnaeff)
   *
   */
  protected class PollingWatchKey implements WatchKey {

    private final Path path;
    private LinkedBlockingQueue<WatchEvent<Path>> pollEvents = null;

    private final Consumer<PollingWatchKey> resetFunction;

    /**
     *
     *
     * @param path
     */
    public PollingWatchKey(Path path, Consumer<PollingWatchKey> resetFunction) {
      this.path = path;
      this.resetFunction = resetFunction;

      pollEvents = new LinkedBlockingQueue<>();

    }

    public Path getRegisteredPath() {
      return path;
    }

    /**
     *
     *
     * @param watchEvent
     */
    public synchronized void addWatchEvent(PollingWatchEvent watchEvent) {
      pollEvents.add(watchEvent);
    }

    @Override
    public synchronized void cancel() {
      pollEvents.clear();
    }

    @Override
    public boolean isValid() {
      // TODO
      return true;
    }

    @Override
    public synchronized List<WatchEvent<?>> pollEvents() {
      synchronized (pollEvents) {
        // Create a copy of the poll events because they might get changed
        // while the events are still being processed. Also, only the current
        // events have to be returned.
        LinkedList<WatchEvent<?>> e = new LinkedList<>(pollEvents);
        pollEvents.clear();
        return e;
      }

    }

    @Override
    public synchronized boolean reset() {
      if (!isValid()) {
        return false;
      }

      resetFunction.accept(this);

      return true;
    }

    @Override
    public Watchable watchable() {
      return path;
    }



  }


  /*************************************************************************
   *
   *
   *
   * @author Thomas Naeff (github.com/thnaeff)
   *
   */
  protected class PollingWatchEvent implements WatchEvent<Path> {

    private Path path = null;
    private Kind<Path> kind = null;


    /**
     *
     *
     * @param path
     * @param kind
     */
    public PollingWatchEvent(Path path, Kind<Path> kind) {
      this.path = path;
      this.kind = kind;

    }


    @Override
    public Path context() {
      // TODO see javadoc on 'context()' method -> should be relative path to registered path
      return path;
    }

    @Override
    public int count() {
      // TODO
      return 1;
    }

    @Override
    public Kind<Path> kind() {
      return kind;
    }

  }


}
