package ch.thn.file.filesystemwatcher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for specific {@link FileSystemWatcher} functionality.
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class FileSystemWatcherTest {

  @Before
  public void setup() throws Exception {
    TestFileUtil.cleanup();
  }

  @Test
  public void testSystemWatcher() throws Exception {

    FileSystemWatcher watcher = new FileSystemWatcher();
    watcher.create();
    watcher.start();

    TestListener testListener = new TestListener();
    watcher.addPathWatcherListener(testListener);

    Path testDir = TestFileUtil.newTestDirectory();
    watcher.registerPath(testDir);

    // Give thread time to start...
    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> watcher.isRunning());


    Path newFile = TestFileUtil.newFile(testDir, "testFile1.txt", "file in test dir");
    Path newDir = TestFileUtil.newDirectory(testDir, "watchedChildDir");

    // Wait for events to arrive
    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(testListener.createDetected::size,
        is(greaterThan(0)));

    Set<Path> registeredPaths = new HashSet<>(testListener.watchedPaths);

    // All the same registered paths
    assertThat(registeredPaths.size(), is(1));
    assertThat(registeredPaths, containsInAnyOrder(testDir));

    // The actual changed resources
    assertThat(testListener.createDetected, containsInAnyOrder(newFile, newDir));
    assertThat(testListener.changeDetected, containsInAnyOrder(testDir, testDir, testDir));
    assertThat(testListener.deleteDetected.size(), is(0));
    assertThat(testListener.modifyDetected, containsInAnyOrder(newFile));

  }

}
