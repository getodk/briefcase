package org.opendatakit.common.pubsub;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ThreadSafePubSub implements PubSub {
  private static final Log LOGGER = LogFactory.getLog(ThreadSafePubSub.class);
  private final Map<Class, Consumer> subscriptions = new ConcurrentHashMap<>();

  @Override
  @SuppressWarnings("unchecked")
  public void publish(Event event) {
    LOGGER.info("Publish " + event.getClass().getName());
    Optional.ofNullable(subscriptions.get(event.getClass()))
        .ifPresent(consumer -> consumer.accept(event));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> eventConsumer) {
    LOGGER.debug("Subscribe to " + eventClass);
    subscriptions.put(eventClass, event -> {
      LOGGER.debug("Consume " + eventClass);
      eventConsumer.accept((T) event);
    });
  }
}