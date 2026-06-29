package javax.baja.bacnet.device;

public interface LatencyRecorderAware {
   boolean addLatencyRecorder(LatencyRecorder var1);

   boolean removeLatencyRecorder(LatencyRecorder var1);

   boolean isRecordingLatency();
}
