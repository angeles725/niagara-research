package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NBacnetPropertyValue;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.schedule.ScheduleSupport16;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.server.object.BObjectHandler;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.schedule.BDateRangeSchedule;
import javax.baja.schedule.BNumericSchedule;
import javax.baja.schedule.BWeeklySchedule;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BLink;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "outOfService",
      type = "boolean",
      defaultValue = "false",
      flags = 4
   ), @NiagaraProperty(
      name = "effective",
      type = "BDateRangeSchedule",
      defaultValue = "new BDateRangeSchedule()",
      flags = 260
   )})
public class BBacnetDynamicScheduleDescriptor extends BBacnetScheduleDescriptor {
   public static final Property outOfService = newProperty(4, false, null);
   public static final Property effective = newProperty(260, new BDateRangeSchedule(), null);
   public static final Type TYPE = Sys.loadType(BBacnetDynamicScheduleDescriptor.class);
   static Logger logger = Logger.getLogger("bacnet.server.schedule");
   ScheduleSupport16 supp = new ScheduleSupport16();

   public boolean getOutOfService() {
      return this.getBoolean(outOfService);
   }

   public void setOutOfService(boolean v) {
      this.setBoolean(outOfService, v, null);
   }

   public BDateRangeSchedule getEffective() {
      return (BDateRangeSchedule)this.get(effective);
   }

   public void setEffective(BDateRangeSchedule v) {
      this.set(effective, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   BStatusValue getEffectiveValueFrom(BStatusValue statusValue) {
      return new BStatusNumeric(0.0, BStatus.nullStatus);
   }

   @Override
   protected void validate() {
      this.markConfigurationError("Unknown schedule type");
   }

   @Override
   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      switch (pId) {
         case 32:
            BWeeklySchedule dynamicSchedule = this.getSchedule();
            if (dynamicSchedule == null) {
               logger.warning("Cannot write effective period to dynamic schedule: null");
               return new NErrorType(2, 56);
            }

            asnIn.setBuffer(val);
            dynamicSchedule.set(BWeeklySchedule.effective, this.supp.decodeDateRange(asnIn), BLocalBacnetDevice.getBacnetContext());
            return null;
         case 38:
         case 54:
         case 85:
         case 123:
         case 174:
            BObjectHandler boj = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getServer().getObjectHandler();
            boj.setDeleteEnabled(true);
            boj.setCreateEnabled(true);

            try {
               Array<PropertyValue> initialValues = new Array(PropertyValue.class);
               initialValues.add(new NBacnetPropertyValue(pId, ndx, val, pri));
               initialValues.add(new NBacnetPropertyValue(77, AsnUtil.toAsnCharacterString(this.getObjectName())));
               initialValues.add(new NBacnetPropertyValue(28, AsnUtil.toAsnCharacterString(this.getDescription())));
               initialValues.add(new NBacnetPropertyValue(88, AsnUtil.toAsnUnsigned(this.getPriorityForWriting())));
               initialValues.add(new NBacnetPropertyValue(81, AsnUtil.toAsnBoolean(this.getOutOfService())));
               if (this.getSchedule() != null) {
                  asnOut.reset();
                  this.supp.encodeDateRange(this.getSchedule().getEffective(), asnOut);
                  initialValues.add(new NBacnetPropertyValue(32, asnOut.toByteArray()));
               }

               BBacnetScheduleDescriptor descriptor = this.replaceCurrentBacnetSchedule(initialValues);
               if (descriptor == null) {
                  logger.warning("Exception while replacing dynamic Schedule object while writing: null");
                  return new NErrorType(2, 56);
               }

               return null;
            } catch (Exception var9) {
               logger.warning("Exception while replacing dynamic Schedule object while writing: " + var9);
               return new NErrorType(2, 56);
            }
         case 81:
            Property pIn = this.getSchedule().loadSlots().getProperty("in");
            BLink[] inLinks = this.getSchedule().getLinks(pIn);
            if (inLinks.length > 0) {
               return new NErrorType(2, 40);
            }

            this.setOutOfService(AsnUtil.fromOnlyAsnBoolean(val));
            this.setFlags(outOfService, this.getFlags(outOfService) | 1);
            return null;
         default:
            return super.writeProperty(pId, ndx, val, pri);
      }
   }

   @Override
   public void doWritePresentValue() {
   }

   @Override
   protected ErrorType doWriteScheduleDefaultValue(AsnInputStream asnInputStream, int applicationTag) throws Exception {
      return new NErrorType(1, 31);
   }

   @Override
   public PropertyValue readProperty(int pId, int ndx) {
      switch (pId) {
         case 81:
            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBoolean(this.getOutOfService()));
         default:
            return super.readProperty(pId, ndx);
      }
   }

   @Override
   int getAsnType() {
      return 4;
   }

   @Override
   Property getScheduleOutputProperty() {
      return BNumericSchedule.out;
   }
}
