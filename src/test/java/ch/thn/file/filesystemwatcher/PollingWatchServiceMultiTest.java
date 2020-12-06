package ch.thn.file.filesystemwatcher;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ch.thn.file.filesystemwatcher.PollingWatchService.PollingWatchEvent;
import ch.thn.file.filesystemwatcher.PollingWatchService.PollingWatchKey;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class PollingWatchServiceMultiTest {

  private PollingWatchService service;

  private Path testDir1;
  private Path testDir1Subdir1;
  private Path testDir2;
  private Path testDir2File1;
  private Path testDir3;
  private Path testDir3File1;
  private Path testDir3File2;
  private String testDir3File1Content;


  @Before
  public void setup() throws Exception {
    TestFileUtil.cleanup();

    Awaitility.await().atMost(1000, TimeUnit.MILLISECONDS)
        .until(() -> TestFileUtil.getContent().length, is(0));

    service = new PollingWatchService(1, TimeUnit.SECONDS);

    // Prepare with some existing content
    testDir1 = TestFileUtil.newDirectory("testDir1");
    testDir1Subdir1 = TestFileUtil.newDirectory(testDir1, "testDir2Subdir1");
    assertThat(TestFileUtil.getContent(testDir1).length, is(1));
    service.register(testDir1);

    testDir2 = TestFileUtil.newDirectory("testDir2");
    testDir2File1 = TestFileUtil.newFile(testDir2, "testDir2File1.txt",
        "testDir2 file 1 - added before watch started");
    assertThat(TestFileUtil.getContent(testDir2).length, is(1));
    service.register(testDir2);

    testDir3 = TestFileUtil.newDirectory("testDir3");
    testDir3File1Content = "testDir3 file 1 - some content";
    testDir3File1 = TestFileUtil.newFile(testDir3, "testDir3File1.txt", testDir3File1Content);
    testDir3File2 = TestFileUtil.newFile(testDir3, "testDir3File2.txt",
        "testDir3 file 2 - added before watch started");
    assertThat(TestFileUtil.getContent(testDir3).length, is(2));
    service.register(testDir3);

    service.start();

  }

  @After
  public void closeUp() throws Exception {

    // Ensure all tests stop the service
    if (service != null && service.isReady()) {
      service.close();
      Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(() -> !service.isReady());
    }

  }



  @Test // (timeout = 3000)
  public void testMultiDirAndVariousEvents1() throws Exception {

    Path testDir1Subdir2 = TestFileUtil.newDirectory(testDir1, "testDir2Subdir2");
    Path testDir1File1 = TestFileUtil.newFile(testDir1, "testDir1File1.txt",
        "testDir1 file 1 - added after watch started");
    testDir1Subdir1.toFile().delete();

    Path testDir2File2 = TestFileUtil.newFile(testDir2, "testDir2File2.txt",
        "testDir2 file 1 - added after watch started");

    int totalExpectedEvents = 4;
    List<Path> registeredPaths = new ArrayList<>();
    List<Kind<?>> kinds = new ArrayList<>();
    List<Path> paths = new ArrayList<>();
    List<Integer> counts = new ArrayList<>();

    Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
      PollingWatchKey key = service.take();
      registeredPaths.add(key.getRegisteredPath());
      assertThat(key, notNullValue());
      List<WatchEvent<?>> events = key.pollEvents();
      events.stream().map((event) -> event.kind()).forEach(kinds::add);
      events.stream().map((event) -> event.count()).forEach(counts::add);
      events.stream().map((event) -> ((PollingWatchEvent) event).context()).forEach(paths::add);
      return kinds.size() == totalExpectedEvents;
    });

    assertThat(kinds.size(), is(totalExpectedEvents));
    assertThat(registeredPaths, containsInAnyOrder(testDir1, testDir1, testDir1, testDir2));
    assertThat(kinds,
        containsInAnyOrder(StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE));
    assertThat(paths,
        containsInAnyOrder(testDir1Subdir2, testDir1File1, testDir1Subdir1, testDir2File2));
    assertThat(counts, containsInAnyOrder(1, 1, 1, 1));

  }


  @Test(timeout = 3000)
  public void testMultiDirDeleteAndCreateModifySameFile() throws Exception {

    TestFileUtil.appendToFile(testDir2File1, "\nsome appended stuff");
    // Delete and create again, with same content.
    testDir3File1.toFile().delete();
    testDir3File1 = TestFileUtil.newFile(testDir3, testDir3File1.getFileName().toString(),
        testDir3File1Content);
    // Delete and create again, with different content.
    testDir3File2.toFile().delete();
    testDir3File2 = TestFileUtil.newFile(testDir3, testDir3File2.getFileName().toString(),
        "testDir3 file 2 - deleted and created again with new content");

    int totalExpectedEvents = 3;
    List<Path> registeredPaths = new ArrayList<>();
    List<Kind<?>> kinds = new ArrayList<>();
    List<Path> paths = new ArrayList<>();
    List<Integer> counts = new ArrayList<>();

    Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
      PollingWatchKey key = service.take();
      registeredPaths.add(key.getRegisteredPath());
      assertThat(key, notNullValue());
      List<WatchEvent<?>> events = key.pollEvents();
      events.stream().map((event) -> event.kind()).forEach(kinds::add);
      events.stream().map((event) -> event.count()).forEach(counts::add);
      events.stream().map((event) -> ((PollingWatchEvent) event).context()).forEach(paths::add);
      return kinds.size() == totalExpectedEvents;
    });

    assertThat(kinds.size(), is(totalExpectedEvents));
    // Just check for hasItems - hard to determine how many keys will be emitted
    assertThat(registeredPaths, hasItems(testDir2, testDir3));
    // Check for all required items
    assertThat(kinds, containsInAnyOrder(StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_MODIFY));
    assertThat(paths, containsInAnyOrder(testDir2File1, testDir3File1, testDir3File2));
    assertThat(counts, containsInAnyOrder(1, 1, 1));

  }



}
