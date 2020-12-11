package ch.thn.file.filesystemwatcher;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;


/**
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class WatcherSetupTest {

  @Before
  public void setup() throws Exception {
    TestFileUtil.cleanup();
  }


  @Test
  public void testSystemWatcherStartStop() throws Exception {

    FileSystemWatcher observer = new FileSystemWatcher();
    observer.create();

    observer.start();

    // Give thread time to start...
    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> observer.isRunning());

    observer.stop();

    // Give thread time to stop...
    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> !observer.isRunning());

  }

  @Test
  public void testSystemWatcherStartStopAndWait() throws Exception {

    FileSystemWatcher observer = new FileSystemWatcher();
    observer.create();

    observer.start();

    // Give thread time to start...
    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> observer.isRunning());

    // Stop and wait for finish
    observer.stop(1, TimeUnit.SECONDS);

    assertFalse(observer.isRunning());

  }

  @Test
  public void testPollingWatcherStartStop() throws Exception {

    FileSystemWatcher observer = new FileSystemWatcher();
    observer.create(2, TimeUnit.SECONDS);

    observer.start();

    // Give thread time to start...
    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> observer.isRunning());

    observer.stop();

    // Give thread time to stop...
    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> !observer.isRunning());

  }

  @Test
  public void testPollingWatcherStartStopAndWait() throws Exception {

    FileSystemWatcher observer = new FileSystemWatcher();
    // High polling interval. Should stop quickly and not just when polling wakes up.
    observer.create(5, TimeUnit.SECONDS);

    observer.start();

    // Give thread time to start...
    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> observer.isRunning());

    // Stop and wait for finish. Faster than polling interval.
    observer.stop(1, TimeUnit.SECONDS);

    assertFalse(observer.isRunning());

  }


  @Test
  public void testAddChildrenAndParents() throws Exception {

    FileSystemWatcher watcher = new FileSystemWatcher();
    // Just create. Don't start for this test.
    watcher.create();

    Path testDir = TestFileUtil.newTestDirectory();
    TestFileUtil.newFile(testDir, "testFile1.txt", "first child of test dir");
    TestFileUtil.newFile(testDir, "testFile2.txt", "second child of test dir");
    Path watchedChildDir = TestFileUtil.newDirectory(testDir, "watchedChildDir");

    watcher.registerPath(testDir, true, true);

    System.out.println("Watched: "
        + watcher.getWatchedPaths());

    File testFile = testDir.toFile();
    Path[] childPaths =
        Stream.of(testFile.listFiles()).map(file -> file.toPath()).toArray(Path[]::new);
    Path parentPath = testDir.getParent();

    System.out.println("Child: "
        + Arrays.asList(childPaths));
    System.out.println("Parent: "
        + parentPath);
    System.out.println("Working: "
        + testDir);

    Collection<Path> paths = watcher.getWatchedPaths();

    // Check that all directories are being watched
    assertThat(paths, hasItem(watchedChildDir));
    assertThat(paths, hasItem(parentPath));
    assertThat(paths, hasItem(testDir));

  }

}
