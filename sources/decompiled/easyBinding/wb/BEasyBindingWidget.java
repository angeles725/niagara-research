package com.honeywell.easybinding.ui;

import com.honeywell.easybinding.bindings.BEasyAlarmBinding;
import com.honeywell.easybinding.bindings.BEasyOverrideBinding;
import com.honeywell.easybinding.bindings.BEasyValueBinding;
import com.honeywell.easybinding.util.BEbLabelPosEnum;
import com.tridium.kitpx.BBoundLabel;
import java.math.BigDecimal;
import javax.baja.agent.BIAgent;
import javax.baja.control.BControlPoint;
import javax.baja.converters.BObjectToString;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.BImage;
import javax.baja.gx.Graphics;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.BVector;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBorder;
import javax.baja.ui.BLayout;
import javax.baja.ui.BWidget;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BScaleMode;
import javax.baja.ui.enums.BValign;
import javax.baja.util.BFormat;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "content",
      type = "gu:EasyBindingCanvasPane",
      defaultValue = "new BEasyBindingCanvasPane()",
      flags = 8
   ), @NiagaraProperty(
      name = "valueBinding",
      type = "gu:EasyValueBinding",
      defaultValue = "new BEasyValueBinding()",
      flags = 4
   ), @NiagaraProperty(
      name = "alarmBinding",
      type = "gu:EasyAlarmBinding",
      defaultValue = "new BEasyAlarmBinding()",
      flags = 4
   ), @NiagaraProperty(
      name = "overrideBinding",
      type = "gu:EasyOverrideBinding",
      defaultValue = "new BEasyOverrideBinding()",
      flags = 4
   ), @NiagaraProperty(
      name = "valueImage",
      type = "gx:Image",
      defaultValue = "BImage.NULL",
      flags = 4
   ), @NiagaraProperty(
      name = "valueOnImage",
      type = "gx:Image",
      defaultValue = "BImage.NULL",
      flags = 4
   ), @NiagaraProperty(
      name = "valueOffImage",
      type = "gx:Image",
      defaultValue = "BImage.NULL",
      flags = 4
   ), @NiagaraProperty(
      name = "alarmImage",
      type = "gx:Image",
      defaultValue = "BImage.make(BOrd.make(\"module://easyBinding/res/images/Alert_icon.png\"))",
      flags = 4
   ), @NiagaraProperty(
      name = "overrideImage",
      type = "gx:Image",
      defaultValue = "BImage.make(BOrd.make(\"module://easyBinding/res/images/Override.png\"))",
      flags = 4
   ), @NiagaraProperty(
      name = "labelPosition",
      type = "gu:EbLabelPosEnum",
      defaultValue = "BEbLabelPosEnum.bottom",
      flags = 8
   ), @NiagaraProperty(
      name = "showValue",
      type = "boolean",
      defaultValue = "true",
      flags = 8
   ), @NiagaraProperty(
      name = "showAlarm",
      type = "boolean",
      defaultValue = "true",
      flags = 8
   ), @NiagaraProperty(
      name = "showOverride",
      type = "boolean",
      defaultValue = "true",
      flags = 8
   ), @NiagaraProperty(
      name = "labelOnly",
      type = "boolean",
      defaultValue = "false",
      flags = 8
   ), @NiagaraProperty(
      name = "images",
      type = "BVector",
      defaultValue = "new BVector()",
      flags = 4
   ), @NiagaraProperty(
      name = "widgetName",
      type = "String",
      defaultValue = "Easy Widget",
      flags = 4
   ), @NiagaraProperty(
      name = "widgetPath",
      type = "String",
      defaultValue = "Easy Widget",
      flags = 4
   ), @NiagaraProperty(
      name = "valueImageScale",
      type = "BScaleMode",
      defaultValue = "BScaleMode.none",
      flags = 8
   ), @NiagaraProperty(
      name = "alarmImageScale",
      type = "BScaleMode",
      defaultValue = "BScaleMode.none",
      flags = 8
   ), @NiagaraProperty(
      name = "overrideImageScale",
      type = "BScaleMode",
      defaultValue = "BScaleMode.none",
      flags = 8
   )})
public class BEasyBindingWidget extends BWidget implements BIAgent {
   public static final Property content;
   public static final Property valueBinding;
   public static final Property alarmBinding;
   public static final Property overrideBinding;
   public static final Property valueImage;
   public static final Property valueOnImage;
   public static final Property valueOffImage;
   public static final Property alarmImage;
   public static final Property overrideImage;
   public static final Property labelPosition;
   public static final Property showValue;
   public static final Property showAlarm;
   public static final Property showOverride;
   public static final Property labelOnly;
   public static final Property images;
   public static final Property widgetName;
   public static final Property widgetPath;
   public static final Property valueImageScale;
   public static final Property alarmImageScale;
   public static final Property overrideImageScale;
   public static final Type TYPE;
   private String[] a;
   private static final String[] z;

   public BEasyBindingCanvasPane getContent() {
      return (BEasyBindingCanvasPane)this.get(content);
   }

   public void setContent(BEasyBindingCanvasPane var1) {
      this.set(content, var1, null);
   }

   public BEasyValueBinding getValueBinding() {
      return (BEasyValueBinding)this.get(valueBinding);
   }

   public void setValueBinding(BEasyValueBinding var1) {
      this.set(valueBinding, var1, null);
   }

