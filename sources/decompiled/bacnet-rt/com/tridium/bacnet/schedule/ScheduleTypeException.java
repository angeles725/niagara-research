package com.tridium.bacnet.schedule;

import javax.baja.sys.LocalizableRuntimeException;

public class ScheduleTypeException extends LocalizableRuntimeException {
   public ScheduleTypeException(String lexiconModule, String lexiconKey, Object[] lexiconArgs, Throwable cause) {
      super(lexiconModule, lexiconKey, lexiconArgs, cause);
   }

   public ScheduleTypeException(String lexiconModule, String lexiconKey, Object[] lexiconArgs) {
      super(lexiconModule, lexiconKey, lexiconArgs, null);
   }
}
