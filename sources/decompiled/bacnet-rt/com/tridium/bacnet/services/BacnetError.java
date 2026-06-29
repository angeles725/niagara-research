package com.tridium.bacnet.services;

import javax.baja.bacnet.io.ErrorType;

public abstract class BacnetError extends BacnetServicePrimitive {
   protected BacnetError(int errorChoice) {
      super(5, errorChoice);
   }

   public abstract ErrorType getError();

   public abstract Object[] getErrorParameters();

   public abstract String toUserString();
}
