package com.tridium.opcUaClient.history;

import com.tridium.history.BNumeric64BitTrendRecord;
import com.tridium.opcUaClient.BOpcUaNetwork;
import java.io.IOException;
import javax.baja.control.BNumericPoint;
import javax.baja.driver.BDeviceNetwork;
import javax.baja.history.BHistoryConfig;
import javax.baja.history.BNumericTrendRecord;
import javax.baja.history.BRolloverValue;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "changeTolerance",
      type = "double",
      defaultValue = "0d"
   ), @NiagaraProperty(
      name = "precision",
      type = "int",
      defaultValue = "32",
      facets = {@Facet("BFacets.make(BFacets.FIELD_EDITOR, BString.make(\"history:PrecisionFE\"), BFacets.UX_FIELD_EDITOR, BString.make(\"history:PrecisionEditor\"))")}
   ), @NiagaraProperty(
      name = "minRolloverValue",
      type = "BRolloverValue",
      defaultValue = " new BRolloverValue()"
   ), @NiagaraProperty(
      name = "maxRolloverValue",
      type = "BRolloverValue",
      defaultValue = " new BRolloverValue()"
   )})
public class BNumericImportHistoryExt extends BImportHistoryExt {
   public static final Property changeTolerance = newProperty(0, 0.0, null);
   public static final Property precision = newProperty(
      0, 32, BFacets.make("fieldEditor", BString.make("history:PrecisionFE"), "uxFieldEditor", BString.make("history:PrecisionEditor"))
   );
   public static final Property minRolloverValue = newProperty(0, new BRolloverValue(), null);
   public static final Property maxRolloverValue = newProperty(0, new BRolloverValue(), null);
   public static final Type TYPE = Sys.loadType(BNumericImportHistoryExt.class);
   private BNumericTrendRecord rec;

   public double getChangeTolerance() {
      return this.getDouble(changeTolerance);
   }

   public void setChangeTolerance(double v) {
      this.setDouble(changeTolerance, v, null);
   }

   public int getPrecision() {
      return this.getInt(precision);
   }

   public void setPrecision(int v) {
      this.setInt(precision, v, null);
   }

   public BRolloverValue getMinRolloverValue() {
      return (BRolloverValue)this.get(minRolloverValue);
   }

   public void setMinRolloverValue(BRolloverValue v) {
      this.set(minRolloverValue, v, null);
   }

   public BRolloverValue getMaxRolloverValue() {
      return (BRolloverValue)this.get(maxRolloverValue);
   }

   public void setMaxRolloverValue(BRolloverValue v) {
      this.set(maxRolloverValue, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BNumericPoint;
   }

   @Override
   public Type getRecordType() {
      return this.getPrecision() == 64 ? BNumeric64BitTrendRecord.TYPE : BNumericTrendRecord.TYPE;
   }

   @Override
   public void started() throws Exception {
      super.started();
      if (this.getPrecision() == 64) {
         this.rec = new BNumeric64BitTrendRecord();
      } else {
         this.rec = new BNumericTrendRecord();
      }

      this.syncProperties();
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (cx != Context.decoding) {
         if (this.isRunning()) {
            if (p.equals(minRolloverValue)) {
               BHistoryConfig config = this.getHistoryConfig();
               Property prop = config.loadSlots().getProperty(minRolloverValue.getName());
               if (prop == null) {
                  config.add(minRolloverValue.getName(), this.getMinRolloverValue().newCopy(), 1);
               } else {
                  config.set(prop, this.getMinRolloverValue().newCopy());
               }
            } else if (p.equals(maxRolloverValue)) {
               BHistoryConfig config = this.getHistoryConfig();
               Property prop = config.loadSlots().getProperty(maxRolloverValue.getName());
               if (prop == null) {
                  config.add(maxRolloverValue.getName(), this.getMaxRolloverValue().newCopy(), 1);
               } else {
                  config.set(prop, this.getMaxRolloverValue().newCopy());
               }
            } else if (p.equals(precision)) {
               BHistoryConfig config = this.getHistoryConfig();
               Property prop = config.loadSlots().getProperty(precision.getName());
               if (prop == null) {
                  config.add(
                     precision.getName(),
                     BInteger.make(this.getPrecision()),
                     1,
                     BFacets.make("fieldEditor", BString.make("history:PrecisionFE"), "uxFieldEditor", BString.make("history:PrecisionEditor")),
                     null
                  );
               } else {
                  config.set(prop, BInteger.make(this.getPrecision()));
               }

               if (this.getPrecision() == 64) {
                  this.rec = new BNumeric64BitTrendRecord();
               } else {
                  this.rec = new BNumericTrendRecord();
               }

               if (!config.getRecordType().equals(this.getRecordType().getTypeSpec())) {
                  config.setRecordType(this.getRecordType().getTypeSpec());
               }
            }
         }
      }
   }

   protected void writeRecord(BAbsTime timestamp, BStatusValue out) throws IOException {
      this.append(this.rec.set(timestamp, ((BStatusNumeric)out).getValue(), out.getStatus()));
   }

   private void syncProperties() {
      BHistoryConfig config = this.getHistoryConfig();
      Property prop = config.loadSlots().getProperty(minRolloverValue.getName());
      if (prop == null) {
         config.add(minRolloverValue.getName(), this.getMinRolloverValue().newCopy(), 1);
      } else {
         BRolloverValue configMin = (BRolloverValue)config.get(prop);
         if (!configMin.equivalent(this.getMinRolloverValue())) {
            config.set(prop, this.getMinRolloverValue().newCopy());
         }
      }

      prop = config.loadSlots().getProperty(maxRolloverValue.getName());
      if (prop == null) {
         config.add(maxRolloverValue.getName(), this.getMaxRolloverValue().newCopy(), 1);
      } else {
         BRolloverValue configMax = (BRolloverValue)config.get(prop);
         if (!configMax.equivalent(this.getMaxRolloverValue())) {
            config.set(prop, this.getMaxRolloverValue().newCopy());
         }
      }

      prop = config.loadSlots().getProperty(precision.getName());
      if (prop == null) {
         config.add(
            precision.getName(),
            BInteger.make(this.getPrecision()),
            1,
            BFacets.make("fieldEditor", BString.make("history:PrecisionFE"), "uxFieldEditor", BString.make("history:PrecisionEditor")),
            null
         );
      } else {
         BInteger configPrecision = (BInteger)config.get(prop);
         if (configPrecision.getInt() != this.getPrecision()) {
            config.set(prop, BInteger.make(this.getPrecision()));
         }
      }
   }

   @Override
   public IFuture post(Action action, BValue argument, Context cx) {
      BDeviceNetwork network = this.getNetwork();
      return ((BOpcUaNetwork)network).postAsync(new Invocation(this, action, argument, cx));
   }
}
