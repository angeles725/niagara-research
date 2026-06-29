package javax.baja.bacnet.point;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import java.util.logging.Level;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.util.PollListEntry;
import javax.baja.control.BBooleanPoint;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BFloat;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetBooleanProxyExt extends BBacnetProxyExt {
   public static final Type TYPE = Sys.loadType(BBacnetBooleanProxyExt.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBooleanPoint;
   }

   @Override
   public void fromEncodedValue(byte[] encodedValue, BStatus bacnetStatus, Context cx) {
      BStatusBoolean dv = (BStatusBoolean)this.getReadValue().newCopy();
      Context baseCx = cx.getBase();

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
                        dv.setValue(asnIn.readBoolean());
                        break;
                     case 2:
                        dv.setStatusNull(false);
                        dv.setValue(asnIn.readUnsignedInteger() != 0L);
                        break;
                     case 3:
                        dv.setStatusNull(false);
                        dv.setValue(asnIn.readSignedInteger() != 0);
                        break;
                     case 4:
                        dv.setStatusNull(false);
                        dv.setValue(!BFloat.equals(asnIn.readReal(), 0.0F));
                        break;
                     case 5:
                        dv.setStatusNull(false);
                        dv.setValue(!BDouble.equals(asnIn.readDouble(), 0.0));
                        break;
                     case 6:
                        dv.setStatusNull(false);
                        byte[] b = asnIn.readOctetString();
                        dv.setValue(b.length > 0 && b[0] != 0);
                        break;
                     case 7:
                        String cs = asnIn.readCharacterString();
                        dv.setStatusNull(false);
                        dv.setValue(cs.equals(this.getParentPoint().getFacets().getFacet("trueText").toString()));
                        break;
                     case 8:
                        asnIn.readBitString();
                        dv.setStatusNull(false);
                        break;
                     case 9:
                        dv.setStatusNull(false);
                        dv.setValue(asnIn.readEnumerated() != 0);
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
                        dv.setValue(asnIn.readBoolean());
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
      } catch (AsnException var14) {
         this.readFail(var14.toString());
         this.setLastReadError(ERROR_DEVICE_OTHER);
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Exception decoding value for " + this + ":" + var14, (Throwable)var14);
         }
      }
   }

   @Override
   public byte[] toEncodedValue(BStatusValue newValue) {
      if (newValue == null) {
         return AsnUtil.toAsnNull();
      } else {
         boolean b = ((BStatusBoolean)newValue).getValue();
         switch (this.asnType) {
            case 0:
               return AsnUtil.toAsnNull();
            case 1:
               return AsnUtil.toAsnBoolean(b);
            case 2:
               return AsnUtil.toAsnUnsigned(b ? 1L : 0L);
            case 3:
               return AsnUtil.toAsnInteger(b ? 1 : 0);
            case 4:
               return AsnUtil.toAsnReal(b ? 1.0 : 0.0);
            case 5:
               return AsnUtil.toAsnDouble(b ? 1.0 : 0.0);
            case 6:
               return AsnUtil.toAsnOctetString(new byte[]{(byte)(b ? 1 : 0)});
            case 7:
               return AsnUtil.toAsnCharacterString(String.valueOf(b));
            case 8:
               return NO_VALUE;
            case 9:
               return AsnUtil.toAsnEnumerated(b ? 1 : 0);
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
               return this.isPriorityArrayPoint() ? AsnUtil.toAsnEnumerated(b ? 1 : 0) : AsnUtil.toAsnBoolean(b);
         }
      }
   }
}
