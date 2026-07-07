package com.tridium.webChart;

import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BAbsTimeRange;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "period",
      type = "BWebChartTimeRangeType",
      defaultValue = "BWebChartTimeRangeType.DEFAULT"
   ), @NiagaraProperty(
      name = "startFixed",
      type = "boolean",
      defaultValue = "true",
      facets = {@Facet(
         name = "BFacets.TRUE_TEXT",
         value = "START_TEXT"
      ), @Facet(
         name = "BFacets.FALSE_TEXT",
         value = "START_TEXT"
      )}
   ), @NiagaraProperty(
      name = "endFixed",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet(
         name = "BFacets.TRUE_TEXT",
         value = "END_TEXT"
      ), @Facet(
         name = "BFacets.FALSE_TEXT",
         value = "END_TEXT"
      )}
   )})
public class BWebChartTimeRange extends BAbsTimeRange {
   private static final Lexicon lex = Lexicon.make("webChart");
   private static final String START_TEXT = lex.getText("start");
   private static final String END_TEXT = lex.getText("end");
   public static final Property period = newProperty(0, BWebChartTimeRangeType.DEFAULT, null);
   public static final Property startFixed = newProperty(0, true, BFacets.make(BFacets.make("trueText", START_TEXT), BFacets.make("falseText", START_TEXT)));
   public static final Property endFixed = newProperty(0, false, BFacets.make(BFacets.make("trueText", END_TEXT), BFacets.make("falseText", END_TEXT)));
   public static final Type TYPE = Sys.loadType(BWebChartTimeRange.class);

   public BWebChartTimeRangeType getPeriod() {
      return (BWebChartTimeRangeType)this.get(period);
   }

   public void setPeriod(BWebChartTimeRangeType v) {
      this.set(period, v, null);
   }

   public boolean getStartFixed() {
      return this.getBoolean(startFixed);
   }

   public void setStartFixed(boolean v) {
      this.setBoolean(startFixed, v, null);
   }

   public boolean getEndFixed() {
      return this.getBoolean(endFixed);
   }

   public void setEndFixed(boolean v) {
      this.setBoolean(endFixed, v, null);
   }

   public Type getType() {
      return TYPE;
   }
}
