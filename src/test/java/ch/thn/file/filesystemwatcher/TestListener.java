package ch.thn.file.filesystemwatcher;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class TestListener implements PathWatcherListener {

  public Collection<Path> watchedPaths = new ArrayList<>();
  public List<Path> changeDetected = new ArrayList<>();
  public List<Path> createDetected = new ArrayList<>();
  public List<Path> deleteDetected = new ArrayList<>();
  public List<Path> modifyDetected = new ArrayList<>();


  @Override
  public void newPathWatched(Path path) {

    System.out.println("New path watched: "
        + path);
    watchedPaths.add(path);

  }

  @Override
  public void pathChanged(Path path, Path context, boolean overflow) {

    System.out.println("Changed:");
    System.out.println("  Path="
        + path);
    System.out.println("  Change="
        + context);
    changeDetected.add(path);

  }

  @Override
  public void directoryCreated(Path path, Path created) {

    System.out.println("Created:");
    System.out.println("  Path="
        + path);
    System.out.println("  New="
        + created);
    createDetected.add(created);

  }

  @Override
  public void directoryDeleted(Path path, Path deleted) {

    System.out.println("Deleted:");
    System.out.println("  Path="
        + path);
    System.out.println("  Del="
        + deleted);
    deleteDetected.add(deleted);

  }

  @Override
  public void directoryModified(Path path, Path modified) {

    System.out.println("Modified:");
    System.out.println("  Path="
        + path);
    System.out.println("  Mod="
        + modified);
    modifyDetected.add(modified);

  }

}
