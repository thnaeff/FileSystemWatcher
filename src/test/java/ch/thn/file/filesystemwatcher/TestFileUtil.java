package ch.thn.file.filesystemwatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class TestFileUtil {

  public static final Path TEST_DIR = Path.of("target", "test_dir");

  private TestFileUtil() {
    // Private. Only static methods.
  }

  public static Path newFile(String path, String content) throws IOException {
    File newFile = new File(TEST_DIR.toFile(), path);
    return newFile(newFile.toPath(), content);
  }

  public static Path newFile(Path inDir, String path, String content) throws IOException {
    File newFile = new File(inDir.toFile(), path);
    return newFile(newFile.toPath(), content);
  }

  public static Path newFile(Path path, String content) throws IOException {
    return fileOperation(path, content, StandardOpenOption.CREATE_NEW);
  }

  public static void appendToFile(Path path, String content) throws IOException {
    fileOperation(path, content, StandardOpenOption.APPEND);
  }

  private static Path fileOperation(Path path, String content, StandardOpenOption operation)
      throws IOException {
    if (!path.startsWith(TEST_DIR)) {
      File newFile = new File(TEST_DIR.toFile(), path.toFile().getPath());
      path = newFile.toPath();
    }
    File newFile = path.toFile();
    mkdir(newFile.getParentFile());
    Path pathObj = newFile.toPath();
    Files.writeString(pathObj, content, operation);
    return pathObj;
  }

  public static Path newTestDirectory() {
    mkdir(TEST_DIR.toFile());
    return TEST_DIR;
  }

  public static Path newDirectory(String path) throws IOException {
    File newDir = new File(TEST_DIR.toFile(), path);
    mkdir(newDir);
    return newDir.toPath();
  }

  public static Path newDirectory(Path inDir, String path) throws IOException {
    File newDir = new File(inDir.toFile(), path);
    mkdir(newDir);
    return newDir.toPath();
  }

  private static void mkdir(File newDir) {
    if (!newDir.exists()) {
      if (!newDir.mkdirs()) {
        throw new RuntimeException("Failed to set up directory "
            + newDir);
      }
    }
  }

  public static File[] getContent(String path) {
    File dir = new File(TEST_DIR.toFile(), path);
    if (!dir.exists()) {
      return new File[0];
    }
    return dir.listFiles();
  }

  public static File[] getContent(Path path) {
    if (!path.startsWith(TEST_DIR)) {
      File newFile = new File(TEST_DIR.toFile(), path.toFile().getPath());
      path = newFile.toPath();
    }
    File dir = path.toFile();
    if (!dir.exists()) {
      return new File[0];
    }
    return dir.listFiles();
  }


  public static File[] getContent() {
    File dir = TEST_DIR.toFile();
    if (!dir.exists()) {
      return new File[0];
    }
    return dir.listFiles();
  }

  public static void cleanup() throws IOException {
    List<Path> pathsInReversedDepthOrder = new ArrayList<>();

    if (TEST_DIR.toFile().exists()) {
      pathsInReversedDepthOrder.add(TEST_DIR);
      Files.walk(TEST_DIR).forEach(path -> {
        if (!pathsInReversedDepthOrder.contains(path)) {
          pathsInReversedDepthOrder.add(path);
        }
      });
    }

    Collections.sort(pathsInReversedDepthOrder, (item1, item2) -> {
      Integer length2 = Integer.valueOf(item2.toString().length());
      return length2.compareTo(item1.toString().length());
    });

    for (Path path : pathsInReversedDepthOrder) {
      if (path.toFile().exists() && !path.toFile().delete()) {
        throw new IOException("Failed to clean up: "
            + path);
      }
    }
  }

  public static void deleteDir(Path dir) throws IOException {
    if (dir.toFile().exists()) {
      Files.walk(dir).forEach(path -> path.toFile().delete());
      dir.toFile().delete();
    }
  }

}
