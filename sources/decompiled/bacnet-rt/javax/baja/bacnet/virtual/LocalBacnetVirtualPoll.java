package javax.baja.bacnet.virtual;

import com.tridium.bacnet.asn.NErrorType;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.datatypes.BBacnetPropertyReference;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.util.LocalBacnetPoll;
import javax.baja.bacnet.util.PollListEntry;
import javax.baja.status.BStatus;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Type;

public class LocalBacnetVirtualPoll extends LocalBacnetPoll {
   private BLocalBacnetVirtualGateway gateway = null;
   private static final Logger logger = Logger.getLogger("bacnet.virtual");

   LocalBacnetVirtualPoll(BLocalBacnetVirtualGateway gateway) {
      this.gateway = gateway;
   }

   @Override
   protected BRelTime getPollRate() {
      return this.gateway.getPollRate();
   }

   @Override
   protected String getThreadName() {
      return "Local Bacnet Virtual Poll";
   }

   @Override
   protected Type getPolledType() {
      return BLocalBacnetVirtualProperty.TYPE;
   }

   @Override
   protected boolean poll(BObject o) throws Exception {
      BLocalBacnetVirtualProperty lbvp = (BLocalBacnetVirtualProperty)o;
      BIBacnetExportObject export = ((BLocalBacnetVirtualObject)lbvp.getParent()).getExport();
      PollListEntry[] ples = lbvp.getPollListEntries();

      for (int i = 0; i < ples.length; i++) {
         PollListEntry ple = ples[i];

         try {
            BBacnetPropertyReference pr = new BBacnetPropertyReference(ple.getPropertyId(), ple.getPropertyArrayIndex());
            PropertyValue pv = export.readProperty(pr);
            if (!pv.isError()) {
               lbvp.fromEncodedValue(pv.getPropertyValue(), BStatus.ok, ple);
            } else {
               lbvp.readFail(NErrorType.toString(pv.getErrorClass(), pv.getErrorCode()));
            }
         } catch (Exception var9) {
            logger.log(Level.SEVERE, "Exception occurred in LocalBacnetVirtualPoll poll", (Throwable)var9);
            lbvp.readFail(var9.toString());
         }
      }

      return true;
   }
}
