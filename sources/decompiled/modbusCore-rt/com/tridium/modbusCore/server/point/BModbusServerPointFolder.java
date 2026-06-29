package com.tridium.modbusCore.server.point;

import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BAddressFormatEnum;
import javax.baja.control.BControlPoint;
import javax.baja.driver.point.BPointFolder;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BModbusServerPointFolder extends BPointFolder {
   public static final Type TYPE = Sys.loadType(BModbusServerPointFolder.class);

   public Type getType() {
      return TYPE;
   }

   public void childParented(Property prop, BValue newChild, Context context) {
      super.childParented(prop, newChild, context);
      if (newChild instanceof BControlPoint
         && ((BControlPoint)newChild).isWritablePoint()
         && ((BControlPoint)newChild).getProxyExt() instanceof BModbusServerProxyExt
         && this.writablePointAlreadyExists((BModbusServerProxyExt)((BControlPoint)newChild).getProxyExt())) {
         ((BModbusServerProxyExt)((BControlPoint)newChild).getProxyExt()).setDataAddress(new BFlexAddress(BAddressFormatEnum.hex, "-1"));
      }
   }

   private boolean writablePointAlreadyExists(BModbusServerProxyExt newExt) {
      if (newExt == null) {
         return false;
      } else {
         BControlPoint[] points = this.getDeviceExt().getPoints();
         if (points != null && points.length > 0) {
            int newOffset = 0;
            if (newExt instanceof BModbusServerNumericProxyExt
               && (((BModbusServerNumericProxyExt)newExt).isDataTypeLong() || ((BModbusServerNumericProxyExt)newExt).isDataTypeFloat())) {
               newOffset = 1;
            }

            int newAddress = newExt.getDataAddress().getDataAddress();

            for (int i = 0; i < points.length; i++) {
               if (!points[i].getProxyExt().equals(newExt) && points[i].getProxyExt() instanceof BModbusServerProxyExt && points[i].isWritablePoint()) {
                  if (points[i].getProxyExt() instanceof BModbusServerRegisterBitProxyExt && newExt instanceof BModbusServerRegisterBitProxyExt) {
                     int existAddress = ((BModbusServerProxyExt)points[i].getProxyExt()).getDataAddress().getDataAddress();
                     if (((BModbusServerProxyExt)points[i].getProxyExt()).determineRegisterType().equals(newExt.determineRegisterType())
                        && existAddress == newAddress
                        && ((BModbusServerRegisterBitProxyExt)points[i].getProxyExt()).getBitNumber()
                           == ((BModbusServerRegisterBitProxyExt)newExt).getBitNumber()) {
                        return true;
                     }
                  } else {
                     int existOffset = 0;
                     if (points[i].getProxyExt() instanceof BModbusServerNumericProxyExt
                        && (
                           ((BModbusServerNumericProxyExt)points[i].getProxyExt()).isDataTypeLong()
                              || ((BModbusServerNumericProxyExt)points[i].getProxyExt()).isDataTypeFloat()
                        )) {
                        existOffset = 1;
                     }

                     int existAddress = ((BModbusServerProxyExt)points[i].getProxyExt()).getDataAddress().getDataAddress();
                     if (((BModbusServerProxyExt)points[i].getProxyExt()).determineRegisterType().equals(newExt.determineRegisterType())
                        && (
                           existAddress == newAddress
                              || existAddress == newAddress + newOffset
                              || existAddress + existOffset == newAddress
                              || existAddress + existOffset == newAddress + newOffset
                        )) {
                        return true;
                     }
                  }
               }
            }

            return false;
         } else {
            return false;
         }
      }
   }
}
