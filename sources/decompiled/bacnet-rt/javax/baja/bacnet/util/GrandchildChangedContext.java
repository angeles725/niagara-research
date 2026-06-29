package javax.baja.bacnet.util;

import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.sys.BasicContext;

public class GrandchildChangedContext extends BasicContext {
   private int arrayIndex;
   private byte[] encodedValue;

   public GrandchildChangedContext(int arrayIndex, byte[] encodedValue) {
      this.arrayIndex = arrayIndex;
      this.encodedValue = encodedValue;
   }

   public int getArrayIndex() {
      return this.arrayIndex;
   }

   public byte[] getEncodedValue() {
      return this.encodedValue;
   }

   public String toString() {
      return "GrandchildChangedContext: ndx=" + this.arrayIndex + " ev=" + ByteArrayUtil.toHexString(this.encodedValue);
   }
}
