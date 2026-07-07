package com.tridium.px.editor.make;

import com.tridium.util.ClassUtil;
import com.tridium.workbench.celleditors.BObjectCE;
import com.tridium.workbench.util.PropertyManager;
import java.lang.reflect.Method;
import javax.baja.converters.BObjectToString;
import javax.baja.naming.BOrd;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BBorder;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BLabel;
import javax.baja.ui.BLayout;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BValueBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.options.BMruTextDropDown;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.text.BTextEditor;
import javax.baja.util.BFormat;
import javax.baja.util.BTypeSpec;
import javax.baja.workbench.BWbEditor;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Object"}
   )}
)
@NiagaraProperties({@NiagaraProperty(
      name = "widgetType",
      type = "String",
      defaultValue = "kitPx:BoundLabel"
   ), @NiagaraProperty(
      name = "bindingType",
      type = "String",
      defaultValue = "kitPx:BoundLabelBinding"
   )})
@NiagaraActions({@NiagaraAction(
      name = "hyperlinkToggled"
   ), @NiagaraAction(
      name = "statusToggled"
   ), @NiagaraAction(
      name = "formatTextToggled"
   ), @NiagaraAction(
      name = "mouseOverToggled"
   ), @NiagaraAction(
      name = "borderToggled"
   ), @NiagaraAction(
      name = "formatTextFieldModified"
   ), @NiagaraAction(
      name = "mouseOverFEModified"
   ), @NiagaraAction(
      name = "statusFEModified"
   ), @NiagaraAction(
      name = "borderCEModified"
   )})
