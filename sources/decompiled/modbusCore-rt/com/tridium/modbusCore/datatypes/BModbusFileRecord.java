package com.tridium.modbusCore.datatypes;

import com.tridium.modbusCore.BModbusDevice;
import com.tridium.modbusCore.BModbusNetwork;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBlob;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "data",
      type = "BBlob",
      defaultValue = "BBlob.DEFAULT",
      facets = {@Facet("BFacets.make(BFacets.MULTI_LINE, BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "fileNumber",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(null, 0, 65535)")}
   ), @NiagaraProperty(
      name = "startingRecordNumber",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(null, 0, 9999)")}
   ), @NiagaraProperty(
      name = "recordLength",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(null, 0, 65535)")}
   )})
public abstract class BModbusFileRecord extends BComponent {
   public static final Property data = newProperty(0, BBlob.DEFAULT, BFacets.make("multiLine", BBoolean.TRUE));
   public static final Property fileNumber = newProperty(0, 0, BFacets.makeInt(null, 0, 65535));
   public static final Property startingRecordNumber = newProperty(0, 0, BFacets.makeInt(null, 0, 9999));
   public static final Property recordLength = newProperty(0, 0, BFacets.makeInt(null, 0, 65535));
   public static final Type TYPE = Sys.loadType(BModbusFileRecord.class);

   public BBlob getData() {
      return (BBlob)this.get(data);
   }

   public void setData(BBlob v) {
      this.set(data, v, null);
   }

   public int getFileNumber() {
      return this.getInt(fileNumber);
   }

   public void setFileNumber(int v) {
      this.setInt(fileNumber, v, null);
   }

   public int getStartingRecordNumber() {
      return this.getInt(startingRecordNumber);
   }

   public void setStartingRecordNumber(int v) {
      this.setInt(startingRecordNumber, v, null);
   }

   public int getRecordLength() {
      return this.getInt(recordLength);
   }

   public void setRecordLength(int v) {
      this.setInt(recordLength, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BModbusDevice getDevice() {
      for (BComplex parent = this.getParent(); parent != null; parent = parent.getParent()) {
         if (parent instanceof BModbusDevice) {
            return (BModbusDevice)parent;
         }
      }

      return null;
   }

   public BModbusNetwork getNetwork() {
      BModbusDevice device = this.getDevice();
      return device == null ? null : device.modbusNet();
   }
}
