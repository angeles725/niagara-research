package com.tridium.nre.platform;

public class NREMemoryPool {
   private int minimumSize;
   private int maximumSize;
   private int defaultSize;
   private int currentSize;

   public NREMemoryPool(int minimumSize, int maximumSize, int defaultSize) {
      this.minimumSize = minimumSize;
      this.maximumSize = maximumSize;
      this.defaultSize = defaultSize;
   }

   public int getMinimumSize() {
      return this.minimumSize;
   }

   public int getMaximumSize() {
      return this.maximumSize;
   }

   public int getDefaultSize() {
      return this.defaultSize;
   }

   @Override
   public String toString() {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("min=").append(this.minimumSize).append(";");
      stringBuilder.append("max=").append(this.maximumSize).append(";");
      stringBuilder.append("default=").append(this.defaultSize).append(";");
      stringBuilder.append("current=").append(this.currentSize);
      return stringBuilder.toString();
   }

   public int getCurrentSize() {
      return this.currentSize;
   }

   public void setCurrentSize(int currentSize) {
      this.currentSize = currentSize;
   }
}
