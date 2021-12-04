public class Example {
    public static void main(String[] args) {
        RssEmitter emitter = new RssEmitter("https://www.livechart.me/feeds/episodes");
        emitter.onFeedChange((e) -> {
            e.forEach(item -> System.out.println(item.getTitle().get() + " | " + item.getGuid().get()));
        });

        RssEmitter emitter2 = new RssEmitter("https://lorem-rss.herokuapp.com/feed?unit=second&interval=30");
        emitter2.onFeedChange((e) -> {
            e.forEach(item -> System.out.println(item.getTitle().get() + " | " + item.getGuid().get()));
        });
    }

}