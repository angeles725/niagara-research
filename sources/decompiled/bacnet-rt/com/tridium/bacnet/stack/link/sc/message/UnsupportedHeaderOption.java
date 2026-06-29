package com.tridium.bacnet.stack.link.sc.message;

import com.tridium.bacnet.stack.network.DataAttribute;
import java.io.IOException;
import java.util.Arrays;
import javax.baja.nre.util.ByteBuffer;

public final class UnsupportedHeaderOption extends HeaderOption implements DataAttribute {
   private byte[] headerData;

   protected UnsupportedHeaderOption(int headerMarker) {
      super(headerMarker);
   }

   public static UnsupportedHeaderOption copy(UnsupportedHeaderOption source) {
      UnsupportedHeaderOption copy = new UnsupportedHeaderOption(source.getHeaderMarker());
      if (source.headerData != null) {
         copy.headerData = Arrays.copyOf(source.headerData, source.headerData.length);
      }

      return copy;
   }

   @Override
   protected void checkDataFlag(boolean hasData, boolean isDestinationOption) {
   }

   @Override
   protected void decodeHeaderData(ByteBuffer in) throws IOException {
      if (this.hasData()) {
         this.headerData = new byte[in.readUnsignedShort()];
         in.readFully(this.headerData);
      }
   }

   public byte[] getHeaderData() {
      return this.headerData;
   }
}
