# FileSystemWatcher
Watch one or multiple paths for file or directory changes, locally or on a network directory

The file system watcher can be set up to use the watch service that comes with java, or use a polling 
watch service in case the java watch service is not available.


Example:

FileSystemWatcher watcher = new FileSystemWatcher();

//Add some listener
watcher.addPathWatcherListener(new TestListener());

// Needs to be properly used in a thread for the reporting
Thread t = new Thread(watcher);
t.start();

//...

//End watcher thread
watcher.stop();



--------------------------------------


class TestListener implements PathWatcherListener {

    @Override
    public void newPathWatched(Path path) {

      //...

    }

    @Override
    public void pathChanged(Path path, Path context, boolean overflow) {

      //...

    }

    @Override
    public void directoryCreated(Path path, Path created) {

      //...

    }

    @Override
    public void directoryDeleted(Path path, Path deleted) {

      //...

    }

    @Override
    public void directoryModified(Path path, Path modified) {

     	//...

    }

  }
  