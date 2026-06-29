package com.tridium.bacnet.stack.poll;

import com.tridium.bacnet.stack.BBacnetPollOrder;
import java.util.Collections;
import java.util.List;
import javax.baja.bacnet.util.PollList;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetRandomPollOrder extends BBacnetPollOrder {
   public static final Type TYPE = Sys.loadType(BBacnetRandomPollOrder.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void sort(List<PollList> entries) {
      Collections.shuffle(entries);
   }
}
