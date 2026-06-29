package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BInterface;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIBacnetDataType extends BInterface, BacnetConst {
   Type TYPE = Sys.loadType(BIBacnetDataType.class);

   void writeAsn(AsnOutput var1);

   void readAsn(AsnInput var1) throws AsnException;
}
