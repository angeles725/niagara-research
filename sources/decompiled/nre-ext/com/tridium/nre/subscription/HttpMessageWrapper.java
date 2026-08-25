package com.tridium.nre.subscription;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpMessageWrapper<T extends IHttpMessage> {
   private CompletableFuture<IHttpMessage> messageFuture;
   private CompletableFuture<IHttpMessage> transportFuture;
   private final T message;
   private final String messageId;
   private final AtomicInteger retryCounter;
   private static final int DEFAULT_RETRY_COUNT = 3;

   public HttpMessageWrapper(T message, CompletableFuture<IHttpMessage> future) {
      this(message, future, 3);
   }

   public HttpMessageWrapper(T message, CompletableFuture<IHttpMessage> future, int numRetries) {
      this.message = message;
      this.messageFuture = future;
      this.transportFuture = new CompletableFuture<>();
      this.messageId = UUID.randomUUID().toString();
      this.retryCounter = new AtomicInteger(numRetries);
   }

   public CompletableFuture<IHttpMessage> getMessageFuture() {
      return this.messageFuture;
   }

   public void setMessageFuture(CompletableFuture<IHttpMessage> future) {
      this.messageFuture = future;
   }

   public CompletableFuture<IHttpMessage> getTransportFuture() {
      return this.transportFuture;
   }

   public T getMessage() {
      return this.message;
   }

   public String getMessageId() {
      return this.messageId;
   }

   public boolean canRetry() {
      boolean retry = this.retryCounter.decrementAndGet() >= 0;
      if (retry) {
         this.transportFuture = new CompletableFuture<>();
      }

      return retry;
   }
}
