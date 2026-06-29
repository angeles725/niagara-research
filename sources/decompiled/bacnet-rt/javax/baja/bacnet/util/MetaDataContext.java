package javax.baja.bacnet.util;

import javax.baja.sys.BasicContext;

public class MetaDataContext extends BasicContext {
   private String propertyName;

   public MetaDataContext(String propName) {
      this.propertyName = propName;
   }

   public boolean equals(Object o) {
      if (o == null) {
         return false;
      } else if (o instanceof MetaDataContext) {
         MetaDataContext mcx = (MetaDataContext)o;
         return this.propertyName.equals(mcx.propertyName);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return 1;
   }

   public String toString() {
      StringBuilder sb = new StringBuilder("Bacnet:MetaData:");
      sb.append(this.propertyName);
      return sb.toString();
   }

   public String getPropertyName() {
      return this.propertyName;
   }
}
