package javax.baja.bacnet.point;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import java.util.logging.Level;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.util.PollListEntry;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetStringProxyExt extends BBacnetProxyExt {
   public static final Type TYPE = Sys.loadType(BBacnetStringProxyExt.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void fromEncodedValue(byte[] encodedValue, BStatus bacnetStatus, Context cx) {
      BStatusString dv = (BStatusString)this.getReadValue().newCopy();
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
                        dv.setValue(asnIn.readBoolean() ? "true" : "false");
                        break;
                     case 2:
                        dv.setStatusNull(false);
                        dv.setValue(String.valueOf(asnIn.readUnsignedInteger()));
                        break;
                     case 3:
                        dv.setStatusNull(false);
                        dv.setValue(String.valueOf(asnIn.readSignedInteger()));
                        break;
                     case 4:
                        dv.setStatusNull(false);
                        dv.setValue(String.valueOf(asnIn.readReal()));
                        break;
                     case 5:
                        dv.setStatusNull(false);
                        dv.setValue(String.valueOf(asnIn.readDouble()));
                        break;
                     case 6:
                        dv.setStatusNull(false);
                        dv.setValue(asnIn.readBacnetOctetString().toString());
                        break;
                     case 7:
                        dv.setStatusNull(false);
                        dv.setValue(asnIn.readCharacterString());
                        break;
                     case 8:
                        dv.setStatusNull(false);
                        dv.setValue(asnIn.readBitString().toString());
                        break;
                     case 9:
                        dv.setStatusNull(false);
                        dv.setValue(String.valueOf(asnIn.readEnumerated()));
                        break;
                     case 10:
                        dv.setStatusNull(false);
                        dv.setValue(asnIn.readDate().toString());
                        break;
                     case 11:
                        dv.setStatusNull(false);
                        dv.setValue(asnIn.readTime().toString());
                        break;
                     case 12:
                        dv.setStatusNull(false);
                        dv.setValue(asnIn.readObjectIdentifier().toString());
                        break;
                     case 13:
                     case 14:
                     case 15:
                        dv.setStatusNull(false);
                        break;
                     default:
                        dv.setStatusNull(false);
                        PropertyInfo info = this.device().getPropertyInfo(this.getObjectId().getObjectType(), this.getPropertyId().getOrdinal());
                        BValue v = AsnUtil.asnToValue(info, encodedValue);
                        if (log.isLoggable(Level.FINE)) {
                           log.fine(
                              "StringPxExt("
                                 + this
                                 + ").fromEncodedValue: ev="
                                 + ByteArrayUtil.toHexString(encodedValue)
                                 + "\n v="
                                 + v
                                 + " ["
                                 + v.getType()
                                 + "]"
                           );
                        }

                        if (info.isArray()) {
                           int index = this.getPropertyArrayIndex();
                           if (index > 0) {
                              v = ((BBacnetArray)v).getElement(1);
                              if (log.isLoggable(Level.FINE)) {
                                 log.fine("setting dv:" + v + " [" + v.getType() + "]");
                              }

                              dv.setValue(v.toString());
                           } else if (index == 0) {
                              dv.setValue(String.valueOf(((BBacnetArray)v).getSize()));
                           } else {
                              dv.setValue(v.toString());
                           }
                        } else {
                           dv.setValue(v.toString());
                        }
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
         String s = ((BStatusString)newValue).getValue();
         switch (this.asnType) {
            case 0:
               return AsnUtil.toAsnNull();
            case 1:
               return AsnUtil.toAsnBoolean(s.equalsIgnoreCase("true"));
            case 2:
               return AsnUtil.toAsnUnsigned(Long.parseLong(s));
            case 3:
               return AsnUtil.toAsnInteger(Integer.parseInt(s));
            case 4:
               return AsnUtil.toAsnReal(Float.parseFloat(s));
            case 5:
               return AsnUtil.toAsnDouble(Double.parseDouble(s));
            case 6:
               return AsnUtil.toAsnOctetString(new byte[]{Byte.parseByte(s)});
            case 7:
               return AsnUtil.toAsnCharacterString(s);
            case 8:
               return NO_VALUE;
            case 9:
               return AsnUtil.toAsnEnumerated(Integer.parseInt(s));
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
               return AsnUtil.toAsnCharacterString(s);
         }
      }
   }
}
