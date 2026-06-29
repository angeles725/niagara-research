package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.datatypes.BChangeNvTypeParameter;
import com.tridium.lonworks.util.selfdoc.SelfDoc;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.LonException;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BAction;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "changeParam",
   type = "BChangeNvTypeParameter",
   defaultValue = "new BChangeNvTypeParameter()"
)
public class BChangeNvTypeAction extends BAction {
   public static final Property changeParam = newProperty(0, new BChangeNvTypeParameter(), null);
   public static final Type TYPE = Sys.loadType(BChangeNvTypeAction.class);
   private SelfDoc sdoc = null;
   private BLonDevice dev;

   public BChangeNvTypeParameter getChangeParam() {
      return (BChangeNvTypeParameter)this.get(changeParam);
   }

   public void setChangeParam(BChangeNvTypeParameter v) {
      this.set(changeParam, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Type getParameterType() {
      return BChangeNvTypeParameter.TYPE;
   }

   public Type getReturnType() {
      return null;
   }

   public BValue getParameterDefault() {
      return new BChangeNvTypeParameter();
   }

   public BValue invoke(BComponent target, BValue arg) {
      if (!(target instanceof BLonDevice)) {
         throw new IllegalArgumentException("ChangeNvTypeAction cannot be invoked on " + target.getType());
      } else {
         this.dev = (BLonDevice)target;
         this.dev.checkState();
         this.dev.checkChangeNvType();
         BChangeNvTypeParameter par = (BChangeNvTypeParameter)arg;
         return new BLonChangeNvTypeJob(this.dev, par, this).submit(null);
      }
   }

   SelfDoc getSelfDoc() throws LonException {
      if (this.sdoc == null) {
         this.sdoc = new SelfDoc(this.dev);
      }

      return this.sdoc;
   }
}
