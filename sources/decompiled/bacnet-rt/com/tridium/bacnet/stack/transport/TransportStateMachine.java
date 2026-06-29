package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.stack.BBacnetStack;

public abstract class TransportStateMachine {
   protected static BBacnetStack stack = null;
   public static final String CLIENT_TSM_NAME = "BacnetClientTSM";
   public static final String SERVER_TSM_NAME = "BacnetServerTSM";
   protected static final int DEFAULT_SEGMENTATION_WINDOW_SIZE = Integer.parseInt(System.getProperty("niagara.bacnet.segmentation.window.size", "10"));

   public static void startStack(BBacnetStack s) {
      if (stack != null) {
         throw new IllegalStateException("Attempt to re-initialize BACnet Transport Layer State Machine! Check for duplicate BacnetNetwork.");
      } else {
         stack = s;
      }
   }

   public static void stopStack() {
      stack = null;
   }

   static final int modulo(int a, int b) {
      while (a < 0) {
         a += b;
      }

      return a % b;
   }

   static final boolean inWindow(int seqA, int seqB, int actualWindowSize) {
      return modulo(seqA - seqB, 256) < actualWindowSize;
   }

   static boolean duplicateInWindow(int seqA, int firstSeqNum, int lastSeqNum, int actualWindowSize) {
      int receivedCount = modulo(lastSeqNum - firstSeqNum, 256);
      return receivedCount > actualWindowSize ? false : modulo(seqA - firstSeqNum, 256) <= receivedCount;
   }

   public abstract void route(ApplicationPdu var1);

   public abstract String getName();
}
