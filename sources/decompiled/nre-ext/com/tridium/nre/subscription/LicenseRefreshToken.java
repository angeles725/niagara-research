package com.tridium.nre.subscription;

public final class LicenseRefreshToken {
   private String nreId = null;
   private String productId = null;
   private int refreshIncrement = 0;

   public void setNreId(String newNreId) {
      this.nreId = newNreId;
   }

   public String getNreId() {
      return this.nreId;
   }

   public void setProductId(String productId) {
      this.productId = productId;
   }

   public String getProductId() {
      return this.productId;
   }

   public void updateRefreshIncrement() {
      this.refreshIncrement = RefreshIncrement.getInstance().getAndIncrement();
   }

   public int getRefreshIncrement() {
      return this.refreshIncrement;
   }

   public String getRestoreId() {
      return RestoreId.getInstance().get();
   }

   public int getNonce() {
      return EntitlementUtil.RANDOM.nextInt();
   }
}
