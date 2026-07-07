package com.honeywell.easybinding.bindings;

import com.honeywell.easybinding.converter.BIEbConverter;
import com.honeywell.easybinding.ui.BEasyBindingWidget;
import com.tridium.nv.comps.BNiagaraVirtualBooleanPoint;
import com.tridium.nv.comps.BNiagaraVirtualEnumPoint;
import com.tridium.nv.comps.BNiagaraVirtualNumericPoint;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import javax.baja.control.BBooleanPoint;
import javax.baja.control.BEnumPoint;
import javax.baja.control.BNumericPoint;
import javax.baja.converters.BEnumToSimpleMap;
import javax.baja.converters.BNumericToSimpleMap;
import javax.baja.gx.BImage;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BSimple;
import javax.baja.sys.BVector;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "previousOrd",
      type = "BOrd",
      defaultValue = "BOrd.NULL",
      flags = 12
   ), @NiagaraProperty(
      name = "ordsMatch",
      type = "boolean",
      defaultValue = "false",
      flags = 12
   )})
public class BEasyValueBinding extends BEasyBaseBinding {
   public static final Property previousOrd;
   public static final Property ordsMatch;
   public static final Type TYPE;
   private static final String[] z;

   public BOrd getPreviousOrd() {
      return (BOrd)this.get(previousOrd);
   }

   public void setPreviousOrd(BOrd var1) {
      this.set(previousOrd, var1, null);
   }

   public boolean getOrdsMatch() {
      return this.getBoolean(ordsMatch);
   }

