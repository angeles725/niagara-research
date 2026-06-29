package javax.baja.lonworks.datatypes;

import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "extended",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "addressTable",
      type = "BAddressTable",
      defaultValue = "new BAddressTable()",
      flags = 4,
      override = true
   ), @NiagaraProperty(
      name = "extAddressTable",
      type = "BExtAddressTable",
      defaultValue = "new BExtAddressTable()",
      flags = 65,
      override = true
   )})
public class BExtDeviceData extends BDeviceData {
   public static final Property extended = newProperty(0, true, null);
   public static final Property addressTable = newProperty(4, new BAddressTable(), null);
   public static final Property extAddressTable = newProperty(65, new BExtAddressTable(), null);
   public static final Type TYPE = Sys.loadType(BExtDeviceData.class);

   public boolean getExtended() {
      return this.getBoolean(extended);
   }

   public void setExtended(boolean v) {
      this.setBoolean(extended, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public static BExtDeviceData make(BDeviceData dd) {
      BExtDeviceData edd = new BExtDeviceData();
      Property[] pa = dd.getPropertiesArray();

      for (int i = 0; i < pa.length; i++) {
         if (pa[i].getDefaultValue().isComplex()) {
            edd.get(pa[i]).asComplex().copyFrom(dd.get(pa[i]).asComplex());
         } else {
            edd.set(pa[i], dd.get(pa[i]), null);
         }
      }

      return edd;
   }

   public BAddressTable getMyAddressTable() {
      return (BAddressTable)this.get(addressTable);
   }

   @Override
   public boolean isExtended() {
      return this.getExtended();
   }

   @Override
   public void clearAddressTable() {
      this.getExtAddressTable().clearTable();
   }

   @Override
   public BIAddressEntry getAddressEntry(int index) {
      return this.getExtAddressTable().getAddressEntry(index);
   }

   @Override
   public void setAddressEntry(int index, BIAddressEntry e) {
      this.getExtAddressTable().setAddressEntry(index, e);
   }

   @Override
   public void setAddressEntry(int index, BIAddressEntry e, Context c) {
      this.getExtAddressTable().setAddressEntry(index, e, c);
   }
}
