package com.tridium.px.editor.commands;

import com.tridium.px.editor.BPxEditorPane;
import java.util.logging.Logger;
import javax.baja.naming.BOrd;
import javax.baja.nav.BINavNode;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.ui.BHyperlinkMode;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.HyperlinkInfo;
import javax.baja.workbench.BWbShell;

public class GotoOrd extends Command {
   private final BWidget owner;
   private final BWbShell shell;
   private BOrd ord;

   public GotoOrd(BWidget owner, BOrd orig) {
      super(owner, BPxEditorPane.lexicon(), "commands.gotoOrd");
      this.owner = owner;
      this.shell = (BWbShell)owner.getShell();

      try {
         BObject obj = orig.get(this.shell.getActiveOrdTarget().get());
         if (obj instanceof BINavNode) {
            this.ord = ((BINavNode)obj).getNavOrd();
         } else {
            this.setEnabled(false);
         }
      } catch (BajaRuntimeException var4) {
         this.setEnabled(false);
         Logger.getLogger("pxEditor").warning("GotoOrd: " + var4.getMessage());
      }
   }

   public CommandArtifact doInvoke() throws Exception {
      this.shell.hyperlink(new HyperlinkInfo(this.ord, BHyperlinkMode.newTab));
      return null;
   }
}
