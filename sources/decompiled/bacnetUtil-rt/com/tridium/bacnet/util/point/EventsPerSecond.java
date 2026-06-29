package com.tridium.bacnet.util.point;

public interface EventsPerSecond {
   default double calculateEventsPerSecond(long events, long lastUpdateMillis, long nowMillis) {
      double deltaSeconds = (nowMillis - lastUpdateMillis) / 1000.0;
      return deltaSeconds > 0.0 && events > 0L ? events / deltaSeconds : 0.0;
   }
}
