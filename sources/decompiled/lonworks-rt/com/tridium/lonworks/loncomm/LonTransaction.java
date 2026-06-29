package com.tridium.lonworks.loncomm;

import javax.baja.lonworks.LonException;

public class LonTransaction {
   private NAppBuffer outgoingMessage = null;
   private NAppBuffer responseMessage = null;
   private int tag;
   private boolean local = false;
   private boolean used = false;
   private boolean complete = false;
   private LonException exception = null;
   private long endTime = 0L;

   protected LonTransaction(int tag) {
      this.tag = tag;
   }

   protected void setResponseMessage(NAppBuffer a) {
      this.responseMessage = a;
   }

   protected NAppBuffer getResponseMessage() {
      return this.responseMessage;
   }

   protected void addResponseMessage(NAppBuffer appBuffer) {
      if (this.responseMessage != null) {
         this.responseMessage.nextAppBuffer = appBuffer;
      } else {
         this.responseMessage = appBuffer;
      }
   }

   void setOutgoingMessage(NAppBuffer a) {
      this.outgoingMessage = a;
   }

   NAppBuffer getOutgoingMessage() {
      return this.outgoingMessage;
   }

   int getTag() {
      return this.tag;
   }

   void setLocal(boolean l) {
      this.local = l;
   }

   boolean isLocal() {
      return this.local;
   }

   void setUsed(boolean u) {
      this.used = u;
   }

   boolean isUsed() {
      return this.used;
   }

   void setComplete(boolean c) {
      this.complete = c;
   }

   boolean isComplete() {
      return this.complete;
   }

   void setException(LonException e) {
      this.exception = e;
   }

   LonException getException() {
      return this.exception;
   }

   void setEndTime(long s) {
      this.endTime = s;
   }

   long getEndTime() {
      return this.endTime;
   }
}
