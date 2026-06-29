package com.tridium.bacnet.job;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.schedule.BBacnetChangeTypeParm;
import com.tridium.bacnet.schedule.BBacnetScheduleExport;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetAny;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
public class BBacnetScheduleTypeChangeJob extends BDeviceManagerJob {
   public static final Type TYPE = Sys.loadType(BBacnetScheduleTypeChangeJob.class);
   BBacnetScheduleExport expSch;
   BBacnetChangeTypeParm params;
   static Lexicon lex = Lexicon.make("bacnet");

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetScheduleTypeChangeJob() {
   }

   public BBacnetScheduleTypeChangeJob(BBacnetNetwork bacnet, BBacnetScheduleExport s, BBacnetChangeTypeParm p) {
      super(bacnet);
      this.expSch = s;
      this.params = p;
   }

   public void run(Context cx) throws Exception {
      BBacnetObjectIdentifier objectId = this.expSch.getObjectId();
      BBacnetAddress adr = ((BBacnetDevice)this.expSch.getDevice()).getAddress();

      try {
         if (this.params.getOprChanged()) {
            BBacnetListOf bl = this.params.getListOfObjectPropertyRefs();
            byte[] encodedValue = AsnUtil.toAsn(-3, bl);

            try {
               this.client().writeProperty(adr, objectId, 54, encodedValue);
               this.log().message("Changed ListOfObjectPropertyRefs ");
            } catch (Exception var10) {
               this.log().failed("Failed to change ListOfObjectPropertyRefs: ", var10);
            }
         }

         if (this.params.getSchedDefChanged()) {
            BBacnetAny sd = this.params.getScheduleDefault();
            byte[] encodedValue = AsnUtil.toAsn(-4, sd);

            try {
               this.client().writeProperty(adr, objectId, 174, encodedValue);
               this.log().message("Changed ScheduleDefault ");
            } catch (Exception var9) {
               this.log().failed("Failed to change ScheduleDefault: ", var9);
            }
         }

         if (this.params.getSuperChanged()) {
            this.expSch.setSupervisorOrd(this.params.getSupervisorOrd());
            this.log().message("Changed SupervisorOrd");
         }

         this.expSch.setDataType(this.params.getDataType());
         if (this.params.getHasWeeklySchedule() && this.params.getWeeklyChanged()) {
            BBacnetArray ba = this.params.getWeeklySchedule();
            byte[] encodedValue = AsnUtil.toAsn(-2, ba);

            try {
               this.client().writeProperty(adr, objectId, 123, encodedValue);
               this.log().message("Changed WeeklySchedule ");
            } catch (Exception var8) {
               this.log().failed("Failed to change WeeklySchedule: ", var8);
            }
         }

         if (this.params.getHasExceptionSchedule() && this.params.getExceptionChanged()) {
            BBacnetArray ba = this.params.getExceptionSchedule();
            byte[] encodedValue = AsnUtil.toAsn(-2, ba);

            try {
               this.client().writeProperty(adr, objectId, 38, encodedValue);
               this.log().message("Changed ExceptionSchedule ");
            } catch (Exception var7) {
               this.log().failed("Failed to change ExceptionSchedule: ", var7);
            }
         }
      } catch (Exception var11) {
         this.add("failureCause", BString.make(var11.toString()));
         this.log().failed(var11.toString());
         throw var11;
      }
   }
}
