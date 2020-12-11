package ch.thn.file.filesystemwatcher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for specific {@link FileSystemObserver} functionality.
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class FileSystemObserverTest {

  @Before
  public void setup() throws Exception {
    TestFileUtil.cleanup();
  }


  @Test
  public void testSystemWatcher() throws Exception {

    FileSystemObserver observer = new FileSystemObserver();
    observer.create();
    observer.start();

    List<PathWatchEvent> events = new ArrayList<>();

    observer.observe().subscribe(event -> {
      events.add(event);
    });

    Path testDir = TestFileUtil.newTestDirectory();
    observer.registerPath(testDir);

    // Give thread time to start...
    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> observer.isRunning());

    Path newFile = TestFileUtil.newFile(testDir, "testFile1.txt", "file in test dir");
    Path newDir = TestFileUtil.newDirectory(testDir, "watchedChildDir");

    // Wait for events to arrive
    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> events.size() >= 2);

    Set<Path> registeredPaths =
        events.stream().map(event -> event.getRegisteredPath()).collect(Collectors.toSet());
    Set<Path> eventPaths =
        events.stream().map(event -> event.getPath()).collect(Collectors.toSet());
    List<Kind<?>> eventKinds =
        events.stream().map(event -> event.getKind()).collect(Collectors.toList());

    System.out.println("Event paths: "
        + eventPaths);

    // All the same registered paths
    assertThat(registeredPaths.size(), is(1));
    assertThat(registeredPaths, containsInAnyOrder(testDir));

    // The actual changed resources
    assertThat(eventPaths, containsInAnyOrder(newFile, newDir));
    // The file creation and writing happens with two events
    assertThat(eventKinds, containsInAnyOrder(StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE));

  }

}
