package org.opendatakit.common.pubsub;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * This class is a simple, straightforward implementation of {@link PubSub} that runs in the main thread
 * <p>
 * Internally, subscribers are stored in a {@link HashMap}
 */
public class SimplePubSub implements PubSub {
  private final Map<Class, Consumer> subscriptions = new HashMap<>();

  @Override
  @SuppressWarnings("unchecked")
  public void publish(Event event) {
    Optional.ofNullable(subscriptions.get(event.getClass()))
        .ifPresent(consumer -> consumer.accept(event));

  }

  @Override
  public <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> eventConsumer) {
    subscriptions.put(eventClass, eventConsumer);
  }
}