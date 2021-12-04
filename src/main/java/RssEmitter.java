import com.apptastic.rssreader.Item;
import com.apptastic.rssreader.RssReader;
import it.sauronsoftware.cron4j.Scheduler;

import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class RssEmitter {
    private final String url;
    private final RssReader reader;
    private final PropertyChangeSupport propertyChangeSupport;
    private final Scheduler requestScheduler;
    private final String requestInterval;
    private String emitterName;
    private String recentGuid;

    /***
     * Create a new instance of an RssEmitter object.
     * @param url The url of the RSS feed.
     */
    public RssEmitter(String url) {
        this(UUID.randomUUID().toString(), url);
    }

    /***
     * Create a new instance of an RssEmitter object with a custom name.
     * @param emitterName The name of the emitter.
     * @param url The url of the RSS feed.
     */
    public RssEmitter(String emitterName, String url) {
        this.url = url;
        this.reader = new RssReader();
        this.propertyChangeSupport = new PropertyChangeSupport(RssEmitter.class);
        this.requestScheduler = new Scheduler();
        this.requestInterval = "* * * * *";
        this.emitterName = emitterName;
        this.start();
    }

    /**
     * Whenever a change is detected, this method's consumer will be called and return the new items in the feed.
     *
     * @param callback The callback to be called containing the new items in the feed.
     */
    public void onFeedChange(Consumer<Stream<Item>> callback) {
        this.propertyChangeSupport.addPropertyChangeListener(e -> {
            // Whenever a new item is detected
            if ("new-item".equals(e.getPropertyName())) { // this.recentGuid != null ?
                callback.accept(this.getNewItems());
            }
        });
    }

    /**
     * Whenever an exception is thrown, this method's consumer will be called and return the exception object.
     *
     * @param callback The callback to be called containing the exception object and information about the error.
     */
    public void onError(Consumer<Throwable> callback) {
        this.propertyChangeSupport.addPropertyChangeListener(e -> {
            if ("error".equals(e.getPropertyName())) {
                callback.accept((Throwable) e.getNewValue());
            }
        });
    }

    private void setRecentGuid() {
        Optional<Item> first = this.getFirst();
        String guid = null;
        // Checks to see if the first item is present, and also if the first item is different from the previous one.
        // First check to see if the first item in the feed is present and not malformed.
        if (first.isPresent() && first.get().getGuid().isPresent()) {
            // If this first item's GUID does NOT equal our previously stored GUID, then we have a new item.
            Item firstItem = this.getFirst().get();
            if (!firstItem.getGuid().get().equals(this.recentGuid)) {
                if (this.recentGuid == null) // if initial request
                    this.recentGuid = firstItem.getGuid().get();
                else {
                    this.propertyChangeSupport.firePropertyChange("new-item", this.recentGuid, guid);
                    this.recentGuid = first.get().getGuid().get();
                }
            } else {
                this.recentGuid = guid;
            }
        }
    }

    /***
     * Starts the scheduler which polls the feed.
     */
    private void start() {
        this.requestScheduler.schedule(this.requestInterval, this::setRecentGuid);
        this.requestScheduler.start();
        this.propertyChangeSupport.firePropertyChange("start", null, this);
    }

    /***
     * Gets the newest items in the feed since the last time this feed has detected a change.
     * @return A stream of the new items in the feed.
     */
    public Stream<Item> getNewItems() {
        return this.getNewItemsAfter(this.recentGuid);
    }

    /***
     * Gets the newest items in the feed after a specific GUID.
     * @return A stream of the new items in the feed.
     */
    public Stream<Item> getNewItemsAfter(String guid) {
        // We have to use this because we can only operate on a stream once.
        List<Item> feed = this.getFeed().toList();
        boolean containsGuid = feed.stream()
                .anyMatch(item -> item.getGuid().isPresent() && item.getGuid().get().equals(guid));

        if (containsGuid) {
            int foundIndex = -1;
            // Gets the index that the current GUID is at.
            for (int i = 0; i < feed.size(); i++) {
                if (feed.stream().skip(i).findFirst().isPresent() && feed.stream().skip(i).findFirst().get().getGuid().get().equals(guid))
                    foundIndex = i;
            }

            if (foundIndex != -1)
                return feed.stream().limit(foundIndex);

        }

        return Stream.empty();

    }

    private Stream<Item> getFeed() {
        try {
            return reader.read(this.url);
        } catch (IOException e) {
            e.printStackTrace();
            this.propertyChangeSupport.firePropertyChange("error", null, e);
            return Stream.empty();
        }
    }

    /***
     * Gets the first item in the feed.
     * @return An optional containing the first item in the feed.
     */
    private Optional<Item> getFirst() {
        return this.getFeed().findFirst();
    }

    public String getUrl() {
        return this.url;
    }

    public String getEmitterName() {
        return this.emitterName;
    }

    public void setEmitterName(String emitterName) {
        this.emitterName = emitterName;
    }

}