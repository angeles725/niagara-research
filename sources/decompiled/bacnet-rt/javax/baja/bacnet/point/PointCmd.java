package javax.baja.bacnet.point;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.stack.BBacnetPoll;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.transport.TransactionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.io.ErrorException;
import javax.baja.bacnet.util.PollListEntry;
import javax.baja.status.BStatusValue;
import javax.baja.util.ICoalesceable;

public class PointCmd implements Runnable, ICoalesceable {
   private int cmd;
   private BBacnetProxyExt px;
   private Object arg;
   private int clr;
   private int lvl;
   static Logger log = Logger.getLogger("bacnet.point");
   public static final int SUBSCRIBE_COVP_POINT = -268435456;
   public static final int READ_POINT = Integer.MIN_VALUE;
   public static final int SUBSCRIBE_COV_POINT = 1073741824;
   public static final int WRITE_POINT = 536870912;
   public static final int READ_META_DATA = 268435456;
   private static final String READ_POINT_STR = "read";
   private static final String SUBSCRIBE_COV_POINT_STR = "subscribeCov";
   private static final String SUBSCRIBE_COVP_POINT_STR = "subscribeCovProperty";
   private static final String WRITE_POINT_STR = "write";
   private static final String READ_META_DATA_STR = "readMetaData";

   public PointCmd(int cmd, BBacnetProxyExt px) {
      this(cmd, px, null, 0, 0);
   }

   public PointCmd(int cmd, BBacnetProxyExt px, Object arg) {
      this(cmd, px, arg, 0, 0);
   }

   public PointCmd(int cmd, BBacnetProxyExt px, Object arg, int clr, int lvl) {
      this.cmd = cmd;
      this.px = px;
      this.arg = arg;
      this.clr = clr;
      this.lvl = lvl;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Bac PtCmd: px=");
      if (this.px != null) {
         sb.append(this.px.getParentPoint().getName()).append(" hc=").append(this.px.getHandle().hashCode());
      }

      sb.append(" cmd=").append(this.cmd()).append(" arg=").append(this.arg).append(" clr=").append(this.clr).append(" lvl=").append(this.lvl);
      return sb.toString();
   }

   private String cmd() {
      switch (this.cmd) {
         case Integer.MIN_VALUE:
            return "read";
         case -268435456:
            return "subscribeCovProperty";
         case 268435456:
            return "readMetaData";
         case 536870912:
            return "write";
         case 1073741824:
            return "subscribeCov";
         default:
            return String.valueOf(this.cmd);
      }
   }

   public Object getCoalesceKey() {
      return this;
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof PointCmd) {
         PointCmd bac = (PointCmd)o;
         if (this.px == bac.px && this.cmd == bac.cmd && this.clr == bac.clr && this.lvl == bac.lvl) {
            return true;
         }
      }

