package com.tridium.crypto.core.io;

import com.tridium.json.JSONObject;
import java.util.Date;

public class RecurringCertEntry {
   private final String alias;
   private Integer count;
   private Date lastUsed;

   public RecurringCertEntry(String alias) {
      this.alias = alias;
      this.count = 0;
      this.lastUsed = null;
   }

   public Integer getCount() {
      return this.count;
   }

   public Date getLastUsed() {
      return this.lastUsed;
   }

   public String getAlias() {
      return this.alias;
   }

   public void increment() {
      RecurringCertEntry var1 = this;
      var1.count = var1.count + 1;
      this.lastUsed = new Date();
   }

   public JSONObject toJson() {
      return new JSONObject().put("alias", this.alias).put("count", this.count).put("lastUsed", this.lastUsed);
   }
}
