package com.tridium.workbench.user;

import com.tridium.workbench.fieldeditors.BPasswordFE;
import java.security.AccessController;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.security.BPassword;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BLabel;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.util.UiLexicon;
import javax.baja.util.Lexicon;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
public class BConfirmPasswordFE extends BWbFieldEditor {
   public static final Type TYPE = Sys.loadType(BConfirmPasswordFE.class);
   static final Lexicon lex = UiLexicon.bajaui();
   BPasswordFE prompt1 = new BPasswordFE();
   BPasswordFE prompt2 = new BPasswordFE();

   public Type getType() {
      return TYPE;
   }

   public BConfirmPasswordFE() {
      BGridPane pane = new BGridPane(2);
      pane.add(null, new BLabel(lex.getText("user.password")));
      pane.add(null, this.prompt1);
      pane.add(null, new BLabel(lex.getText("user.confirm")));
      pane.add(null, this.prompt2);
      this.setContent(pane);
      this.linkTo(null, this.prompt1, BPasswordFE.pluginModified, setModified);
      this.linkTo(null, this.prompt2, BPasswordFE.pluginModified, setModified);
      this.linkTo(null, this.prompt1, BPasswordFE.actionPerformed, actionPerformed);
      this.linkTo(null, this.prompt2, BPasswordFE.actionPerformed, actionPerformed);
   }

   protected void doSetReadonly(boolean readonly) {
      this.prompt1.setReadonly(readonly);
      this.prompt2.setReadonly(readonly);
   }

   protected void doLoadValue(BObject value, Context cx) {
      this.prompt1.loadValue(value, cx);
      this.prompt2.loadValue(value, cx);
   }

   protected BObject doSaveValue(BObject value, Context cx) throws Exception {
      BPassword p1 = (BPassword)this.prompt1.saveValue();
      BPassword p2 = (BPassword)this.prompt2.saveValue();
      if (!SecurityUtil.equals(AccessController.doPrivileged(p1::getValue), AccessController.doPrivileged(p2::getValue))) {
         throw new LocalizableException(lex, "user.passwordMismatch");
      } else {
         return p1;
      }
   }
}
