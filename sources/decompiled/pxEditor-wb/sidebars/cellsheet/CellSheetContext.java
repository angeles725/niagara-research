package com.tridium.px.editor.sidebars.cellsheet;

import com.tridium.ui.px.PxPropertyComponentArray;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.ui.BBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.px.PxProperty;
import javax.baja.ui.transfer.TransferContext;
import javax.baja.util.BConverter;
import javax.baja.workbench.celleditor.BWbCellEditor;

public interface CellSheetContext {
   boolean allowBindingDelete();

   boolean allowGotoOrd();

   boolean allowLayoutEdit(BWidget[] var1);

   BObject[] resolveBindingTarget(BBinding[] var1);

   void bindingAdded(BWidget var1, Property var2, BBinding var3);

   void bindingDeleted(BWidget var1, Property var2, BBinding var3);

   void converterAdded(BBinding var1, Property var2, BConverter var3);

   void converterDeleted(BBinding var1, Property var2, BConverter var3);

   void bindingPropertyChanged(BBinding var1, Property var2, BValue var3);

   int newBindingDragOver(BPxCellSheetLabel var1, TransferContext var2);

   CommandArtifact newBindingDrop(TransferContext var1) throws Exception;

   int existingBindingDragOver(BPxCellSheetBindingLabel var1, TransferContext var2);

   CommandArtifact existingBindingDrop(TransferContext var1, BWidget var2, BBinding var3) throws Exception;

   void cellModified(BWbCellEditor var1);

   PxPropertyComponentArray getPxPropertyComponents();

   void pxPropertyLinked(PxProperty var1, BComponent var2, String var3);

   void pxPropertyUnlinked(PxProperty var1, BComponent var2, String var3);
}