   public BEasyAlarmBinding getAlarmBinding() {
      return (BEasyAlarmBinding)this.get(alarmBinding);
   }

   public void setAlarmBinding(BEasyAlarmBinding var1) {
      this.set(alarmBinding, var1, null);
   }

   public BEasyOverrideBinding getOverrideBinding() {
      return (BEasyOverrideBinding)this.get(overrideBinding);
   }

   public void setOverrideBinding(BEasyOverrideBinding var1) {
      this.set(overrideBinding, var1, null);
   }

   public BImage getValueImage() {
      return (BImage)this.get(valueImage);
   }

   public void setValueImage(BImage var1) {
      this.set(valueImage, var1, null);
   }

   public BImage getValueOnImage() {
      return (BImage)this.get(valueOnImage);
   }

   public void setValueOnImage(BImage var1) {
      this.set(valueOnImage, var1, null);
   }

   public BImage getValueOffImage() {
      return (BImage)this.get(valueOffImage);
   }

   public void setValueOffImage(BImage var1) {
      this.set(valueOffImage, var1, null);
   }

   public BImage getAlarmImage() {
      return (BImage)this.get(alarmImage);
   }

   public void setAlarmImage(BImage var1) {
      this.set(alarmImage, var1, null);
   }

   public BImage getOverrideImage() {
      return (BImage)this.get(overrideImage);
   }

   public void setOverrideImage(BImage var1) {
      this.set(overrideImage, var1, null);
   }

   public BEbLabelPosEnum getLabelPosition() {
      return (BEbLabelPosEnum)this.get(labelPosition);
   }

   public void setLabelPosition(BEbLabelPosEnum var1) {
      this.set(labelPosition, var1, null);
   }

   public boolean getShowValue() {
      return this.getBoolean(showValue);
   }

   public void setShowValue(boolean var1) {
      this.setBoolean(showValue, var1, null);
   }

   public boolean getShowAlarm() {
      return this.getBoolean(showAlarm);
   }

   public void setShowAlarm(boolean var1) {
      this.setBoolean(showAlarm, var1, null);
   }

   public boolean getShowOverride() {
      return this.getBoolean(showOverride);
   }

   public void setShowOverride(boolean var1) {
      this.setBoolean(showOverride, var1, null);
   }

   public boolean getLabelOnly() {
      return this.getBoolean(labelOnly);
   }

   public void setLabelOnly(boolean var1) {
      this.setBoolean(labelOnly, var1, null);
   }

   public BVector getImages() {
      return (BVector)this.get(images);
   }

   public void setImages(BVector var1) {
      this.set(images, var1, null);
   }

   public String getWidgetName() {
      return this.getString(widgetName);
   }

   public void setWidgetName(String var1) {
      this.setString(widgetName, var1, null);
   }

   public String getWidgetPath() {
      return this.getString(widgetPath);
   }

   public void setWidgetPath(String var1) {
      this.setString(widgetPath, var1, null);
   }

   public BScaleMode getValueImageScale() {
      return (BScaleMode)this.get(valueImageScale);
   }

   public void setValueImageScale(BScaleMode var1) {
      this.set(valueImageScale, var1, null);
   }

   public BScaleMode getAlarmImageScale() {
      return (BScaleMode)this.get(alarmImageScale);
   }

   public void setAlarmImageScale(BScaleMode var1) {
      this.set(alarmImageScale, var1, null);
   }

   public BScaleMode getOverrideImageScale() {
      return (BScaleMode)this.get(overrideImageScale);
   }

