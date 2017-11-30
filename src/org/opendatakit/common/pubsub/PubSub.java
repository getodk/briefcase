package org.opendatakit.common.pubsub;

import java.util.function.Consumer;

/**
 * This interface represents the API for a Publisher/Subscriber type of event bus.
 * <p>
 * {@see <a href="https://www.wikiwand.com/en/Publish%E2%80%93subscribe_pattern">Publish–subscribe pattern</a>}
 */
public interface PubSub {
  /**
   * Publish an instance of {@link Event}
   *
   * @param event the {@link Event} instance to be published
   */
  void publish(Event event);

  /**
   * Subscribe to any {@link Event} of subtype <em>T</em> that gets published in this {@link PubSub} instance.
   *
   * @param eventClass    the {@link Event} subtype class to subscribe to
   * @param eventConsumer the block of code that will be executed whenever an event of class <em>eventClass</em> is published
   * @param <T>           the concrete type of the {@link Event} subtype we want to subscribe to
   */
  <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> eventConsumer);
}