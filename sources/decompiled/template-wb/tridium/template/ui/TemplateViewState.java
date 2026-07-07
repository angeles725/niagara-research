package com.tridium.template.ui;

import javax.baja.naming.BOrd;
import javax.baja.nre.util.Array;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.view.BWbView;

public class TemplateViewState {
   private static Array<TemplateViewState> history = new Array(TemplateViewState.class);
   final String key;
   Type lastSelectedTab;
   BOrd bogSelOrd;
   int programSelIndex;
   int bogSelPane;

   static void save(BTemplateView view) {
      try {
         String key = toKey(view);
         TemplateViewState state = new TemplateViewState(key);
         state.doSave(view);
         add(state);
      } catch (Exception var3) {
         var3.printStackTrace();
      }
   }

   static void restore(BTemplateView view) {
      try {
         String key = toKey(view);
         TemplateViewState state = get(key);
         if (state != null) {
            state.doRestore(view);
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }
   }

   private TemplateViewState(String key) {
      this.key = key;
   }

   private static String toKey(BWbView view) {
      BWbShell shell = view.getWbShell();
      return shell == null ? null : "" + shell.getActiveOrdTarget().getOrdWithoutViewQuery();
   }

   private static TemplateViewState get(String key) {
      for (int i = history.size() - 1; i >= 0; i--) {
         TemplateViewState state = (TemplateViewState)history.get(i);
         if (state != null && state.key != null && state.key.equals(key)) {
            return state;
         }
      }

      return (TemplateViewState)history.last();
   }

   private static void add(TemplateViewState state) {
      TemplateViewState orig = get(state.key);
      if (orig != null && orig.key == state.key) {
         history.remove(orig);
      }

      if (history.size() > 10) {
         history.remove(0);
      }

      history.add(state);
   }

   private void doSave(BTemplateView view) {
      BWidget content = view.tp.getSelectedLabelPane().getContent();
      if (content instanceof BBorderPane) {
         content = ((BBorderPane)content).getContent();
      }

      this.lastSelectedTab = content.getType();
      this.bogSelPane = view.bogPane.selPane;
      if (content instanceof BTemplateBogEditor) {
         this.bogSelOrd = ((BTemplateBogEditor)content).bogSelOrd;
         this.programSelIndex = ((BTemplateBogEditor)content).programSelIndex;
      }
   }

   private void doRestore(BTemplateView view) {
      BWidget content = null;
      if (this.lastSelectedTab.is(BTemplateBogEditor.TYPE)) {
         content = view.bogPane;
      } else if (this.lastSelectedTab.is(BTemplateConfigEditor.TYPE)) {
         content = view.settingsPane;
      } else if (this.lastSelectedTab.is(BTemplateIOEditor.TYPE)) {
         content = view.ioPane;
      } else if (this.lastSelectedTab.is(BTemplateManager.TYPE)) {
         content = view.templateMgr;
      }

      if (content != null) {
         view.tp.selectPane(content.getParentWidget());
      }

      view.bogPane.selPane = this.bogSelPane;
      view.bogPane.bogSelOrd = this.bogSelOrd;
      view.bogPane.programSelIndex = this.programSelIndex;
   }
}
