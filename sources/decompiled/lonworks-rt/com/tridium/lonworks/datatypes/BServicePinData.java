package com.tridium.lonworks.datatypes;

import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "programId",
      type = "BProgramId",
      defaultValue = "BProgramId.DEFAULT"
   ), @NiagaraProperty(
      name = "neuronId",
      type = "BNeuronId",
      defaultValue = "BNeuronId.DEFAULT"
   )})
public class BServicePinData extends BStruct {
   public static final Property programId = newProperty(0, BProgramId.DEFAULT, null);
   public static final Property neuronId = newProperty(0, BNeuronId.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BServicePinData.class);

   public BProgramId getProgramId() {
      return (BProgramId)this.get(programId);
   }

   public void setProgramId(BProgramId v) {
      this.set(programId, v, null);
   }

   public BNeuronId getNeuronId() {
      return (BNeuronId)this.get(neuronId);
   }

   public void setNeuronId(BNeuronId v) {
      this.set(neuronId, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BServicePinData(BNeuronId neuronId, BProgramId programId) {
      this.setNeuronId(neuronId);
      this.setProgramId(programId);
   }

   public BServicePinData() {
   }

   public String toString(Context context) {
      return this.getNeuronId() + "," + this.getProgramId() + ",";
   }
}