   public void setOverrideImageScale(BScaleMode var1) {
      this.set(overrideImageScale, var1, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BEasyBindingWidget() {
      this.a = new String[]{z[15], z[14], z[4], z[10], z[11], z[18], z[12], z[16], z[9], z[7], z[6], z[17]};
      BEasyBindingCanvasPane var1 = this.getContent();
      BEasyValueBinding var2 = new BEasyValueBinding();
      BObjectToString var3 = new BObjectToString();
      var3.setFormat(BFormat.make(z[8]));
      var2.add(z[13], var3);
      var1.getTextOverlayLabel().add(z[19], var2);
      BEasyValueBinding var4 = new BEasyValueBinding();
      var1.getContextMenuLabel().add(z[5], var4);
   }

   public void paint(Graphics var1) {
      var1.push();
      this.getContent().paint(var1);
      var1.pop();
      super.paint(var1);
   }

   public boolean hasValueOffImage(String var1) {
      for (String var5 : this.a) {
         if (var1.endsWith(var5)) {
            return true;
         }
      }

      return false;
   }

   public boolean hasValueOnImage(String var1) {
      for (String var5 : this.a) {
         if (var1.endsWith(var5)) {
            return true;
         }
      }

      return false;
   }

   public void doLayout(BWidget[] param1) {
      // $VF: Couldn't be decompiled
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      // java.lang.RuntimeException: parsing failure!
      //   at org.jetbrains.java.decompiler.modules.decompiler.decompose.DomHelper.parseGraph(DomHelper.java:211)
      //   at org.jetbrains.java.decompiler.main.rels.MethodProcessor.codeToJava(MethodProcessor.java:166)
      //
      // Bytecode:
      // 000: aload 0
      // 001: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getLayout ()Ljavax/baja/ui/BLayout;
      // 004: invokevirtual javax/baja/ui/BLayout.getWidth ()D
      // 007: dstore 2
      // 008: aload 0
      // 009: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getLayout ()Ljavax/baja/ui/BLayout;
      // 00c: invokevirtual javax/baja/ui/BLayout.getHeight ()D
      // 00f: dstore 4
      // 011: aload 0
      // 012: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getContent ()Lcom/honeywell/easybinding/ui/BEasyBindingCanvasPane;
      // 015: astore 6
      // 017: aload 0
      // 018: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getLayout ()Ljavax/baja/ui/BLayout;
      // 01b: invokevirtual javax/baja/ui/BLayout.getWidth ()D
      // 01e: invokestatic java/math/BigDecimal.valueOf (D)Ljava/math/BigDecimal;
      // 021: astore 7
      // 023: aload 0
      // 024: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getLayout ()Ljavax/baja/ui/BLayout;
      // 027: invokevirtual javax/baja/ui/BLayout.getHeight ()D
      // 02a: invokestatic java/math/BigDecimal.valueOf (D)Ljava/math/BigDecimal;
      // 02d: astore 8
      // 02f: bipush 0
      // 030: istore 9
      // 032: dconst_0
      // 033: dstore 10
      // 035: dconst_0
      // 036: dstore 12
      // 038: ldc2_w 10.0
      // 03b: invokestatic java/math/BigDecimal.valueOf (D)Ljava/math/BigDecimal;
      // 03e: astore 14
      // 040: aload 8
      // 042: aload 14
      // 044: invokevirtual java/math/BigDecimal.compareTo (Ljava/math/BigDecimal;)I
      // 047: ifne 08d
      // 04a: aload 7
      // 04c: aload 14
      // 04e: invokevirtual java/math/BigDecimal.compareTo (Ljava/math/BigDecimal;)I
      // 051: ifne 08d
      // 054: goto 058
      // 057: athrow
      // 058: aload 6
      // 05a: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getChildWidgets ()[Ljavax/baja/ui/BWidget;
      // 05d: bipush 0
      // 05e: aaload
      // 05f: instanceof com/honeywell/easybinding/easywidgets/BEasyPicture
      // 062: ifeq 08d
      // 065: goto 069
      // 068: athrow
      // 069: aload 6
      // 06b: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getChildWidgets ()[Ljavax/baja/ui/BWidget;
      // 06e: bipush 0
      // 06f: aaload
      // 070: checkcast com/honeywell/easybinding/easywidgets/BEasyPicture
      // 073: astore 15
      // 075: aload 15
      // 077: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.getImageSize ()Ljavax/baja/gx/Size;
      // 07a: astore 16
      // 07c: aload 16
      // 07e: invokevirtual javax/baja/gx/Size.height ()D
      // 081: dstore 12
      // 083: aload 16
      // 085: invokevirtual javax/baja/gx/Size.width ()D
      // 088: dstore 10
      // 08a: bipush 1
      // 08b: istore 9
      // 08d: aload 6
      // 08f: dload 2
      // 090: dload 4
      // 092: invokestatic javax/baja/gx/BSize.make (DD)Ljavax/baja/gx/BSize;
      // 095: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.setViewSize (Ljavax/baja/gx/BSize;)V
      // 098: aload 6
      // 09a: dconst_0
      // 09b: dconst_0
      // 09c: dload 2
      // 09d: dload 4
      // 09f: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.setBounds (DDDD)V
      // 0a2: dconst_0
      // 0a3: dconst_0
      // 0a4: dload 2
      // 0a5: dload 4
      // 0a7: invokestatic javax/baja/ui/BLayout.makeAbs (DDDD)Ljavax/baja/ui/BLayout;
      // 0aa: astore 15
      // 0ac: aload 6
      // 0ae: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getContextMenuLabel ()Ljavax/baja/ui/BLabel;
      // 0b1: dconst_0
      // 0b2: dconst_0
      // 0b3: aload 0
      // 0b4: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getContent ()Lcom/honeywell/easybinding/ui/BEasyBindingCanvasPane;
      // 0b7: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getLayout ()Ljavax/baja/ui/BLayout;
      // 0ba: invokevirtual javax/baja/ui/BLayout.getWidth ()D
      // 0bd: aload 0
      // 0be: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getContent ()Lcom/honeywell/easybinding/ui/BEasyBindingCanvasPane;
      // 0c1: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getLayout ()Ljavax/baja/ui/BLayout;
      // 0c4: invokevirtual javax/baja/ui/BLayout.getHeight ()D
      // 0c7: invokevirtual javax/baja/ui/BLabel.setBounds (DDDD)V
      // 0ca: aload 6
      // 0cc: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getContextMenuLabel ()Ljavax/baja/ui/BLabel;
      // 0cf: invokestatic com/honeywell/easybinding/util/Utils.returnBinding (Ljavax/baja/ui/BLabel;)Lcom/honeywell/easybinding/bindings/BEasyValueBinding;
      // 0d2: astore 16
      // 0d4: aload 16
      // 0d6: ifnull 0e9
      // 0d9: aload 16
      // 0db: aload 0
      // 0dc: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getValueBinding ()Lcom/honeywell/easybinding/bindings/BEasyValueBinding;
      // 0df: invokevirtual com/honeywell/easybinding/bindings/BEasyValueBinding.getOrd ()Ljavax/baja/naming/BOrd;
      // 0e2: invokevirtual com/honeywell/easybinding/bindings/BEasyValueBinding.setOrd (Ljavax/baja/naming/BOrd;)V
      // 0e5: goto 0e9
      // 0e8: athrow
      // 0e9: aload 0
      // 0ea: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getValueBinding ()Lcom/honeywell/easybinding/bindings/BEasyValueBinding;
      // 0ed: invokevirtual com/honeywell/easybinding/bindings/BEasyValueBinding.getOrd ()Ljavax/baja/naming/BOrd;
      // 0f0: getstatic javax/baja/naming/BOrd.NULL Ljavax/baja/naming/BOrd;
      // 0f3: invokevirtual javax/baja/naming/BOrd.equals (Ljava/lang/Object;)Z
      // 0f6: ifeq 158
      // 0f9: aload 0
      // 0fa: aload 0
      // 0fb: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getValueOffImage ()Ljavax/baja/gx/BImage;
      // 0fe: invokevirtual javax/baja/gx/BImage.toString ()Ljava/lang/String;
      // 101: invokevirtual java/lang/String.toLowerCase ()Ljava/lang/String;
      // 104: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.hasValueOffImage (Ljava/lang/String;)Z
      // 107: ifne 123
      // 10a: goto 10e
      // 10d: athrow
      // 10e: aload 0
      // 10f: aload 0
      // 110: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getValueOnImage ()Ljavax/baja/gx/BImage;
      // 113: invokevirtual javax/baja/gx/BImage.toString ()Ljava/lang/String;
      // 116: invokevirtual java/lang/String.toLowerCase ()Ljava/lang/String;
      // 119: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.hasValueOnImage (Ljava/lang/String;)Z
      // 11c: ifne 13e
      // 11f: goto 123
      // 122: athrow
      // 123: aload 6
      // 125: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getValueImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 128: aload 0
      // 129: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getValueOffImage ()Ljavax/baja/gx/BImage;
      // 12c: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setImage (Ljavax/baja/gx/BImage;)V
      // 12f: aload 0
      // 130: aload 0
      // 131: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getValueOffImage ()Ljavax/baja/gx/BImage;
      // 134: invokevirtual javax/baja/gx/BImage.toString ()Ljava/lang/String;
      // 137: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.setWidgetPath (Ljava/lang/String;)V
      // 13a: goto 17e
      // 13d: athrow
      // 13e: aload 6
      // 140: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getValueImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 143: aload 0
      // 144: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getValueOnImage ()Ljavax/baja/gx/BImage;
      // 147: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setImage (Ljavax/baja/gx/BImage;)V
      // 14a: aload 0
      // 14b: aload 0
      // 14c: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getValueOnImage ()Ljavax/baja/gx/BImage;
      // 14f: invokevirtual javax/baja/gx/BImage.toString ()Ljava/lang/String;
      // 152: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.setWidgetPath (Ljava/lang/String;)V
      // 155: goto 17e
      // 158: aload 6
      // 15a: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getValueImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 15d: aload 0
      // 15e: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getValueImage ()Ljavax/baja/gx/BImage;
      // 161: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setImage (Ljavax/baja/gx/BImage;)V
      // 164: aload 0
      // 165: aload 0
      // 166: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getValueImage ()Ljavax/baja/gx/BImage;
      // 169: invokevirtual javax/baja/gx/BImage.toString ()Ljava/lang/String;
      // 16c: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.setWidgetPath (Ljava/lang/String;)V
      // 16f: goto 17e
      // 172: astore 17
      // 174: aload 17
      // 176: invokevirtual java/lang/Exception.getMessage ()Ljava/lang/String;
      // 179: aload 17
      // 17b: invokestatic com/honeywell/easybinding/logger/EasyBindingLogger.error (Ljava/lang/String;Ljava/lang/Throwable;)V
      // 17e: aload 6
      // 180: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getValueImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 183: dconst_0
      // 184: dconst_0
      // 185: aload 0
      // 186: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getContent ()Lcom/honeywell/easybinding/ui/BEasyBindingCanvasPane;
      // 189: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getLayout ()Ljavax/baja/ui/BLayout;
      // 18c: invokevirtual javax/baja/ui/BLayout.getWidth ()D
      // 18f: aload 0
      // 190: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getContent ()Lcom/honeywell/easybinding/ui/BEasyBindingCanvasPane;
      // 193: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getLayout ()Ljavax/baja/ui/BLayout;
      // 196: invokevirtual javax/baja/ui/BLayout.getHeight ()D
      // 199: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setBounds (DDDD)V
      // 19c: aload 0
      // 19d: aload 6
      // 19f: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.layoutTextOverlayLabel (Lcom/honeywell/easybinding/ui/BEasyBindingCanvasPane;)V
      // 1a2: aload 0
      // 1a3: aload 6
      // 1a5: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getTextOverlayLabel ()Lcom/tridium/kitpx/BBoundLabel;
      // 1a8: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.checkStatusOfTextOverlayLabel (Lcom/tridium/kitpx/BBoundLabel;)V
      // 1ab: aload 0
      // 1ac: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getValueBinding ()Lcom/honeywell/easybinding/bindings/BEasyValueBinding;
      // 1af: invokevirtual com/honeywell/easybinding/bindings/BEasyValueBinding.newCopy ()Ljavax/baja/sys/BValue;
      // 1b2: checkcast com/honeywell/easybinding/bindings/BEasyValueBinding
      // 1b5: astore 17
      // 1b7: aload 6
      // 1b9: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getTextOverlayLabel ()Lcom/tridium/kitpx/BBoundLabel;
      // 1bc: invokestatic com/honeywell/easybinding/util/Utils.returnBinding (Ljavax/baja/ui/BLabel;)Lcom/honeywell/easybinding/bindings/BEasyValueBinding;
      // 1bf: astore 18
      // 1c1: aload 18
      // 1c3: ifnull 1d4
      // 1c6: aload 18
      // 1c8: aload 17
      // 1ca: invokevirtual com/honeywell/easybinding/bindings/BEasyValueBinding.getOrd ()Ljavax/baja/naming/BOrd;
      // 1cd: invokevirtual com/honeywell/easybinding/bindings/BEasyValueBinding.setOrd (Ljavax/baja/naming/BOrd;)V
      // 1d0: goto 1d4
      // 1d3: athrow
      // 1d4: bipush 0
      // 1d5: istore 19
      // 1d7: aload 0
      // 1d8: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getAlarmBinding ()Lcom/honeywell/easybinding/bindings/BEasyAlarmBinding;
      // 1db: invokevirtual com/honeywell/easybinding/bindings/BEasyAlarmBinding.getOrd ()Ljavax/baja/naming/BOrd;
      // 1de: astore 20
      // 1e0: aload 20
      // 1e2: invokevirtual javax/baja/naming/BOrd.isNull ()Z
      // 1e5: istore 21
      // 1e7: aload 6
      // 1e9: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getAlarmImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 1ec: iload 21
      // 1ee: ifeq 1f6
      // 1f1: bipush 0
      // 1f2: goto 1fa
      // 1f5: athrow
      // 1f6: aload 0
      // 1f7: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getShowAlarm ()Z
      // 1fa: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setVisible (Z)V
      // 1fd: aload 0
      // 1fe: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getAlarmImage ()Ljavax/baja/gx/BImage;
      // 201: getstatic com/honeywell/easybinding/ui/BEasyBindingWidget.alarmImage Ljavax/baja/sys/Property;
      // 204: invokeinterface javax/baja/sys/Property.getDefaultValue ()Ljavax/baja/sys/BValue; 1
      // 209: invokevirtual javax/baja/gx/BImage.equivalent (Ljava/lang/Object;)Z
      // 20c: ifne 253
      // 20f: aload 6
      // 211: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getAlarmImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 214: dconst_0
      // 215: dconst_0
      // 216: aload 0
      // 217: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getContent ()Lcom/honeywell/easybinding/ui/BEasyBindingCanvasPane;
      // 21a: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getLayout ()Ljavax/baja/ui/BLayout;
      // 21d: invokevirtual javax/baja/ui/BLayout.getWidth ()D
      // 220: aload 0
      // 221: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getContent ()Lcom/honeywell/easybinding/ui/BEasyBindingCanvasPane;
      // 224: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getLayout ()Ljavax/baja/ui/BLayout;
      // 227: invokevirtual javax/baja/ui/BLayout.getHeight ()D
      // 22a: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setBounds (DDDD)V
      // 22d: aload 6
      // 22f: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getAlarmImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 232: getstatic javax/baja/ui/enums/BHalign.center Ljavax/baja/ui/enums/BHalign;
      // 235: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setHalign (Ljavax/baja/ui/enums/BHalign;)V
      // 238: aload 6
      // 23a: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getAlarmImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 23d: getstatic javax/baja/ui/enums/BValign.center Ljavax/baja/ui/enums/BValign;
      // 240: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setValign (Ljavax/baja/ui/enums/BValign;)V
      // 243: aload 6
      // 245: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getAlarmImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 248: aload 0
      // 249: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getAlarmImageScale ()Ljavax/baja/ui/enums/BScaleMode;
      // 24c: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setScale (Ljavax/baja/ui/enums/BScaleMode;)V
      // 24f: goto 28c
      // 252: athrow
      // 253: aload 6
      // 255: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getAlarmImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 258: dconst_0
      // 259: dconst_0
      // 25a: aload 0
      // 25b: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getLayout ()Ljavax/baja/ui/BLayout;
      // 25e: invokevirtual javax/baja/ui/BLayout.getWidth ()D
      // 261: aload 0
      // 262: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getLayout ()Ljavax/baja/ui/BLayout;
      // 265: invokevirtual javax/baja/ui/BLayout.getHeight ()D
      // 268: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setBounds (DDDD)V
      // 26b: aload 6
      // 26d: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getAlarmImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 270: getstatic javax/baja/ui/enums/BHalign.left Ljavax/baja/ui/enums/BHalign;
      // 273: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setHalign (Ljavax/baja/ui/enums/BHalign;)V
      // 276: aload 6
      // 278: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getAlarmImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 27b: getstatic javax/baja/ui/enums/BValign.top Ljavax/baja/ui/enums/BValign;
      // 27e: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setValign (Ljavax/baja/ui/enums/BValign;)V
      // 281: aload 6
      // 283: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getAlarmImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 286: getstatic javax/baja/ui/enums/BScaleMode.none Ljavax/baja/ui/enums/BScaleMode;
      // 289: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setScale (Ljavax/baja/ui/enums/BScaleMode;)V
      // 28c: aload 6
      // 28e: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getAlarmImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 291: aload 0
      // 292: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getAlarmImage ()Ljavax/baja/gx/BImage;
      // 295: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setImage (Ljavax/baja/gx/BImage;)V
      // 298: aload 0
      // 299: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getOverrideBinding ()Lcom/honeywell/easybinding/bindings/BEasyOverrideBinding;
      // 29c: invokevirtual com/honeywell/easybinding/bindings/BEasyOverrideBinding.getOrd ()Ljavax/baja/naming/BOrd;
      // 29f: astore 22
      // 2a1: aload 22
      // 2a3: invokevirtual javax/baja/naming/BOrd.isNull ()Z
      // 2a6: istore 23
      // 2a8: aload 6
      // 2aa: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getOverrideImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 2ad: iload 23
      // 2af: ifeq 2b7
      // 2b2: bipush 0
      // 2b3: goto 2bb
      // 2b6: athrow
      // 2b7: aload 0
      // 2b8: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getShowOverride ()Z
      // 2bb: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setVisible (Z)V
      // 2be: aload 0
      // 2bf: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getOverrideImage ()Ljavax/baja/gx/BImage;
      // 2c2: getstatic com/honeywell/easybinding/ui/BEasyBindingWidget.overrideImage Ljavax/baja/sys/Property;
      // 2c5: invokeinterface javax/baja/sys/Property.getDefaultValue ()Ljavax/baja/sys/BValue; 1
      // 2ca: invokevirtual javax/baja/gx/BImage.equivalent (Ljava/lang/Object;)Z
      // 2cd: ifne 314
      // 2d0: aload 6
      // 2d2: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getOverrideImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 2d5: dconst_0
      // 2d6: dconst_0
      // 2d7: aload 0
      // 2d8: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getContent ()Lcom/honeywell/easybinding/ui/BEasyBindingCanvasPane;
      // 2db: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getLayout ()Ljavax/baja/ui/BLayout;
      // 2de: invokevirtual javax/baja/ui/BLayout.getWidth ()D
      // 2e1: aload 0
      // 2e2: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getContent ()Lcom/honeywell/easybinding/ui/BEasyBindingCanvasPane;
      // 2e5: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getLayout ()Ljavax/baja/ui/BLayout;
      // 2e8: invokevirtual javax/baja/ui/BLayout.getHeight ()D
      // 2eb: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setBounds (DDDD)V
      // 2ee: aload 6
      // 2f0: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getOverrideImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 2f3: getstatic javax/baja/ui/enums/BHalign.center Ljavax/baja/ui/enums/BHalign;
      // 2f6: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setHalign (Ljavax/baja/ui/enums/BHalign;)V
      // 2f9: aload 6
      // 2fb: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getOverrideImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 2fe: getstatic javax/baja/ui/enums/BValign.center Ljavax/baja/ui/enums/BValign;
      // 301: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setValign (Ljavax/baja/ui/enums/BValign;)V
      // 304: aload 6
      // 306: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getOverrideImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 309: aload 0
      // 30a: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getOverrideImageScale ()Ljavax/baja/ui/enums/BScaleMode;
      // 30d: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setScale (Ljavax/baja/ui/enums/BScaleMode;)V
      // 310: goto 34d
      // 313: athrow
      // 314: aload 6
      // 316: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getOverrideImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 319: dconst_0
      // 31a: dconst_0
      // 31b: aload 0
      // 31c: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getLayout ()Ljavax/baja/ui/BLayout;
      // 31f: invokevirtual javax/baja/ui/BLayout.getWidth ()D
      // 322: aload 0
      // 323: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getLayout ()Ljavax/baja/ui/BLayout;
      // 326: invokevirtual javax/baja/ui/BLayout.getHeight ()D
      // 329: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setBounds (DDDD)V
      // 32c: aload 6
      // 32e: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getOverrideImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 331: getstatic javax/baja/ui/enums/BHalign.left Ljavax/baja/ui/enums/BHalign;
      // 334: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setHalign (Ljavax/baja/ui/enums/BHalign;)V
      // 337: aload 6
      // 339: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getOverrideImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 33c: getstatic javax/baja/ui/enums/BValign.bottom Ljavax/baja/ui/enums/BValign;
      // 33f: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setValign (Ljavax/baja/ui/enums/BValign;)V
      // 342: aload 6
      // 344: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getOverrideImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 347: getstatic javax/baja/ui/enums/BScaleMode.none Ljavax/baja/ui/enums/BScaleMode;
      // 34a: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setScale (Ljavax/baja/ui/enums/BScaleMode;)V
      // 34d: aload 6
      // 34f: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getOverrideImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 352: aload 0
      // 353: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getOverrideImage ()Ljavax/baja/gx/BImage;
      // 356: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setImage (Ljavax/baja/gx/BImage;)V
      // 359: aload 0
      // 35a: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getLabelOnly ()Z
      // 35d: ifeq 37f
      // 360: aload 6
      // 362: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getAlarmImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 365: bipush 0
      // 366: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setVisible (Z)V
      // 369: aload 6
      // 36b: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getOverrideImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 36e: bipush 0
      // 36f: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setVisible (Z)V
      // 372: aload 6
      // 374: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getValueImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 377: bipush 0
      // 378: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setVisible (Z)V
      // 37b: goto 3b2
      // 37e: athrow
      // 37f: aload 6
      // 381: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getValueImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 384: bipush 1
      // 385: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setVisible (Z)V
      // 388: aload 6
      // 38a: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getValueImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 38d: aload 0
      // 38e: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.getValueImageScale ()Ljavax/baja/ui/enums/BScaleMode;
      // 391: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setScale (Ljavax/baja/ui/enums/BScaleMode;)V
      // 394: aload 6
      // 396: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getValueImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 399: aload 15
      // 39b: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setLayout (Ljavax/baja/ui/BLayout;)V
      // 39e: aload 6
      // 3a0: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getAlarmImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 3a3: aload 15
      // 3a5: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setLayout (Ljavax/baja/ui/BLayout;)V
      // 3a8: aload 6
      // 3aa: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.getOverrideImageLabel ()Lcom/honeywell/easybinding/easywidgets/BEasyPicture;
      // 3ad: aload 15
      // 3af: invokevirtual com/honeywell/easybinding/easywidgets/BEasyPicture.setLayout (Ljavax/baja/ui/BLayout;)V
      // 3b2: aload 0
      // 3b3: aload 6
      // 3b5: invokevirtual com/honeywell/easybinding/ui/BEasyBindingWidget.setContent (Lcom/honeywell/easybinding/ui/BEasyBindingCanvasPane;)V
      // 3b8: iload 9
      // 3ba: ifeq 3c9
      // 3bd: aload 0
      // 3be: dload 12
      // 3c0: dload 10
      // 3c2: invokespecial com/honeywell/easybinding/ui/BEasyBindingWidget.a (DD)V
      // 3c5: goto 3c9
      // 3c8: athrow
      // 3c9: aload 6
      // 3cb: invokevirtual com/honeywell/easybinding/ui/BEasyBindingCanvasPane.layout ()V
      // 3ce: return
   }

   private void a(double var1, double var3) {
      BLayout var5 = this.getLayout();
      BigDecimal var6 = BigDecimal.valueOf(var5.getWidth());
      BigDecimal var7 = BigDecimal.valueOf(var5.getHeight());
      BigDecimal var8 = BigDecimal.valueOf(10.0);
      if (var7.compareTo(var8) == 0 && var6.compareTo(var8) == 0) {
         BLayout var9 = BLayout.makeAbs(var5.getX(), var5.getY(), var3, var1);
         this.setLayout(var9);
      }
   }

   public void checkStatusOfTextOverlayLabel(BBoundLabel var1) {
      BEasyValueBinding var2 = (BEasyValueBinding)var1.getBindings()[0];
      if (var2.isBound()) {
         BControlPoint var3 = (BControlPoint)var2.getTarget().get();
         BStatus var4 = var3.getOutStatusValue().getStatus();
         if (!var4.isDown() && !var4.isDisabled() && !var4.isStale() && !var4.isFault()) {
            BObjectToString var7 = new BObjectToString();
            BFormat var8 = BFormat.make(z[1]);
            var7.setFormat(var8);
            var2.set(z[2], var7);
            this.layoutTextOverlayLabel((BEasyBindingCanvasPane)var1.getParentWidget());
         } else {
            BObjectToString var5 = new BObjectToString();
            BFormat var6 = BFormat.make(z[3].concat(var4.flagsToString(null)).concat(")"));
            var5.setFormat(var6);
            var2.set(z[0], var5);
         }
      }
   }

   public void layoutTextOverlayLabel(BEasyBindingCanvasPane var1) {
      BBoundLabel var2 = var1.getTextOverlayLabel();
      var2.setVisible(this.getShowValue());
      var2.setBackground(BBrush.makeSolid(BColor.white));
      var2.setBorder(BBorder.make(BBorder.solid, BBrush.makeSolid(BColor.black)));
      double var3 = var1.getWidth() * 0.5;
      double var5 = 18.0;
      double var7 = 0.0;
      double var9 = 0.0;
      var2.setHalign(BHalign.center);
      var2.setValign(BValign.center);
      switch (this.getLabelPosition().getOrdinal()) {
         case 1:
            var7 = 0.0;
            var9 = this.getLayout().getHeight() / 2.0 - var5 / 2.0;
            break;
         case 2:
            var7 = this.getLayout().getWidth() - var3;
            var9 = this.getLayout().getHeight() / 2.0 - var5 / 2.0;
            break;
         case 3:
            var7 = (this.getLayout().getWidth() - var3) / 2.0;
            var9 = var5 * 0.5;
            break;
         case 4:
            var7 = (this.getLayout().getWidth() - var3) / 2.0;
            var9 = this.getLayout().getHeight() - var5 * 1.5;
      }

      var2.setBounds(var7, var9, var3, var5);
      var2.setLayout(BLayout.makeAbs(var7, var9, var3, var5));
   }

   static {
      String[] var10000 = new String[20];
      String[] var10001 = var10000;
      int var10002 = 0;
      String var10003 = "RZ_\r";
      int var10004 = -1;

      while (true) {
         char[] var15 = var10003.toCharArray();
         int var10006 = var15.length;
         char[] var22 = var15;
         var10004 = var10006;

         for (int var0 = 0; var10004 > var0; var0++) {
            char var10008 = var22[var0];
            byte var10009;
            switch (var0 % 5) {
               case 0:
                  var10009 = 38;
                  break;
               case 1:
                  var10009 = 63;
                  break;
               case 2:
                  var10009 = 39;
                  break;
               case 3:
                  var10009 = 121;
                  break;
               default:
                  var10009 = 110;
            }

            var22[var0] = (char)(var10008 ^ var10009);
         }

         String var26 = new String(var22).intern();
         switch (var10004) {
            case 0:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 2;
               var10003 = "RZ_\r";
               var10004 = 1;
               break;
            case 1:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 3;
               var10003 = "\u0003PR\r@P^K\f\u000b\u0003\u001f\u000f";
               var10004 = 2;
               break;
            case 2:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 4;
               var10003 = "LO@";
               var10004 = 3;
               break;
            case 3:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 5;
               var10003 = "P^K\f\u000bdVI\u001d\u0007HX";
               var10004 = 4;
               break;
            case 4:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 6;
               var10003 = "OQC\u001d";
               var10004 = 5;
               break;
            case 5:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 7;
               var10003 = "GV";
               var10004 = 6;
               break;
            case 6:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 8;
               var10003 = "\u0003PR\r@P^K\f\u000b\u0003";
               var10004 = 7;
               break;
            case 7:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 9;
               var10003 = "COT";
               var10004 = 8;
               break;
            case 8:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 10;
               var10003 = "LOB\u001e";
               var10004 = 9;
               break;
            case 9:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 11;
               var10003 = "AVA";
               var10004 = 10;
               break;
            case 10:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 12;
               var10003 = "VLC";
               var10004 = 11;
               break;
            case 11:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 13;
               var10003 = "RZ_\r";
               var10004 = 12;
               break;
            case 12:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 14;
               var10003 = "VQ@";
               var10004 = 13;
               break;
            case 13:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 15;
               var10003 = "UI@";
               var10004 = 14;
               break;
            case 14:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 16;
               var10003 = "V[A";
               var10004 = 15;
               break;
            case 15:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 17;
               var10003 = "T^P";
               var10004 = 16;
               break;
            case 16:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 18;
               var10003 = "RVA\u001f";
               var10004 = 17;
               break;
            case 17:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 19;
               var10003 = "P^K\f\u000bdVI\u001d\u0007HX";
               var10004 = 18;
               break;
            case 18:
               var10001[var10002] = var26;
               z = var10000;
               content = newProperty(8, new BEasyBindingCanvasPane(), null);
               valueBinding = newProperty(4, new BEasyValueBinding(), null);
               alarmBinding = newProperty(4, new BEasyAlarmBinding(), null);
               overrideBinding = newProperty(4, new BEasyOverrideBinding(), null);
               valueImage = newProperty(4, BImage.NULL, null);
               valueOnImage = newProperty(4, BImage.NULL, null);
               valueOffImage = newProperty(4, BImage.NULL, null);
               byte var2 = 4;
               String var3 = "KPC\f\u0002C\u0005\bV\u000bGL^;\u0007H[N\u0017\t\tMB\nAORF\u001e\u000bU\u0010f\u0015\u000bTKx\u0010\rIQ\t\t\u0000A";
               var10002 = (byte)-1;

               while (true) {
                  char[] var7 = var3.toCharArray();
                  var10004 = var7.length;
                  char[] var12 = var7;
                  var10002 = var10004;

                  for (int var1 = 0; var10002 > var1; var1++) {
                     char var27 = var12[var1];
                     byte var10007;
                     switch (var1 % 5) {
                        case 0:
                           var10007 = 38;
                           break;
                        case 1:
                           var10007 = 63;
                           break;
                        case 2:
                           var10007 = 39;
                           break;
                        case 3:
                           var10007 = 121;
                           break;
                        default:
                           var10007 = 110;
                     }

                     var12[var1] = (char)(var27 ^ var10007);
                  }

                  String var21 = new String(var12).intern();
                  switch (var10002) {
                     case 0:
                        overrideImage = newProperty(var2, BImage.make(BOrd.make(var21)), null);
                        labelPosition = newProperty(8, BEbLabelPosEnum.bottom, null);
                        showValue = newProperty(8, true, null);
                        showAlarm = newProperty(8, true, null);
                        showOverride = newProperty(8, true, null);
                        labelOnly = newProperty(8, false, null);
                        images = newProperty(4, new BVector(), null);
                        var2 = 4;
                        var3 = "c^T\u0000NqVC\u001e\u000bR";
                        var10002 = (byte)1;
                        break;
                     case 1:
                        widgetName = newProperty(var2, var21, null);
                        var2 = 4;
                        var3 = "c^T\u0000NqVC\u001e\u000bR";
                        var10002 = (byte)2;
                        break;
                     case 2:
                        widgetPath = newProperty(var2, var21, null);
                        valueImageScale = newProperty(8, BScaleMode.none, null);
                        alarmImageScale = newProperty(8, BScaleMode.none, null);
                        overrideImageScale = newProperty(8, BScaleMode.none, null);
                        TYPE = Sys.loadType(BEasyBindingWidget.class);
                        return;
                     default:
                        alarmImage = newProperty(var2, BImage.make(BOrd.make(var21)), null);
                        var2 = 4;
                        var3 = "KPC\f\u0002C\u0005\bV\u000bGL^;\u0007H[N\u0017\t\tMB\nAORF\u001e\u000bU\u0010h\u000f\u000bTMN\u001d\u000b\bOI\u001e";
                        var10002 = (byte)0;
                  }
               }
            default:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 1;
               var10003 = "\u0003PR\r@P^K\f\u000b\u0003";
               var10004 = 0;
         }
      }
   }
}
