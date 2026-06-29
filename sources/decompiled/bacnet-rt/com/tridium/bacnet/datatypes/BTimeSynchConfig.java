package com.tridium.bacnet.datatypes;

import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "timeSynchType",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("BFacets.makeBoolean(TS_TYPE_UTC, TS_TYPE_LOCAL)")}
   ), @NiagaraProperty(
      name = "addressRange",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("BFacets.makeBoolean(TS_RANGE_GLOBAL, TS_RANGE_LOCAL)")}
   )})
public class BTimeSynchConfig extends BRequestConfig {
   private static final Lexicon lex = Lexicon.make("bacnet");
   private static final String TS_TYPE_UTC = lex.getText("timeSynchType.utc");
   private static final String TS_TYPE_LOCAL = lex.getText("timeSynchType.local");
   private static final String TS_RANGE_LOCAL = lex.getText("timeSynchRange.local");
   private static final String TS_RANGE_GLOBAL = lex.getText("timeSynchRange.global");
   public static final Property timeSynchType = newProperty(0, false, BFacets.makeBoolean(TS_TYPE_UTC, TS_TYPE_LOCAL));
   public static final Property addressRange = newProperty(0, false, BFacets.makeBoolean(TS_RANGE_GLOBAL, TS_RANGE_LOCAL));
   public static final Type TYPE = Sys.loadType(BTimeSynchConfig.class);

   public boolean getTimeSynchType() {
      return this.getBoolean(timeSynchType);
   }

   public void setTimeSynchType(boolean v) {
      this.setBoolean(timeSynchType, v, null);
   }

   public boolean getAddressRange() {
      return this.getBoolean(addressRange);
   }

   public void setAddressRange(boolean v) {
      this.setBoolean(addressRange, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }
}
