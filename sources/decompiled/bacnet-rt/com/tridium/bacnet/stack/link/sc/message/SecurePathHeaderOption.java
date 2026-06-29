package com.tridium.bacnet.stack.link.sc.message;

import com.tridium.bacnet.stack.network.DataAttribute;
import javax.baja.bacnet.enums.BBacnetErrorCode;

public final class SecurePathHeaderOption extends HeaderOption implements DataAttribute {
   private static final SecurePathHeaderOption INSTANCE = new SecurePathHeaderOption();

   SecurePathHeaderOption(int headerMarker) {
      super(headerMarker);
   }

   private SecurePathHeaderOption() {
      super(1, true, false);
   }

   public static SecurePathHeaderOption make() {
      return INSTANCE;
   }

   @Override
   protected void checkMustUnderstandFlag(boolean mustUnderstand, boolean isDestinationOption) throws ScReadMessageException {
      if (!mustUnderstand) {
         throw new ScReadMessageException("Must Understand flag must be set on a Secure Path Header Option", BBacnetErrorCode.inconsistentParameters);
      }
   }
}
