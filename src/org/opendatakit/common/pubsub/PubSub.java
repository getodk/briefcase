package org.opendatakit.common.pubsub;

import java.util.function.Consumer;

public interface PubSub {
  void publish(Event event);

  <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> eventConsumer);
}