package com.tridium.niagarad.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class RequestState {
   public static final short MAX_LENGTH = 255;
   public static final short MAX_REQUESTS = 32;
   private Map<String, String> requestCache = null;
   private final Object requestMonitor = new Object();

   public String getRequestState(String requestId) {
      synchronized (this.requestMonitor) {
         if (this.requestCache == null) {
            return "";
         }

         if (requestId == null) {
            return "";
         }

         String state = this.requestCache.get(requestId);
         return state != null ? state : "";
      }
   }

   public void removeRequest(String requestId) {
      synchronized (this.requestMonitor) {
         if (this.requestCache != null) {
            if (requestId != null) {
               this.requestCache.remove(requestId);
            }
         }
      }
   }

   public void updateRequest(String requestId, String state) {
      synchronized (this.requestMonitor) {
         if (requestId != null) {
            if (state != null) {
               if (this.requestCache == null) {
                  this.requestCache = new LinkedHashMap<String, String>(32, 0.75F) {
                     @Override
                     public boolean removeEldestEntry(Entry<String, String> eldest) {
                        return this.size() > 32;
                     }
                  };
               }

               this.requestCache.put(requestId, state);
            }
         }
      }
   }

   private RequestState() {
      this.requestCache = null;
   }

   public static RequestState getInstance() {
      return RequestState.InstanceHolder.INSTANCE;
   }

   private static final class InstanceHolder {
      private static final RequestState INSTANCE = new RequestState();
   }
}
