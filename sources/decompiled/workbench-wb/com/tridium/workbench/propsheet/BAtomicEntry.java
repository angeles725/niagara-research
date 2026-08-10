package com.tridium.workbench.propsheet;

import com.tridium.workbench.fieldeditors.BDefaultSimpleFE;
import javax.baja.agent.AgentInfo;
import javax.baja.gx.Graphics;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIPropertyContainer;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.BValue;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
@NiagaraProperty(
   name = "editor",
   type = "BWbFieldEditor",
   defaultValue = "new BDefaultSimpleFE()",
   flags = 3
)
@NiagaraActions({@NiagaraAction(
      name = "handleEditorModified",
      parameterType = "BWidgetEvent",
      defaultValue = "new BWidgetEvent()"
   ), @NiagaraAction(
      name = "handleEditorAction",
      parameterType = "BWidgetEvent",
      defaultValue = "new BWidgetEvent()"
   )})
public class BAtomicEntry extends BPropertyEntry {
   public static final Property editor = newProperty(3, new BDefaultSimpleFE(), null);
   public static final Action handleEditorModified = newAction(0, new BWidgetEvent(), null);
   public static final Action handleEditorAction = newAction(0, new BWidgetEvent(), null);
   public static final Type TYPE = Sys.loadType(BAtomicEntry.class);
   private static final BPropertyEntry[] noEntries = new BPropertyEntry[0];

   public BWbFieldEditor getEditor() {
      return (BWbFieldEditor)this.get(editor);
   }

   public void setEditor(BWbFieldEditor v) {
      this.set(editor, v, null);
   }

   public void handleEditorModified(BWidgetEvent parameter) {
      this.invoke(handleEditorModified, parameter, null);
   }

   public void handleEditorAction(BWidgetEvent parameter) {
      this.invoke(handleEditorAction, parameter, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BAtomicEntry() {
   }

   public BAtomicEntry(BFieldEditorSheet sheet, BComplexEntry parent, Property prop, BObject target, AgentInfo editorType) {
      super(sheet, parent, prop, target);

      try {
         BWbFieldEditor editor = (BWbFieldEditor)editorType.getInstance();
         this.setEditor(editor);
         if (this.readonly) {
            editor.setReadonly(true);
         }

         this.linkTo(editor, BWbFieldEditor.actionPerformed, handleEditorAction);
         this.linkTo(editor, BWbFieldEditor.pluginModified, handleEditorModified);
      } catch (Exception var7) {
         System.out.println("BAtomicEntry<init> editorType=" + editorType);
         var7.printStackTrace();
      }
   }

   @Override
   public void init() {
      try {
         this.loadEditor();
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }

   @Override
   void computeContentSize() {
      BWidget editor = this.getEditor();
      editor.computePreferredSize();
      this.content.width = editor.getPreferredWidth();
      this.content.height = editor.getPreferredHeight();
   }

   @Override
   void positionsComputed() {
      this.getEditor().setBounds(this.content.x, this.content.y, this.content.width, this.content.height);
   }

   @Override
   void paintContent(Graphics g) {
      this.paintChild(g, this.getEditor());
   }

   @Override
   boolean isExpandable() {
      return false;
   }

   @Override
   BPropertyEntry[] getChildEntries() {
      return noEntries;
   }

   @Override
   void doSave(Context cx) throws Exception {
      boolean isSimple = this.target instanceof BSimple;
      BObject newTarget = this.getEditor().saveValue(this.target, cx);
      BPropertyEntry parent = (BPropertyEntry)this.getParent();
      if (isSimple) {
         if (parent.target instanceof BIPropertyContainer) {
            ((BIPropertyContainer)parent.target).set(this.property, (BValue)newTarget, cx);
         } else {
            parent.target.asComplex().set(this.property, (BValue)newTarget, cx);
         }
      } else if (parent != null && parent.target instanceof BIPropertyContainer && !parent.target.isComplex()) {
         ((BIPropertyContainer)parent.target).set(this.property, (BValue)newTarget, cx);
      }
   }

   @Override
   void doUpdate(BObject target) {
      this.target = target;
      if (!this.dirty) {
         try {
            this.loadEditor();
         } catch (Exception var3) {
            System.out.println("BAtomicEntry.update: " + var3);
         }

         this.repaint();
      }
   }

   @Override
   void clearDirty() {
      boolean wasDirty = this.dirty;
      super.clearDirty();
      if (wasDirty) {
         try {
            BPropertyEntry parent = (BPropertyEntry)this.getParent();
            if (parent == null || !(parent.target instanceof BIPropertyContainer) || parent.target.isComplex()) {
               this.loadEditor();
            }
         } catch (Exception var3) {
            System.out.println("BAtomicEntry.loadEditor: " + var3);
         }
      }
   }

   private void loadEditor() throws Exception {
      Context cx = this.sheet.getCurrentContext();
      BFacets facets = null;
      if (this.parentEntry.target instanceof BIPropertyContainer) {
         facets = ((BIPropertyContainer)this.parentEntry.target).getSlotFacets(this.property);
      } else {
         facets = this.parentEntry.target.asComplex().getSlotFacets(this.property);
      }

      this.getEditor().loadValue(this.target, new BasicContext(cx, facets));
   }

   public void doHandleEditorModified(BWidgetEvent event) {
      this.setDirty();
   }

   public void doHandleEditorAction(BWidgetEvent event) {
      BWbShell shell = this.sheet.getWbShell();
      if (shell != null) {
         shell.getSaveCommand().invoke();
      }
   }
}
