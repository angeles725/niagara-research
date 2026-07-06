package javax.baja.px.editor;

import com.tridium.px.editor.commands.EditPropertiesContext;
import com.tridium.px.editor.factory.AliasNavNodeFactory;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BActionArgCE;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BVariablesCE;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BWidgetEventCE;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BWidgetPropertyCE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.baja.gx.BBrush;
import javax.baja.naming.BOrd;
import javax.baja.nre.util.Array;
import javax.baja.px.editor.factory.ImageFileFactory;
import javax.baja.px.editor.factory.JsFileFactory;
import javax.baja.px.editor.factory.LabelFactory;
import javax.baja.px.editor.factory.NavNodeFactory;
import javax.baja.px.editor.factory.PictureFactory;
import javax.baja.px.editor.factory.PxFileFactory;
import javax.baja.px.editor.factory.WidgetCloningFactory;
import javax.baja.px.editor.factory.WidgetFactory;
import javax.baja.px.editor.factory.WidgetInserter;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BMenu;
import javax.baja.ui.Command;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.px.BPxInclude;
import javax.baja.ui.transfer.TransferContext;
import javax.baja.util.BTypeSpec;
import javax.baja.workbench.celleditor.BWbCellEditor;

public class PxEditorController {
   private static Type ACTION_BINDING = null;
   private static Type SETPOINT_BINDING = null;
   private static Type SPECTRUM_BINDING = null;
   private final BPxEditor editor;
   private final List<WidgetFactory> factories = new ArrayList<>();

   public PxEditorController(BPxEditor editor) {
      this.editor = editor;
      this.factories.addAll(new ArrayList<>(Arrays.asList(this.getDefaultWidgetFactories())));
   }

   public BWbCellEditor getCellEditor(BComponent component, Property property) {
      if (component instanceof BBinding) {
         BWbCellEditor ce = makeBindingCE(this.editor, (BBinding)component, property);
         ce.loadValue(component.get(property), component.getSlotFacets(property));
         return ce;
      } else if (component instanceof BPxInclude && property.getName().equals("variables")) {
         return makePxIncludeVariablesCE((BPxInclude)component);
      } else {
         BWbCellEditor ce = BWbCellEditor.makeFor(component.get(property), component.getSlotFacets(property));
         ce.loadValue(component.get(property), component.getSlotFacets(property));
         return ce;
      }
   }

   public BMenu getPopupMenu(BIPxTransferWidget transferWidget, BMouseEvent event) {
      return transferWidget.getDefaultPopupMenu(event);
   }

   public boolean allowDrop(BIPxTransferWidget transferWidget, TransferContext cx) {
      return true;
   }

   public Command getDoubleClickCommand(BIPxTransferWidget transferWidget, BMouseEvent event) {
      return new EditPropertiesContext(this.editor);
   }

   public final WidgetFactory[] getDefaultWidgetFactories() {
      return new WidgetFactory[]{
         new LabelFactory(this.editor),
         new PictureFactory(this.editor),
         new WidgetCloningFactory(this.editor),
         new PxFileFactory(this.editor),
         new ImageFileFactory(this.editor),
         new JsFileFactory(this.editor),
         new AliasNavNodeFactory(this.editor),
         new NavNodeFactory(this.editor)
      };
   }

   public WidgetFactory[] getWidgetFactories() {
      return this.factories.toArray(new WidgetFactory[0]);
   }

   public void addWidgetFactory(WidgetFactory factory) {
      if (!this.factories.contains(factory)) {
         this.factories.add(factory);
      }
   }

   public void removeWidgetFactory(WidgetFactory factory) {
      this.factories.remove(factory);
   }

   public WidgetInserter getWidgetInserter(BIPxTransferWidget transferWidget, BObject[] objects) {
      for (int i = 0; i < this.factories.size(); i++) {
         WidgetFactory f = this.factories.get(i);
         if (f.canConvert(objects)) {
            return f.make(objects);
         }
      }

      throw new BajaRuntimeException("Cannot create widgets for " + new Array(objects));
   }

   private static BWbCellEditor makeBindingCE(BPxEditor editor, BBinding binding, Property property) {
      if (property.getName().equals("widgetEvent")) {
         if (isType(binding.getType(), ACTION_BINDING)) {
            return new BWidgetEventCE(binding.getWidget(), "started");
         }

         if (isType(binding.getType(), SETPOINT_BINDING)) {
            return new BWidgetEventCE(binding.getWidget());
         }
      } else {
         if (property.getName().equals("widgetProperty")) {
            return new BWidgetPropertyCE(binding.getWidget(), isType(binding.getType(), SPECTRUM_BINDING) ? BBrush.TYPE : BObject.TYPE);
         }

         if (property.getName().equals("actionArg") && isType(binding.getType(), ACTION_BINDING)) {
            return new BActionArgCE(getActionForOrd(editor, binding.getOrd()));
         }
      }

      return BWbCellEditor.makeFor(binding.get(property));
   }

   private static Action getActionForOrd(BPxEditor editor, BOrd ord) {
      if (ord != null && !ord.isNull()) {
         try {
            BObject base = editor.getWbShell().getActiveOrdTarget().get();
            return (Action)ord.resolve(base).getSlotInComponent();
         } catch (Exception var3) {
            Logger.getLogger("pxEditor").warning("BPxProfile.getActionForOrd: " + var3.getMessage());
            return null;
         }
      } else {
         return null;
      }
   }

   private static BWbCellEditor makePxIncludeVariablesCE(BPxInclude inc) {
      if (inc.getRootWidget() == null) {
         BVariablesCE ce = new BVariablesCE();
         ce.loadValue(BFacets.NULL, null);
         ce.setReadonly(true);
         return ce;
      } else {
         Object[] obj = (Object[])inc.fw(304, null, null, null, null);
         BOrd[] origOrds = (BOrd[])obj[0];
         BVariablesCE ce = new BVariablesCE(origOrds);
         ce.loadValue(BVariablesCE.makeFacets(inc), inc.getSlotFacets(BPxInclude.variables));
         return ce;
      }
   }

   private static boolean isType(Type type, Type is) {
      return is == null ? false : type.is(is);
   }

   public BPxEditor getPxEditor() {
      return this.editor;
   }

   static {
      try {
         ACTION_BINDING = BTypeSpec.make("kitPx:ActionBinding").getResolvedType();
         SETPOINT_BINDING = BTypeSpec.make("kitPx:SetPointBinding").getResolvedType();
         SPECTRUM_BINDING = BTypeSpec.make("kitPx:SpectrumBinding").getResolvedType();
      } catch (Exception var1) {
         Logger.getLogger("pxEditor").warning("BPxProfile<clinit>: " + var1.getMessage());
      }
   }
}
