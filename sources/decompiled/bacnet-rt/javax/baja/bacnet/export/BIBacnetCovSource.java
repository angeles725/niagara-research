package javax.baja.bacnet.export;

import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetCovSubscription;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BInterface;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIBacnetCovSource extends BInterface {
   Type TYPE = Sys.loadType(BIBacnetCovSource.class);

   BObject getObject();

   BIBacnetExportObject getExport();

   void addCovSubscription(BBacnetCovSubscription var1);

   void removeCovSubscription(BBacnetCovSubscription var1);

   BBacnetCovSubscription findCovSubscription(BBacnetAddress var1, long var2, BBacnetObjectIdentifier var4);

   BBacnetCovSubscription findCovPropertySubscription(BBacnetAddress var1, long var2, BBacnetObjectIdentifier var4, int var5, int var6);

   void startCovTimer(BBacnetCovSubscription var1, long var2);

   void checkCov();

   Property getOutProperty();

   boolean supportsSubscribeCov();

   BValue getCurrentCovValue(BBacnetCovSubscription var1);
}
