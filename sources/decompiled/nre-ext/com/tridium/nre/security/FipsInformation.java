package com.tridium.nre.security;

import java.util.Date;

public class FipsInformation {
   private final int fipsVersion;
   private final int fipsLevel;
   private final Date fipsRevisionDate;
   private final String providerName;
   private final double providerVersion;
   private final int niagaraVersion;

   public FipsInformation(int fipsVersion, int fipsLevel, Date fipsRevisionDate, String providerName, double providerVersion, int niagaraVersion) {
      this.fipsVersion = fipsVersion;
      this.fipsLevel = fipsLevel;
      this.fipsRevisionDate = fipsRevisionDate;
      this.providerName = providerName;
      this.providerVersion = providerVersion;
      this.niagaraVersion = niagaraVersion;
   }

   public int getFipsVersion() {
      return this.fipsVersion;
   }

   public int getFipsLevel() {
      return this.fipsLevel;
   }

   public Date getFipsRevisionDate() {
      return this.fipsRevisionDate;
   }

   public String getProviderName() {
      return this.providerName;
   }

   public double getProviderVersion() {
      return this.providerVersion;
   }

   public int getNiagaraVersion() {
      return this.niagaraVersion;
   }
}
