package javax.baja.px.editor.factory;

import com.tridium.file.types.bog.BBogFile;
import com.tridium.file.types.bog.BBogSpace;
import com.tridium.file.types.bog.BPaletteFile;
import com.tridium.px.editor.make.BMakeWidget;
import com.tridium.sys.module.BModulePaletteNode;
import java.util.Arrays;
import java.util.Objects;
import javax.baja.nav.BINavNode;
import javax.baja.px.editor.BPxEditor;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.ui.BDialog;

public class NavNodeFactory extends WidgetFactory {
   public NavNodeFactory(BPxEditor editor) {
      super(editor, BINavNode.TYPE);
   }

   @Override
   public WidgetInserter make(BObject[] objects) {
      if (!this.canConvert(objects)) {
         throw new IllegalStateException();
      } else {
         BMakeWidget dialog = new BMakeWidget(this.getPxEditor(), objects);
         int r = BDialog.open(this.getPxEditor(), dialog.getTitle(), dialog, 3);
         return r != 1 ? null : dialog.getWidgetInserter();
      }
   }

   @Override
   public boolean canConvert(BObject[] objects) {
      return super.canConvert(objects)
         && !Arrays.stream(objects)
            .filter(obj -> obj instanceof BComponent)
            .map(obj -> ((BComponent)obj).getComponentSpace())
            .filter(Objects::nonNull)
            .filter(space -> {
               if (space instanceof BModulePaletteNode) {
                  return true;
               } else {
                  if (space instanceof BBogSpace) {
                     BBogFile file = ((BBogSpace)space).getBogFile();
                     if (file != null && file instanceof BPaletteFile) {
                        return true;
                     }
                  }

                  return false;
               }
            })
            .findFirst()
            .isPresent();
   }
}
