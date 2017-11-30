package org.opendatakit.common.pubsub;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * This class is a trhead-safe implementation of {@link PubSub} designed to be used in a multi-threaded
 * execution context.
 * <p>
 * Internally, subscribers are stored in a {@link ConcurrentHashMap}, which makes this implementation thread-safe
 */
public class ThreadSafePubSub implements PubSub {
  private final Map<Class, Consumer> subscriptions = new ConcurrentHashMap<>();

  @Override
  @SuppressWarnings("unchecked")
  public void publish(Event event) {
    Optional.ofNullable(subscriptions.get(event.getClass()))
        .ifPresent(consumer -> consumer.accept(event));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> eventConsumer) {
    subscriptions.put(eventClass, event -> eventConsumer.accept((T) event));
  }
}