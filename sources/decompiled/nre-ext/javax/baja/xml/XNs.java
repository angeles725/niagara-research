package javax.baja.xml;

public final class XNs {
   String prefix;
   String uri;
   XElem declaringElem;

   public XNs(String prefix, String uri) {
      if (prefix != null && uri != null) {
         this.prefix = prefix;
         this.uri = uri;
      } else {
         throw new NullPointerException();
      }
   }

   public boolean isDefault() {
      return this.prefix.equals("");
   }

   public final String prefix() {
      return this.prefix;
   }

   public final String uri() {
      return this.uri;
   }

   @Override
   public String toString() {
      return this.uri;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else {
         return obj instanceof XNs ? this.uri.equals(((XNs)obj).uri) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.uri.hashCode();
   }

   static boolean equals(Object ns1, Object ns2) {
      return ns1 == null ? ns2 == null : ns1.equals(ns2);
   }
}
