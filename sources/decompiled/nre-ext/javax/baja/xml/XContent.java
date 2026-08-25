package javax.baja.xml;

public abstract class XContent {
   XElem parent;

   public final XElem parent() {
      return this.parent;
   }

   @Override
   public final boolean equals(Object obj) {
      return this == obj;
   }

   public abstract void write(XWriter var1);
}
