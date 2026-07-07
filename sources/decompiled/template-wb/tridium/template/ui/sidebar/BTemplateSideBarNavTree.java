package com.tridium.template.ui.sidebar;

import com.tridium.template.ui.BTemplateView;
import com.tridium.template.ui.file.BWbDeployableNtplFile;
import com.tridium.workbench.transfer.TransferUtil;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.Mark;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BDialog;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BPane;
import javax.baja.ui.transfer.BTransferWidget;
import javax.baja.util.Lexicon;
import javax.baja.workbench.nav.tree.BNavTree;
import javax.baja.workbench.view.BWbView;

@NiagaraType
public class BTemplateSideBarNavTree extends BNavTree {
   public static final Type TYPE = Sys.loadType(BTemplateSideBarNavTree.class);
   private static final Lexicon lex = Lexicon.make("template");

   public Type getType() {
      return TYPE;
   }

   public CommandArtifact doDelete() throws Exception {
      BObject[] selection = this.getSelectedObjects();
      if (selection.length == 0) {
         return null;
      } else {
         for (BObject object : selection) {
            if (object instanceof BWbDeployableNtplFile) {
               BWbDeployableNtplFile file = (BWbDeployableNtplFile)object;
               if (file.isOpen()) {
                  file.close();
               }
            }
         }

         Mark mark = new Mark(selection);

         try {
            return TransferUtil.delete(this, mark);
         } catch (IOException var7) {
            BDialog.error(
               this.getShell(),
               lex.getText("commands.delete.error.dialog.title"),
               lex.getText("commands.delete.error.dialog.message"),
               var7.getLocalizedMessage()
            );
            return null;
         }
      }
   }

   public void mouseDragStarted(BMouseEvent event) {
      for (BObject selected : this.getSelectedObjects()) {
         if (selected instanceof BWbDeployableNtplFile) {
            BWbDeployableNtplFile file = (BWbDeployableNtplFile)selected;
            BWidget shell = this.getShell();
            Optional<BWidget> templateViewWidget = this.findTemplateView(shell);
            if (!templateViewWidget.isPresent()) {
               super.mouseDragStarted(event);
               return;
            }

            BTemplateView templateView = (BTemplateView)templateViewWidget.get();
            if (file.getTitle().equals(templateView.getManifest().title)) {
               BDialog.warning(
                  this.getShell(),
                  lex.getText("templateSideBar.dragTemplateError.title"),
                  lex.getText("templateSideBar.dragTemplateError.message", new Object[]{file.getTitle()})
               );
               return;
            }
         }
      }

      super.mouseDragStarted(event);
   }

   private Optional<BWidget> findTemplateView(BWidget root) {
      Queue<BWidget> q = new ArrayDeque<>();
      q.add(root);

      while (!q.isEmpty()) {
         BWidget node = q.remove();
         if (node.getType().is(BTemplateView.TYPE)) {
            return Optional.of(node);
         }

         for (BWidget child : node.getChildWidgets()) {
            Type childType = child.getType();
            if (childType.is(BPane.TYPE) || childType.is(BWbView.TYPE) || childType.is(BTransferWidget.TYPE)) {
               q.add(child);
            }
         }
      }

      return Optional.empty();
   }
}
