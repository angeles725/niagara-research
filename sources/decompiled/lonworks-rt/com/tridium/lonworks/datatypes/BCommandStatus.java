package com.tridium.lonworks.datatypes;

import com.tridium.lonworks.enums.BNetmgmtState;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BEnum;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "level",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "text",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "state",
      type = "BEnum",
      defaultValue = "BNetmgmtState.nil"
   )})
public class BCommandStatus extends BStruct {
   public static final Property level = newProperty(0, 0, null);
   public static final Property text = newProperty(0, "", null);
   public static final Property state = newProperty(0, BNetmgmtState.nil, null);
   public static final Type TYPE = Sys.loadType(BCommandStatus.class);
   public static final int NORMAL = 0;
   public static final int SUCCESS = 1;
   public static final int FAILURE = 2;
   public static final int WARNING = 4;

   public int getLevel() {
      return this.getInt(level);
   }

   public void setLevel(int v) {
      this.setInt(level, v, null);
   }

   public String getText() {
      return this.getString(text);
   }

   public void setText(String v) {
      this.setString(text, v, null);
   }

   public BEnum getState() {
      return (BEnum)this.get(state);
   }

   public void setState(BEnum v) {
      this.set(state, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BCommandStatus() {
   }

   public BCommandStatus(int level, String text) {
      this(level, text, null);
   }

   public BCommandStatus(int level, String text, BEnum state) {
      this.setLevel(level);
      this.setText(text);
      if (state != null) {
         this.setState(state);
      }
   }
}