public class BMwBoundLabel extends BMwConfig {
   public static final Property widgetType = newProperty(0, "kitPx:BoundLabel", null);
   public static final Property bindingType = newProperty(0, "kitPx:BoundLabelBinding", null);
   public static final Action hyperlinkToggled = newAction(0, null);
   public static final Action statusToggled = newAction(0, null);
   public static final Action formatTextToggled = newAction(0, null);
   public static final Action mouseOverToggled = newAction(0, null);
   public static final Action borderToggled = newAction(0, null);
   public static final Action formatTextFieldModified = newAction(0, null);
   public static final Action mouseOverFEModified = newAction(0, null);
   public static final Action statusFEModified = newAction(0, null);
   public static final Action borderCEModified = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BMwBoundLabel.class);
   private BCheckBox chkHyperlink;
   private BCheckBox chkStatus;
   private BCheckBox chkFormatText;
   private BCheckBox chkMouseOver;
   private BCheckBox chkBorder;
   private BCheckBox chkUnbound;
   protected ToggleCommand hyperlink;
   protected ToggleCommand status;
   protected ToggleCommand formatText;
   protected ToggleCommand mouseOver;
   protected ToggleCommand border;
   protected ToggleCommand unbound;
   private BMruTextDropDown formatMru;
   private BTextEditor formatMruEd;
   private BWbFieldEditor statusFE;
   private BWbFieldEditor mouseOverFE;
   private BObjectCE borderCE;
   private BCellPane borderCEPane;
   protected BGridPane content = new BGridPane(1);

   public String getWidgetType() {
      return this.getString(widgetType);
   }

   public void setWidgetType(String v) {
      this.setString(widgetType, v, null);
   }

   public String getBindingType() {
      return this.getString(bindingType);
   }

   public void setBindingType(String v) {
      this.setString(bindingType, v, null);
   }

   public void hyperlinkToggled() {
      this.invoke(hyperlinkToggled, null, null);
   }

   public void statusToggled() {
      this.invoke(statusToggled, null, null);
   }

   public void formatTextToggled() {
      this.invoke(formatTextToggled, null, null);
   }

   public void mouseOverToggled() {
      this.invoke(mouseOverToggled, null, null);
   }

   public void borderToggled() {
      this.invoke(borderToggled, null, null);
   }

   public void formatTextFieldModified() {
      this.invoke(formatTextFieldModified, null, null);
   }

   public void mouseOverFEModified() {
      this.invoke(mouseOverFEModified, null, null);
   }

   public void statusFEModified() {
      this.invoke(statusFEModified, null, null);
   }

   public void borderCEModified() {
      this.invoke(borderCEModified, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BMwBoundLabel() {
      this.command = new BMwBoundLabel.Cmd();
   }

   @Override
   public void setMakeWidget(BMakeWidget mw) {
      super.setMakeWidget(mw);
      this.hyperlink = new ToggleCommand(mw, BMakeWidget.text("hyperlink"));
      this.status = new ToggleCommand(mw, BMakeWidget.text("status"));
      this.formatText = new ToggleCommand(mw, BMakeWidget.text("formatText"));
      this.mouseOver = new ToggleCommand(mw, BMakeWidget.text("mouseOver"));
      this.border = new ToggleCommand(mw, BMakeWidget.text("border"));
      this.unbound = new ToggleCommand(mw, BMakeWidget.text("unbound"));
      this.chkHyperlink = new BCheckBox(this.hyperlink);
      this.chkStatus = new BCheckBox(this.status);
      this.chkFormatText = new BCheckBox(this.formatText);
      this.chkMouseOver = new BCheckBox(this.mouseOver);
      this.chkBorder = new BCheckBox(this.border);
      this.chkUnbound = new BCheckBox(this.unbound);
      this.formatText.setSelected(true);
      this.formatMru = new BMruTextDropDown("makeWidgetFormatText", 20);
      this.formatMruEd = this.formatMru.getEditor();
      BFrozenEnum e = enumValue("kitPx:MouseOverEffect", "none");
      this.mouseOverFE = BWbFieldEditor.makeFor(e);
      this.mouseOverFE.loadValue(e);
      this.mouseOverFE.setEnabled(false);
      this.mouseOverFE.setReadonly(true);
      e = enumValue("kitPx:StatusEffect", "none");
      this.statusFE = BWbFieldEditor.makeFor(e);
      this.statusFE.loadValue(e);
      this.statusFE.setEnabled(false);
      this.statusFE.setReadonly(true);
      this.formatMru.computePreferredSize();
      this.mouseOverFE.computePreferredSize();
      double width = this.formatMru.getPreferredWidth();
      double height = this.mouseOverFE.getPreferredHeight();
      this.borderCE = new BObjectCE();
      this.borderCE.loadValue(BBorder.none);
      this.borderCE.setEnabled(false);
      this.borderCE.setReadonly(true);
      this.borderCEPane = new BCellPane(this.borderCE, width, height);
      this.borderCEPane.setEnabled(false);
      BGridPane grid = new BGridPane(2);
      grid.setValign(BValign.top);
      grid.setHalign(BHalign.left);
      grid.add(null, this.chkFormatText);
      grid.add(null, this.formatMru);
      grid.add(null, this.chkStatus);
      grid.add(null, this.statusFE);
      grid.add(null, this.chkHyperlink);
      grid.add(null, new BNullWidget());
      grid.add(null, this.chkMouseOver);
      grid.add(null, this.mouseOverFE);
      grid.add(null, this.chkBorder);
      grid.add(null, this.borderCEPane);
      this.content.setValign(BValign.top);
      this.content.setHalign(BHalign.left);
      this.content.add(null, grid);
      this.content.add(null, this.chkUnbound);
      this.setContent(new BBorderPane(this.content, BBorder.groove));
      this.linkTo(this.chkHyperlink, BCheckBox.actionPerformed, hyperlinkToggled);
      this.linkTo(this.chkStatus, BCheckBox.actionPerformed, statusToggled);
      this.linkTo(this.chkFormatText, BCheckBox.actionPerformed, formatTextToggled);
      this.linkTo(this.chkMouseOver, BCheckBox.actionPerformed, mouseOverToggled);
      this.linkTo(this.chkBorder, BCheckBox.actionPerformed, borderToggled);
      this.linkTo(this.mouseOverFE, BWbFieldEditor.pluginModified, mouseOverFEModified);
      this.linkTo(this.statusFE, BWbFieldEditor.pluginModified, statusFEModified);
      this.linkTo(this.borderCE, BObjectCE.pluginModified, borderCEModified);
   }

   public void doHyperlinkToggled() {
      BValueBinding bnd = (BValueBinding)this.mw.workingBinding();
      if (this.hyperlink.isSelected()) {
         bnd.setHyperlink(BMakeWidget.placeholder);
      } else {
         bnd.setHyperlink(BOrd.NULL);
      }

      this.mw.editWorkingWidget();
   }

   public void doMouseOverToggled() {
      BLabel lab = (BLabel)this.mw.getWorkingWidget();
      BFrozenEnum e = enumValue("kitPx:MouseOverEffect", this.mouseOver.isSelected() ? "outline" : "none");
      if (this.mouseOver.isSelected()) {
         this.mouseOverFE.setEnabled(true);
         this.mouseOverFE.setReadonly(false);
      } else {
         this.mouseOverFE.setEnabled(false);
         this.mouseOverFE.setReadonly(true);
      }

      this.mouseOverFE.loadValue(e);
      this.reflect(lab, "setMouseOver", e);
      this.mw.editWorkingWidget();
   }

   public void doStatusToggled() {
      BValueBinding bnd = (BValueBinding)this.mw.workingBinding();
      BFrozenEnum e = enumValue("kitPx:StatusEffect", this.status.isSelected() ? "colorAndBlink" : "none");
      if (this.status.isSelected()) {
         this.statusFE.setEnabled(true);
         this.statusFE.setReadonly(false);
      } else {
         this.statusFE.setEnabled(false);
         this.statusFE.setReadonly(true);
      }

      this.statusFE.loadValue(e);
      this.reflect(bnd, "setStatusEffect", e);
      this.mw.editWorkingWidget();
   }

   public void doBorderToggled() {
      BLabel lab = (BLabel)this.mw.getWorkingWidget();
      BBorder brd = this.border.isSelected() ? BBorder.solid : BBorder.none;
      if (this.border.isSelected()) {
         this.borderCEPane.setEnabled(true);
         this.borderCE.setEnabled(true);
         this.borderCE.setReadonly(false);
      } else {
         this.borderCEPane.setEnabled(false);
         this.borderCE.setEnabled(false);
         this.borderCE.setReadonly(true);
      }

      this.borderCE.loadValue(brd);
      this.reflect(lab, "setBorder", brd);
      this.mw.editWorkingWidget();
   }

   public void doFormatTextToggled() {
      BValueBinding bnd = (BValueBinding)this.mw.workingBinding();
      BObjectToString text = (BObjectToString)bnd.get("text");
      if (text != null) {
         bnd.remove("text");
      }

      this.initFormatText(bnd);
      this.mw.editWorkingWidget();
   }

   public void doFormatTextFieldModified() {
      BValueBinding bnd = (BValueBinding)this.mw.workingBinding();
      BObjectToString text = (BObjectToString)bnd.get("text");
      text.setFormat(BFormat.make(this.formatMru.getText()));
   }

   public void doMouseOverFEModified() {
      BLabel lab = (BLabel)this.mw.getWorkingWidget();
      this.reflectEditor(lab, "setMouseOver", this.mouseOverFE);
      this.mw.editWorkingWidget();
   }

   public void doStatusFEModified() {
      BValueBinding bnd = (BValueBinding)this.mw.workingBinding();
      this.reflectEditor(bnd, "setStatusEffect", this.statusFE);
      this.mw.editWorkingWidget();
   }

   public void doBorderCEModified() {
      BLabel lab = (BLabel)this.mw.getWorkingWidget();
      this.reflectEditor(lab, "setBorder", this.borderCE);
      this.mw.editWorkingWidget();
   }

   @Override
   public void load() {
      BObject[] objects = this.mw.getDrawingObjects();
      if (ClassUtil.anyNull(objects)) {
         this.command.setEnabled(false);
      } else {
         this.command.setEnabled(true);
      }
   }

   @Override
   public void setWorkingWidget() {
      if (!this.mw.isConstructing()) {
         BLabel lab = (BLabel)BTypeSpec.make(this.getWidgetType()).getInstance();
         BValueBinding bnd = (BValueBinding)BTypeSpec.make(this.getBindingType()).getInstance();
         if (this.hyperlink.isSelected()) {
            bnd.setHyperlink(BMakeWidget.placeholder);
         }

         this.reflectEditor(bnd, "setStatusEffect", this.statusFE);
         this.initFormatText(bnd);
         this.reflectEditor(lab, "setBorder", this.borderCE);
         this.reflectEditor(lab, "setMouseOver", this.mouseOverFE);
         addBinding(lab, bnd, BMakeWidget.placeholder);
         lab.setLayout(BLayout.make("0,0,100,20"));
         this.mw.setWorkingWidget(lab);
      }
   }

   @Override
   public int columnCount() {
      return this.unbound.isSelected() ? 2 : 1;
   }

   @Override
   public BWidget[] makePxWidgets(WidgetCopier wc) {
      if (!this.unbound.isSelected()) {
         return this.makeDefaultWidgets(wc);
      } else {
         BObject[] objects = this.mw.getDrawingObjects();
         BOrd[] ords = this.mw.getOrds();
         BWidget[] widgets = new BWidget[objects.length * 2];
         int y = 0;
         int n = 0;

         for (int i = 0; i < objects.length; i++) {
            widgets[n] = new BLabel(dumbLabel(objects[i]));
            ((BLabel)widgets[n]).setHalign(BHalign.left);
            widgets[n].setLayout(BLayout.make("0," + y + ",100,20"));
            widgets[++n] = wc.copyWidget(this.mw.getWorkingWidget());
            BBinding[] bindings = (BBinding[])widgets[n].getChildren(BBinding.class);
            if (bindings.length > 0) {
               bindings[0].setOrd(ords[i]);
               if (bindings[0] instanceof BValueBinding) {
                  BValueBinding v = (BValueBinding)bindings[0];
                  if (!v.getHyperlink().equals(BOrd.NULL)) {
                     v.setHyperlink(ords[i]);
                  }
               }
            }

            widgets[n].setLayout(BLayout.make("100," + y + ",100,20"));
            n++;
            y += 20;
         }

         return widgets;
      }
   }

   private static String dumbLabel(BObject obj) {
      if (obj instanceof BINavNode) {
         return ((BINavNode)obj).getNavDisplayName(null);
      } else {
         return obj instanceof BComplex ? ((BComplex)obj).getDisplayName(null) : TextUtil.toFriendly(obj.getType().getTypeName());
      }
   }

   @Override
   public void pickle(PropertyManager propMgr) {
      try {
         propMgr.setb("mwBoundLabelHyperlink", this.hyperlink.isSelected());
         propMgr.setb("mwBoundLabelStatus", this.status.isSelected());
         propMgr.setb("mwBoundLabelFormatText", this.formatText.isSelected());
         propMgr.setb("mwBoundLabelMouseOver", this.mouseOver.isSelected());
         propMgr.setb("mwBoundLabelBorder", this.border.isSelected());
         propMgr.setb("mwBoundLabelUnbound", this.unbound.isSelected());
         BFrozenEnum e = this.status.isSelected() ? (BFrozenEnum)this.statusFE.saveValue() : enumValue("kitPx:StatusEffect", 0);
         propMgr.seti("mwBoundLabelStatusFE", e.getOrdinal());
         e = this.mouseOver.isSelected() ? (BFrozenEnum)this.mouseOverFE.saveValue() : enumValue("kitPx:MouseOverEffect", 0);
         propMgr.seti("mwBoundLabelMouseOverFE", e.getOrdinal());
         BBorder b = this.border.isSelected() ? (BBorder)this.borderCE.saveValue() : BBorder.none;
         propMgr.set("mwBoundLabelBorderCE", b.toString());
         if (this.formatText.isSelected()) {
            this.formatMru.getTextAndSave();
         }
      } catch (Exception var4) {
         throw new BajaRuntimeException(var4);
      }
   }

   @Override
   public void unpickle(PropertyManager propMgr) {
      this.hyperlink.setSelected(propMgr.getb("mwBoundLabelHyperlink", false));
      this.status.setSelected(propMgr.getb("mwBoundLabelStatus", false));
      this.formatText.setSelected(propMgr.getb("mwBoundLabelFormatText", true));
      this.mouseOver.setSelected(propMgr.getb("mwBoundLabelMouseOver", false));
      this.border.setSelected(propMgr.getb("mwBoundLabelBorder", false));
      this.unbound.setSelected(propMgr.getb("mwBoundLabelUnbound", false));
      this.statusFE.loadValue(enumValue("kitPx:StatusEffect", this.status.isSelected() ? propMgr.geti("mwBoundLabelStatusFE", 0) : 0));
      boolean sel = this.status.isSelected();
      this.statusFE.setEnabled(sel);
      this.statusFE.setReadonly(!sel);
      this.mouseOverFE.loadValue(enumValue("kitPx:MouseOverEffect", this.mouseOver.isSelected() ? propMgr.geti("mwBoundLabelMouseOverFE", 0) : 0));
      sel = this.mouseOver.isSelected();
      this.mouseOverFE.setEnabled(sel);
      this.mouseOverFE.setReadonly(!sel);
      this.borderCE.loadValue(BBorder.make(this.border.isSelected() ? propMgr.get("mwBoundLabelBorderCE", "none") : "none"));
      sel = this.border.isSelected();
      this.borderCE.setEnabled(sel);
      this.borderCE.setReadonly(!sel);
      this.borderCEPane.setEnabled(sel);
   }

   private void initFormatText(BBinding bnd) {
      if (this.get("lnkFormatTextField") != null) {
         this.remove("lnkFormatTextField");
      }

      if (this.formatText.isSelected()) {
         this.formatMru.setEnabled(true);
         this.formatMru.setTextFromMruOptions();
         BObjectToString text = new BObjectToString();
         String str = this.formatMru.getText();
         if (str == "") {
            str = "%.%";
            this.formatMru.setText(str);
         }

         text.setFormat(BFormat.make(str));
         bnd.add("text", text);
      } else {
         this.formatMru.setEnabled(false);
         this.formatMru.setText("");
      }

      this.linkTo("lnkFormatTextField", this.formatMruEd, BTextEditor.textModified, formatTextFieldModified);
   }

   private static BFrozenEnum enumValue(String spec, String name) {
      BFrozenEnum froz = (BFrozenEnum)BTypeSpec.make(spec).getInstance();

      try {
         return (BFrozenEnum)froz.getClass().getField(name).get(froz);
      } catch (Exception var4) {
         throw new BajaRuntimeException(var4);
      }
   }

   private static BFrozenEnum enumValue(String spec, int ordinal) {
      try {
         Class<?> cls = BTypeSpec.make(spec).getResolvedType().getTypeClass();
         Method m = cls.getMethod("make", int.class);
         return (BFrozenEnum)m.invoke(null, ordinal);
      } catch (Exception var4) {
         throw new BajaRuntimeException(var4);
      }
   }

   private void reflectEditor(Object obj, String method, BWbEditor editorPane) {
      try {
         this.reflect(obj, method, editorPane.saveValue());
      } catch (Exception var5) {
         throw new BajaRuntimeException(var5);
      }
   }

   private void reflect(Object obj, String method, Object arg) {
      try {
         obj.getClass().getMethod(method, arg.getClass()).invoke(obj, arg);
      } catch (Exception var5) {
         throw new BajaRuntimeException(var5);
      }
   }

   private class Cmd extends ToggleCommand {
      Cmd() {
         super(BMwBoundLabel.this, BMakeWidget.text("boundLabel"));
      }

      public CommandArtifact doInvoke() throws Exception {
         if (this.isSelected()) {
            BMwBoundLabel.this.mw.getRightPane().setContent(BMwBoundLabel.this.mw.sheet());
            BMwBoundLabel.this.mw.getLeftPane().setContent(BMwBoundLabel.this);
            BMwBoundLabel.this.setWorkingWidget();
            BMwBoundLabel.this.mw.editWorkingWidget();
         }

         return null;
      }
   }
}
