package com.honeywell.easybinding.ui;

import com.honeywell.easybinding.easywidgets.BEasyPicture;
import com.tridium.kitpx.BBoundLabel;
import javax.baja.agent.BIAgent;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BLabel;
import javax.baja.ui.pane.BCanvasPane;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "valueImageLabel",
      type = "BEasyPicture",
      defaultValue = "new BEasyPicture()",
      flags = 8
   ), @NiagaraProperty(
      name = "alarmImageLabel",
      type = "BEasyPicture",
      defaultValue = "new BEasyPicture()",
      flags = 8
   ), @NiagaraProperty(
      name = "overrideImageLabel",
      type = "BEasyPicture",
      defaultValue = "new BEasyPicture()",
      flags = 8
   ), @NiagaraProperty(
      name = "textOverlayLabel",
      type = "kitPx:BoundLabel",
      defaultValue = "new BBoundLabel()",
      flags = 8
   ), @NiagaraProperty(
      name = "contextMenuLabel",
      type = "BLabel",
      defaultValue = "new BLabel()",
      flags = 8
   )})
public class BEasyBindingCanvasPane extends BCanvasPane implements BIAgent {
   public static final Property valueImageLabel = newProperty(8, new BEasyPicture(), null);
   public static final Property alarmImageLabel = newProperty(8, new BEasyPicture(), null);
   public static final Property overrideImageLabel = newProperty(8, new BEasyPicture(), null);
   public static final Property textOverlayLabel = newProperty(8, new BBoundLabel(), null);
   public static final Property contextMenuLabel = newProperty(8, new BLabel(), null);
   public static final Type TYPE = Sys.loadType(BEasyBindingCanvasPane.class);

   public BEasyPicture getValueImageLabel() {
      return (BEasyPicture)this.get(valueImageLabel);
   }

   public void setValueImageLabel(BEasyPicture var1) {
      this.set(valueImageLabel, var1, null);
   }

   public BEasyPicture getAlarmImageLabel() {
      return (BEasyPicture)this.get(alarmImageLabel);
   }

   public void setAlarmImageLabel(BEasyPicture var1) {
      this.set(alarmImageLabel, var1, null);
   }

   public BEasyPicture getOverrideImageLabel() {
      return (BEasyPicture)this.get(overrideImageLabel);
   }

   public void setOverrideImageLabel(BEasyPicture var1) {
      this.set(overrideImageLabel, var1, null);
   }

   public BBoundLabel getTextOverlayLabel() {
      return (BBoundLabel)this.get(textOverlayLabel);
   }

   public void setTextOverlayLabel(BBoundLabel var1) {
      this.set(textOverlayLabel, var1, null);
   }

   public BLabel getContextMenuLabel() {
      return (BLabel)this.get(contextMenuLabel);
   }

   public void setContextMenuLabel(BLabel var1) {
      this.set(contextMenuLabel, var1, null);
   }

   public Type getType() {
      return TYPE;
   }
}