      return false;
   }

   @Override
   public int hashCode() {
      if (this.px != null) {
         Object handle = this.px.getHandle();
         if (handle != null) {
            return handle.hashCode();
         }
      }

      return 31 * this.cmd + 37 * this.clr + 41 * this.lvl;
   }

   public ICoalesceable coalesce(ICoalesceable c) {
      this.arg = ((PointCmd)c).arg;
      this.clr = ((PointCmd)c).clr;
      this.lvl = ((PointCmd)c).lvl;
      return this;
   }

   @Override
   public void run() {
      switch (this.cmd) {
         case Integer.MIN_VALUE:
            try {
               byte[] encodedValue = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm())
                  .getClient()
                  .readProperty(this.px.getDeviceAddress(), this.px.getObjectId(), this.px.getPropertyId().getOrdinal(), this.px.getPropertyArrayIndex());
               this.px.device().pingOk();
               this.px.fromEncodedValue(encodedValue, null, PollListEntry.forceCx);
            } catch (TransactionException var5) {
               this.px.device().ping();
               this.px.readFail(var5.toString());
               log.log(Level.WARNING, "TransactionException reading point value for " + this.px + " in " + this.px.device() + ": " + var5, (Throwable)var5);
            } catch (ErrorException var6) {
               this.px.readFail(var6.toString());
               this.px.setLastReadError(var6.getErrorType());
               log.log(Level.WARNING, "ErrorException reading point value for " + this.px + " in " + this.px.device() + ": " + var6, (Throwable)var6);
            } catch (BacnetException var7) {
               this.px.readFail(var7.toString());
               this.px.setLastReadError(BBacnetProxyExt.ERROR_DEVICE_OTHER);
               log.log(Level.WARNING, "BacnetException reading point value for " + this.px + " in " + this.px.device() + ": " + var7, (Throwable)var7);
            }
            break;
         case -268435456:
            this.px.device().subscribeCovProperty(this.px);
            break;
         case 268435456:
            try {
               PollListEntry[] ples = this.px.getPollListEntries();

               for (int i = 1; i < ples.length; i++) {
                  byte[] encodedValue = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm())
                     .getClient()
                     .readProperty(ples[i].getDevice().getAddress(), ples[i].getObjectId(), ples[i].getPropertyId(), ples[i].getPropertyArrayIndex());
                  BStatusValue sv = (BStatusValue)this.px.getReadValue().newCopy();
                  this.px.readMetaData(encodedValue, ples[i], sv);
                  this.px.readOk(sv);
                  this.px.setLastReadError(null);
               }

               this.px.device().pingOk();
            } catch (TransactionException var10) {
               this.px.device().ping();
               this.px.readFail(var10.toString());
               log.log(Level.WARNING, "TransactionException reading metadata for " + this.px + " in " + this.px.device() + ": " + var10, (Throwable)var10);
            } catch (ErrorException var11) {
               this.px.readFail(var11.toString());
               this.px.setLastReadError(var11.getErrorType());
               log.log(Level.WARNING, "ErrorException reading metadata for " + this.px + " in " + this.px.device() + ": " + var11, (Throwable)var11);
            } catch (BacnetException var12) {
               this.px.readFail(var12.toString());
               this.px.setLastReadError(BBacnetProxyExt.ERROR_DEVICE_OTHER);
               log.log(Level.WARNING, "BacnetException reading metadata for " + this.px + " in " + this.px.device() + ": " + var12, (Throwable)var12);
            }
            break;
         case 536870912:
            try {
               if (this.lvl >= 1 && this.lvl <= 16) {
                  ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm())
                     .getClient()
                     .writeProperty(
                        this.px.getDeviceAddress(),
                        this.px.getObjectId(),
                        this.px.getPropertyId().getOrdinal(),
                        this.px.getPropertyArrayIndex(),
                        this.px.toEncodedValue((BStatusValue)this.arg),
                        this.lvl
                     );
               } else if (this.arg != null) {
                  ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm())
                     .getClient()
                     .writeProperty(
                        this.px.getDeviceAddress(),
                        this.px.getObjectId(),
                        this.px.getPropertyId().getOrdinal(),
                        this.px.getPropertyArrayIndex(),
                        this.px.toEncodedValue((BStatusValue)this.arg)
                     );
               } else if (this.clr == 0) {
                  ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm())
                     .getClient()
                     .writeProperty(
                        this.px.getDeviceAddress(),
                        this.px.getObjectId(),
                        this.px.getPropertyId().getOrdinal(),
                        this.px.getPropertyArrayIndex(),
                        AsnUtil.toAsnNull()
                     );
               }

               if (this.clr > 0 && this.clr != this.lvl) {
                  if (this.clr == 17) {
                     ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm())
                        .getClient()
                        .writeProperty(
                           this.px.getDeviceAddress(),
                           this.px.getObjectId(),
                           this.px.getPropertyId().getOrdinal(),
                           this.px.getPropertyArrayIndex(),
                           AsnUtil.toAsnNull()
                        );
                  } else {
                     ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm())
                        .getClient()
                        .writeProperty(
                           this.px.getDeviceAddress(),
                           this.px.getObjectId(),
                           this.px.getPropertyId().getOrdinal(),
                           this.px.getPropertyArrayIndex(),
                           AsnUtil.toAsnNull(),
                           this.clr
                        );
                  }
               }

               this.px.device().pingOk();
               this.px.writeOk((BStatusValue)this.arg);
               this.px.setWriteStatus(BBacnetProxyExt.OK);
               ((BBacnetPoll)this.px.network().getPollService(this.px)).pollNow(this.px);
            } catch (TransactionException var8) {
               this.px.device().ping();
               this.px.writeFail(var8.toString());
               log.log(Level.WARNING, "TransactionException writing point value for " + this.px + " in " + this.px.device() + ": " + var8, (Throwable)var8);
            } catch (BacnetException var9) {
               this.px.writeFail(var9.toString());
               log.log(Level.SEVERE, "BacnetException writing point value for " + this.px + ": " + var9, (Throwable)var9);
            }
            break;
         case 1073741824:
            this.px.device().subscribeCov(this.px);
            break;
         default:
            throw new RuntimeException("Invalid cmd {" + this.cmd + " } in PointCmd.");
      }
   }
}
