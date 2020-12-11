package ch.thn.file.filesystemwatcher;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;


/**
 * An implementation of the watch service with the observable model, based on ReactiveX/RxJava.
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class FileSystemObserver extends AbstractWatcher {

  private List<Emitter<PathWatchEvent>> emitters = new ArrayList<>();


  /**
   * A path watcher service using the file system watcher.<br />
   * This is the preferred setup compared to using the setup with polling time.
   *
   */
  public FileSystemObserver() {
    super();
  }


  public @NonNull Observable<PathWatchEvent> observe() {
    return Observable.create(subscriber -> {
      emitters.add(subscriber);
      subscriber.setCancellable(() -> emitters.remove(subscriber));
    });
  }

  public @NonNull Flowable<PathWatchEvent> flow() {
    return Flowable.create(emitter -> {
      emitters.add(emitter);
      emitter.setCancellable(() -> emitters.remove(emitter));
    }, BackpressureStrategy.BUFFER);
  }

  @Override
  public void stop() throws IOException {
    super.stop();

    // TODO better place than 'stop' override?
    for (Emitter<PathWatchEvent> emitter : emitters) {
      emitter.onComplete();
    }
  }

  @Override
  protected void processEvent(Kind<?> eventKind, Path path, Path context, boolean overflow) {
    PathWatchEvent event = new PathWatchEvent(path, context, eventKind);
    for (Emitter<PathWatchEvent> emitter : emitters) {
      emitter.onNext(event);
    }

  }



}
