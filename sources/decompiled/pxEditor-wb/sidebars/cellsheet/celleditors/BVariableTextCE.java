package com.tridium.px.editor.sidebars.cellsheet.celleditors;

import com.tridium.ui.theme.Theme;
import com.tridium.workbench.cellmini.BMiniTextField;
import com.tridium.workbench.fieldeditors.BOrdFE;
import javax.baja.gx.BBrush;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.sys.Action;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BButton;
import javax.baja.ui.BDialog;
import javax.baja.ui.BWidget;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.text.TextRenderer;
import javax.baja.util.Lexicon;
import javax.baja.workbench.celleditor.BWbCellEditor;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "buttonMouseEvent",
      parameterType = "BMouseEvent",
      defaultValue = "new BMouseEvent()"
   ), @NiagaraAction(
      name = "textModified"
   )})
public class BVariableTextCE extends BWbCellEditor {
   public static final Action buttonMouseEvent = newAction(0, new BMouseEvent(), null);
   public static final Action textModified = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BVariableTextCE.class);
   private static Lexicon lexicon = Lexicon.make("wbutil");
   BButton button = new BButton();
   private boolean isPressed = false;
   private BMiniTextField textField = new BMiniTextField();

   public void buttonMouseEvent(BMouseEvent parameter) {
      this.invoke(buttonMouseEvent, parameter, null);
   }

   public void textModified() {
      this.invoke(textModified, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BVariableTextCE() {
      this.textField.setRenderer(new TextRenderer() {
         public BBrush getBackground() {
            return BVariableTextCE.this.getBackground();
         }
      });
      this.add("txtFld", this.textField);
      this.add("button", this.button);
      this.linkTo("linkMod", this.textField, BMiniTextField.textModified, textModified);
      this.linkTo("lkButton", this.button, BButton.mouseEvent, buttonMouseEvent);
   }

   public void paint(Graphics g) {
      this.paintBackground(g);
      super.paint(g);
      this.paintButton(g);
   }

   public void doLayout(BWidget[] children) {
      double w = this.getWidth();
      double h = this.getHeight();
      if (this.isReadonly()) {
         this.textField.setBounds(2.0, 2.0, w - 4.0, h - 2.0);
         this.button.setBounds(0.0, 0.0, 0.0, 0.0);
      } else {
         this.textField.setBounds(2.0, 2.0, w - 4.0 - h, h - 2.0);
         this.button.setBounds(w - 2.0 - h, 2.0, h, h - 4.0);
      }
   }

   public final void mousePressed(BMouseEvent event) {
      this.requestFocus();
      this.cellSelected();
      if (this.button.isEnabled()) {
         if (event.isButton1Down()) {
            this.buttonPressed();
         } else if (event.isButton3Down()) {
            this.cellPopup(event);
         }
      }
   }

   protected void doSetReadonly(boolean readonly) {
      this.textField.setEditable(!readonly);
   }

   protected void doLoadValue(BObject value, Context cx) throws Exception {
      super.doLoadValue(value, cx);
      this.remove("linkMod");
      this.textField.setText(this.escape(value.toString()));
      this.linkTo("linkMod", this.textField, BMiniTextField.textModified, textModified);
   }

   protected BObject doSaveValue(BObject value, Context cx) throws Exception {
      return BString.make(this.unescape(this.textField.getText()));
   }

   public void doTextModified() {
      this.setModified();
   }

   private void paintButton(Graphics g) {
      if (!this.isReadonly()) {
         this.paintChild(g, this.button);
         double x = this.button.getX() + 3.0;
         double y = this.button.getY() + 7.0;
         if (this.isPressed) {
            Point offset = new Point(1.0, 1.0);
            x += offset.x;
            y += offset.y;
         }

         if (this.button.getEnabled()) {
            g.setBrush(Theme.widget().getControlForeground());
         } else {
            g.setBrush(Theme.widget().getControlShadow());
         }

         for (int i = 0; i < 3; i++) {
            g.fillRect(x + i * 4, y, 2.0, 2.0);
         }
      }
   }

   public void doButtonMouseEvent(BMouseEvent event) {
      if (event.getId() == 501) {
         this.mousePressed(event);
         this.isPressed = true;
      } else if (event.getId() == 502) {
         this.isPressed = false;
      }
   }

   private String escape(String text) {
      text = TextUtil.replace(text, "\n", "\\n");
      return TextUtil.replace(text, "\r", "\\n");
   }

   private String unescape(String text) {
      return TextUtil.replace(text, "\\n", "\n");
   }

   private void buttonPressed() {
      if (!this.isReadonly()) {
         try {
            Context cx = new BasicContext(this.getCurrentContext(), BFacets.make("chooseView", true));
            BOrdFE fe = new BOrdFE();
            fe.loadValue(BOrd.NULL, cx);

            try {
               fe.loadValue(BOrd.make(this.textField.getText()), cx);
            } catch (Exception var5) {
            }

            int result = BDialog.open(this, this.getPropertyName(), fe, 3);
            if (result != 2) {
               BOrd ord = (BOrd)fe.saveValue();
               if (ord != null) {
                  this.textField.setText(ord.toString());
                  this.setModified();
               }
            }
         } catch (Exception var6) {
            BDialog.error(this, BDialog.TITLE_ERROR, text("buttonCE.dialogError"), var6);
         }
      }
   }

   public static String text(String s) {
      return lexicon.getText(s);
   }
}
