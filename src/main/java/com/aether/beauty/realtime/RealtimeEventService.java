package com.aether.beauty.realtime;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class RealtimeEventService {
  private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

  public SseEmitter subscribe() {
    SseEmitter emitter = new SseEmitter(0L);
    emitters.add(emitter);
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError((error) -> emitters.remove(emitter));
    send(emitter, "connected", "AETHER realtime stream connected");
    return emitter;
  }

  public void publish(String eventName, Object payload) {
    for (SseEmitter emitter : emitters) {
      send(emitter, eventName, payload);
    }
  }

  private void send(SseEmitter emitter, String eventName, Object payload) {
    try {
      emitter.send(SseEmitter.event().name(eventName).data(payload));
    } catch (IOException | IllegalStateException ex) {
      emitters.remove(emitter);
    }
  }
}
