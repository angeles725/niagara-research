package com.tridium.program.ui;

import com.tridium.util.EscUtil;
import java.util.ArrayList;
import java.util.List;
import javax.baja.gx.BColor;
import javax.baja.gx.BImage;
import javax.baja.gx.Graphics;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BLabel;
import javax.baja.ui.BWidget;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.workbench.BWbEditor;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "labelMargin",
      type = "double",
      defaultValue = "10d"
   ), @NiagaraProperty(
      name = "rowGap",
      type = "double",
      defaultValue = "8d"
   ), @NiagaraProperty(
      name = "indent",
      type = "double",
      defaultValue = "0d"
   )})
@NiagaraAction(
   name = "handleEditorModified",
   parameterType = "BWidgetEvent",
   defaultValue = "new BWidgetEvent()"
)
public class BFieldSheet extends BWbEditor {
   public static final Property labelMargin = newProperty(0, 10.0, null);
   public static final Property rowGap = newProperty(0, 8.0, null);
   public static final Property indent = newProperty(0, 0.0, null);
   public static final Action handleEditorModified = newAction(0, new BWidgetEvent(), null);
   public static final Type TYPE = Sys.loadType(BFieldSheet.class);
   public static final String FORCE_SHOW = "fieldSheetShow";
   static final BColor dividerColor = BColor.make(12303291);
   static BImage defaultIcon = BImage.make("module://icons/x16/object.png");
   boolean showModified;
   private List<BFieldSheet.Field> fields;
   private double[] divs;

   public double getLabelMargin() {
      return this.getDouble(labelMargin);
   }

   public void setLabelMargin(double v) {
      this.setDouble(labelMargin, v, null);
   }

   public double getRowGap() {
      return this.getDouble(rowGap);
   }

   public void setRowGap(double v) {
      this.setDouble(rowGap, v, null);
   }

   public double getIndent() {
      return this.getDouble(indent);
   }

   public void setIndent(double v) {
      this.setDouble(indent, v, null);
   }

   public void handleEditorModified(BWidgetEvent parameter) {
      this.invoke(handleEditorModified, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BFieldSheet() {
      this(true);
   }

   public BFieldSheet(boolean showModified) {
      this.showModified = showModified;
   }

   public void doLoadValue(BObject o, Context cx) {
      BComponent obj = (BComponent)o;
      Property[] props = obj.getPropertiesArray();
      this.fields = new ArrayList<>();

      for (int i = 0; i < props.length; i++) {
         Property prop = props[i];
         if (!Flags.isHidden(obj, prop) && (!prop.isDynamic() || prop.getFacets().getb("fieldSheetShow", false))) {
            BValue propValue = obj.get(prop);
            BWbFieldEditor ed = BWbFieldEditor.makeFor(propValue, prop.getFacets());
            ed.setReadonly((prop.getDefaultFlags() & 1) != 0);
            ed.loadValue(propValue, new BasicContext(cx, prop.getFacets()));
            String ordList;
            BImage icon;
            if ((ordList = prop.getFacets().gets("iconOverride", null)) != null) {
               icon = BImage.make(BIcon.make(ordList));
            } else {
               icon = BImage.make(propValue.getIcon());
            }

            if (icon == null) {
               icon = defaultIcon;
            }

            BFieldSheet.Field f = new BFieldSheet.Field(prop, new BLabel(icon, EscUtil.slot.unescape(prop.getName())), ed);
            f.icon = icon;
            this.add("label" + i, f.label);
            this.add("editor" + i, f.editor);
            this.linkTo(f.editor, BWbFieldEditor.setModified, setModified);
            this.linkTo(f.editor, BWbFieldEditor.setModified, handleEditorModified);
            this.fields.add(f);
         }
      }

      this.divs = new double[this.fields.size()];
   }

   public BObject doSaveValue(BObject o, Context cx) throws Exception {
      BComponent obj = (BComponent)o;

      for (BFieldSheet.Field f : this.fields) {
         if (f.editor.isModified()) {
            obj.set(f.prop, (BValue)f.editor.saveValue(obj.get(f.prop), cx));
         }
      }

      for (BFieldSheet.Field field : this.fields) {
         field.editor.clearModified();
      }

      this.handleEditorModified(null);
      this.clearModified();
      return obj;
   }

   public void computePreferredSize() {
      if (this.fields == null) {
         this.setPreferredSize(0.0, 0.0);
      } else {
         double maxLabel = 0.0;
         double maxEditor = 0.0;
         double ph = 0.0;
         int fc = this.fields.size();

         for (int i = 0; i < fc; i++) {
            BFieldSheet.Field f = this.fields.get(i);
            f.label.computePreferredSize();
            f.editor.computePreferredSize();
            if (i == 0) {
               maxLabel = f.label.getPreferredWidth();
               maxEditor = f.editor.getPreferredWidth();
            } else {
               maxLabel = Math.max(maxLabel, f.label.getPreferredWidth());
               maxEditor = Math.max(maxEditor, f.editor.getPreferredWidth());
            }

            if (i != 0) {
               ph += this.getRowGap();
            }

            ph += Math.max(f.label.getPreferredHeight(), f.editor.getPreferredHeight());
         }

         this.setPreferredSize(this.getIndent() + maxLabel + maxEditor + this.getLabelMargin(), ph);
      }
   }

   public void doLayout(BWidget[] kids) {
      if (this.fields != null) {
         double labelWidth = 0.0;
         int fc = this.fields.size();

         for (int i = 0; i < fc; i++) {
            BFieldSheet.Field f = this.fields.get(i);
            f.label.computePreferredSize();
            f.editor.computePreferredSize();
            if (i == 0) {
               labelWidth = f.label.getPreferredWidth();
            } else {
               labelWidth = Math.max(labelWidth, f.label.getPreferredWidth());
            }
         }

         double y = 0.0;
         double indent = this.getIndent();

         for (int ix = 0; ix < fc; ix++) {
            BFieldSheet.Field field = this.fields.get(ix);
            double rowHeight = Math.max(field.label.getPreferredHeight(), field.editor.getPreferredHeight());
            field.label.setBounds(indent, y + (rowHeight - field.label.getPreferredHeight()) / 2.0, field.label.getPreferredWidth(), rowHeight);
            field.editor
               .setBounds(
                  indent + labelWidth + this.getLabelMargin(),
                  y + (rowHeight - field.editor.getPreferredHeight()) / 2.0,
                  field.editor.getPreferredWidth(),
                  rowHeight
               );
            y += rowHeight + this.getRowGap();
            this.divs[ix] = y;
         }
      }
   }

   public void paint(Graphics g) {
      this.paintChildren(g);

      for (double div : this.divs) {
         double y = div - this.getRowGap() / 2.0;
         g.setBrush(dividerColor);
         g.strokeLine(0.0, y, this.getWidth(), y);
      }
   }

   public void doHandleEditorModified(BWidgetEvent event) {
      if (this.showModified) {
         for (BFieldSheet.Field f : this.fields) {
            if (f.editor.isModified()) {
               f.label.setImage(f.icon.getHighlightedImage());
            } else {
               f.label.setImage(f.icon);
            }
         }

         this.repaint();
      }
   }

   private class Field {
      public Property prop;
      public BLabel label;
      public BWbFieldEditor editor;
      public BImage icon;

      public Field(Property prop, BLabel label, BWbFieldEditor editor) {
         this.prop = prop;
         this.label = label;
         this.editor = editor;
      }
   }
}
