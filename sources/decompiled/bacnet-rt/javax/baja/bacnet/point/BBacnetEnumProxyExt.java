package javax.baja.bacnet.point;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import java.util.logging.Level;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.util.PollListEntry;
import javax.baja.control.BEnumPoint;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "signed",
   type = "boolean",
   defaultValue = "false"
)
public class BBacnetEnumProxyExt extends BBacnetProxyExt {
   public static final Property signed = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BBacnetEnumProxyExt.class);

   public boolean getSigned() {
      return this.getBoolean(signed);
   }

   public void setSigned(boolean v) {
      this.setBoolean(signed, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BEnumPoint;
   }

   @Override
   public void fromEncodedValue(byte[] encodedValue, BStatus bacnetStatus, Context cx) {
      BStatusEnum dv = (BStatusEnum)this.getReadValue().newCopy();
      Context baseCx = cx.getBase();
      BEnum ms = ((BEnumPoint)this.getParentPoint()).getEnum();
      BEnumRange msr = (BEnumRange)((BEnumPoint)this.getParentPoint()).getEnumFacets().getFacet("range");
      if (msr == null) {
         msr = ms.getRange();
      }

      try {
         if (bacnetStatus == null) {
            dv.setStatusDown(false);
         } else {
            dv.setStatus(bacnetStatus);
         }

         if (encodedValue != null) {
            if (baseCx == PollListEntry.pointCx || cx == covContext || cx == PollListEntry.pointCx || cx == PollListEntry.forceCx) {
               this.dataSize = encodedValue.length;
               AsnInputStream asnIn = AsnInputStream.make(encodedValue);

               try {
                  int tag = asnIn.peekApplicationTag();
                  if (this.getDataType().length() == 0) {
                     this.setDataType(AsnUtil.getAsnTypeName(tag));
                  }

                  switch (tag) {
                     case 0:
                        dv.setStatusNull(true);
                        break;
                     case 1:
                        dv.setStatusNull(false);
                        dv.setValue(msr.get(asnIn.readBoolean() ? 1 : 0));
                        break;
                     case 2:
                        dv.setStatusNull(false);
                        dv.setValue(msr.get(asnIn.readUnsignedInt()));
                        break;
                     case 3:
                        dv.setStatusNull(false);
                        dv.setValue(msr.get(asnIn.readSignedInteger()));
                        break;
                     case 4:
                        dv.setStatusNull(false);
                        dv.setValue(msr.get((int)asnIn.readReal()));
                        break;
                     case 5:
                        dv.setStatusNull(false);
                        dv.setValue(msr.get((int)asnIn.readDouble()));
                        break;
                     case 6:
                        dv.setStatusNull(false);
                        dv.setValue(msr.get(asnIn.readOctetString()[0]));
                        break;
                     case 7:
                        String cs = asnIn.readCharacterString();
                        dv.setStatusNull(false);
                        if (msr.isTag(cs)) {
                           dv.setValue(ms.getRange().get(msr.tagToOrdinal(cs)));
                        }
                        break;
                     case 8:
                        asnIn.readBitString();
                        dv.setStatusNull(false);
                        break;
                     case 9:
                        dv.setStatusNull(false);
                        dv.setValue(msr.get(asnIn.readEnumerated()));
                        break;
                     case 10:
                        asnIn.readDate();
                        dv.setStatusNull(false);
                        break;
                     case 11:
                        asnIn.readTime();
                        dv.setStatusNull(false);
                        break;
                     case 12:
                        asnIn.readObjectIdentifier();
                        dv.setStatusNull(false);
                        break;
                     case 13:
                     case 14:
                     case 15:
                        dv.setStatusNull(false);
                        break;
                     default:
                        dv.setStatusNull(false);
                        dv.setValue(ms.getRange().get(asnIn.readInteger()));
                  }
               } finally {
                  asnIn.release();
               }
            } else if (cx instanceof PollListEntry) {
               this.readMetaData(encodedValue, cx, dv);
            }
         }

         this.readOk(dv);
         this.setLastReadError(null);
         this.updateReadStatus(cx);
      } catch (AsnException var15) {
         this.readFail(var15.toString());
         this.setLastReadError(ERROR_DEVICE_OTHER);
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Exception decoding value for " + this + ":" + var15, (Throwable)var15);
         }
      }
   }

   @Override
   public byte[] toEncodedValue(BStatusValue newValue) {
      if (newValue == null) {
         return AsnUtil.toAsnNull();
      } else {
         int i = ((BStatusEnum)newValue).getValue().getOrdinal();
         switch (this.asnType) {
            case 0:
               return AsnUtil.toAsnNull();
            case 1:
               return AsnUtil.toAsnBoolean(i != 0);
            case 2:
               return AsnUtil.toAsnUnsigned(i);
            case 3:
               return AsnUtil.toAsnInteger(i);
            case 4:
               return AsnUtil.toAsnReal(i);
            case 5:
               return AsnUtil.toAsnDouble(i);
            case 6:
               return AsnUtil.toAsnOctetString(new byte[]{(byte)i});
            case 7:
               return AsnUtil.toAsnCharacterString(String.valueOf(i));
            case 8:
               return NO_VALUE;
            case 9:
               return AsnUtil.toAsnEnumerated(i);
            case 10:
               return NO_VALUE;
            case 11:
               return NO_VALUE;
            case 12:
               return NO_VALUE;
            case 13:
               return NO_VALUE;
            case 14:
               return NO_VALUE;
            case 15:
               return NO_VALUE;
            default:
               return this.getSigned() ? AsnUtil.toAsnInteger(i) : AsnUtil.toAsnUnsigned(i);
         }
      }
   }
}