   public void setOrdsMatch(boolean var1) {
      this.setBoolean(ordsMatch, var1, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   private void a(BWidget var1) {
      if (var1.getType().equals(BEasyBindingWidget.TYPE)) {
         BEasyBindingWidget var2 = (BEasyBindingWidget)var1;
         BComponent var3 = this.getTarget().getComponent();
         BImage var4 = var2.getValueOffImage();
         BImage var5 = var2.getValueOnImage();
         BVector var6 = var2.getImages();
         BIEbConverter var7 = new BIEbConverter();
         if (var3 instanceof BBooleanPoint || var3 instanceof BNiagaraVirtualBooleanPoint) {
            var2.getValueBinding().set(z[2], var7);
            var2.animateChildren();
            var7.setFalseValue(var4);
            var7.setTrueValue(var5);
         } else if (var3 instanceof BNumericPoint || var3 instanceof BNiagaraVirtualNumericPoint) {
            ArrayList var20 = new ArrayList();
            ArrayList var21 = new ArrayList();
            ArrayList var22 = new ArrayList();
            var2.getValueBinding().set(z[3], var7);
            var2.animateChildren();
            char var24 = DecimalFormatSymbols.getInstance().getDecimalSeparator();
            if (var6 != null && ((BImage[])var6.getChildren(BImage.class)).length > 0) {
               Property[] var25 = var6.asComponent().getDynamicPropertiesArray();

               for (Property var16 : var25) {
                  String var17 = var16.getName();
                  String var18 = var17.substring(1).replace('_', var24);
                  BImage var19 = (BImage)var6.asComponent().get(var16.getName());
                  this.a(var20, Double.parseDouble(var18), var22, var19);
               }

               for (int var28 = 1; var28 < var20.size(); var28++) {
                  var21.add((Double)var20.get(var28) - 1.0E-4);
               }

               var21.add(Double.POSITIVE_INFINITY);
            } else {
               var20.add(Double.NEGATIVE_INFINITY);
               var20.add(1.0);
               var21.add(0.99);
               var21.add(Double.POSITIVE_INFINITY);
               var22.add(var4);
               var22.add(var5);
            }

            double[] var26 = new double[var20.size()];
            double[] var29 = new double[var21.size()];
            BSimple[] var31 = new BSimple[var22.size()];

            for (int var34 = 0; var34 < var20.size(); var34++) {
               var26[var34] = (Double)var20.get(var34);
               var29[var34] = (Double)var21.get(var34);
               var31[var34] = (BSimple)var22.get(var34);
            }

            BNumericToSimpleMap var35 = BNumericToSimpleMap.make(var26, var29, var31, var4);
            var7.setNumMap(var35);
         } else if (var3 instanceof BEnumPoint || var3 instanceof BNiagaraVirtualEnumPoint) {
            Property[] var8 = var6.asComponent().getDynamicPropertiesArray();
            var2.getValueBinding().set(z[0], var7);
            var2.animateChildren();
            BEnumPoint var9 = (BEnumPoint)var3;
            BFacets var10 = var9.getEnumFacets();
            int[] var11 = new int[]{0, 1};
            BEnumRange var12 = (BEnumRange)var10.get(z[1]);
            var11 = var10.isEmpty() ? var11 : var12.getOrdinals();
            BSimple[] var13 = new BSimple[var11.length];
            int[][] var14 = new int[var11.length][1];

            for (int var15 = 0; var15 < var11.length; var15++) {
               if (var8.length > 0) {
                  var14[var15][0] = var11[var15];
                  var13[var15] = (BImage)var6.get("V" + var11[var15]);
               } else {
                  var14[var15][0] = var15;
                  if (var15 == 0) {
                     var13[var15] = var4;
                  } else {
                     var13[var15] = var5;
                  }
               }
            }

            BEnumToSimpleMap var32 = BEnumToSimpleMap.make(var14, var13, var4);
            var7.setEnumMap(var32);
         }
      }
   }

   private void a(ArrayList<Double> var1, double var2, ArrayList<BSimple> var4, BSimple var5) {
      if (var1.size() == 0) {
         var1.add(var2);
         var4.add(var5);
      } else {
         Double var6 = var2;
         ArrayList var7 = new ArrayList();
         var7.addAll(var1);
         Collections.sort(var7);

         for (Double var9 : var7) {
            if (var6.compareTo(var9) <= 0) {
               var6 = var9;
               break;
            }
         }

         int var10 = var1.indexOf(var6);
         if (var10 >= 0) {
            var1.add(var10, var2);
            var4.add(var10, var5);
         } else {
            var1.add(var2);
            var4.add(var5);
         }
      }
   }

   public void targetChanged() {
      this.setOrdsMatch(this.getOrd().equals(this.getPreviousOrd()));
      super.targetChanged();
      if (!this.getOrdsMatch()) {
         BWidget var1 = this.getWidget().getParentWidget().getParentWidget();
         this.a(var1);
         this.setPreviousOrd(this.getOrd());
      }
   }

   public boolean areOrdsMatching(BOrd var1) {
      return this.getOrd().equals(var1);
   }

   static {
      String[] var10000 = new String[4];
      String[] var10001 = var10000;
      byte var10002 = 0;
      String var10003 = "qzzp(Nvwb(";
      int var10004 = -1;

      while (true) {
         char[] var3 = var10003.toCharArray();
         int var10006 = var3.length;
         char[] var6 = var3;
         var10004 = var10006;

         for (int var0 = 0; var10004 > var0; var0++) {
            char var10008 = var6[var0];
            byte var10009;
            switch (var0 % 5) {
               case 0:
                  var10009 = 7;
                  break;
               case 1:
                  var10009 = 27;
                  break;
               case 2:
                  var10009 = 22;
                  break;
               case 3:
                  var10009 = 5;
                  break;
               default:
                  var10009 = 77;
            }

            var6[var0] = (char)(var10008 ^ var10009);
         }

         String var10 = new String(var6).intern();
         switch (var10004) {
            case 0:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 2;
               var10003 = "qzzp(Nvwb(";
               var10004 = 1;
               break;
            case 1:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 3;
               var10003 = "qzzp(Nvwb(";
               var10004 = 2;
               break;
            case 2:
               var10001[var10002] = var10;
               z = var10000;
               previousOrd = newProperty(12, BOrd.NULL, null);
               ordsMatch = newProperty(12, false, null);
               TYPE = Sys.loadType(BEasyValueBinding.class);
               return;
            default:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 1;
               var10003 = "uzxb(";
               var10004 = 0;
         }
      }
   }
}
