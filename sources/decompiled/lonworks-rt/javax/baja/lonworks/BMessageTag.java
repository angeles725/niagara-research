package javax.baja.lonworks;

import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BIcon;
import javax.baja.sys.BVector;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "index",
      type = "int",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "direction",
      type = "BLonNvDirection",
      defaultValue = "BLonNvDirection.input"
   )})
public class BMessageTag extends BVector {
   public static final Property index = newProperty(0, -1, null);
   public static final Property direction = newProperty(0, BLonNvDirection.input, null);
   public static final Type TYPE = Sys.loadType(BMessageTag.class);
   private static final BIcon icon = BIcon.make("module://lonworks/com/tridium/lonworks/ui/icons/mtag.png");

   public int getIndex() {
      return this.getInt(index);
   }

   public void setIndex(int v) {
      this.setInt(index, v, null);
   }

   public BLonNvDirection getDirection() {
      return (BLonNvDirection)this.get(direction);
   }

   public void setDirection(BLonNvDirection v) {
      this.set(direction, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BMessageTag() {
   }

   public boolean isInput() {
      return this.getDirection() == BLonNvDirection.input;
   }

   public boolean isOutput() {
      return this.getDirection() == BLonNvDirection.output;
   }

   public BIcon getIcon() {
      return icon;
   }

   public BMessageTag(int index, BLonNvDirection direction) {
      this.setIndex(index);
      this.setDirection(direction);
   }
}
