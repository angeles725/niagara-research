package javax.baja.bacnet.device;

public interface LatencyRecorder {
   void recordLatency(long var1);

   boolean isRecordingLatency();
}
