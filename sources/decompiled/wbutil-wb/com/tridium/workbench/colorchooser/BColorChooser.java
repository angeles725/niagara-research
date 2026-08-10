package com.tridium.workbench.colorchooser;

import com.tridium.ui.theme.Theme;
import java.util.ArrayList;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.BFont;
import javax.baja.gx.BInsets;
import javax.baja.gx.Graphics;
import javax.baja.gx.BBrush.Solid;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BModule;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBorder;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BSlider;
import javax.baja.ui.BTextDropDown;
import javax.baja.ui.BTextField;
import javax.baja.ui.BToolBar;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.ui.list.BList;
import javax.baja.ui.list.ListRenderer;
import javax.baja.ui.list.ListRenderer.Item;
import javax.baja.ui.options.BOptions;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BScrollPane;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "rgbModified",
      parameterType = "BWidgetEvent",
      defaultValue = "new BWidgetEvent()"
   ), @NiagaraAction(
      name = "hsbModified",
      parameterType = "BWidgetEvent",
      defaultValue = "new BWidgetEvent()"
   ), @NiagaraAction(
      name = "xmlModified"
   ), @NiagaraAction(
      name = "nameModified"
   ), @NiagaraAction(
      name = "alphaModified",
      parameterType = "BWidgetEvent",
      defaultValue = "new BWidgetEvent()"
   )})
public class BColorChooser extends BEdgePane implements ColorModel.Agent {
   public static final Action rgbModified = newAction(0, new BWidgetEvent(), null);
   public static final Action hsbModified = newAction(0, new BWidgetEvent(), null);
   public static final Action xmlModified = newAction(0, null);
   public static final Action nameModified = newAction(0, null);
   public static final Action alphaModified = newAction(0, new BWidgetEvent(), null);
   public static final Type TYPE = Sys.loadType(BColorChooser.class);
   private static BModule module = Sys.getModuleForClass(BColorChooser.class);
   private static Lexicon lex = Lexicon.make("wbutil");
   private ColorModel model;
   private boolean ignoreEvents = false;
   private boolean fromHsb = false;
   private boolean fromRgb = false;
   private boolean fromXml = false;
   private boolean fromName = false;
   private boolean fromAlpha = false;
   private BBrushSwatch swatch;
   private BTextField redField;
   private BTextField greField;
   private BTextField bluField;
   private BSlider redSlider;
   private BSlider greSlider;
   private BSlider bluSlider;
   private BTextField hueField;
   private BTextField satField;
   private BTextField briField;
   private BSlider hueSlider;
   private BSlider satSlider;
   private BSlider briSlider;
   private BTextField alphaField;
   private BSlider alphaSlider;
   private BCheckBox nullColor;
   private BTextField xmlField;
   private BTextDropDown nameField;
   private ArrayList<BBrush> customBrushes = new ArrayList<>();
   private BBrushList customBrushList;
   private BColorChooser.Add addCommand;
   private BBrush[] defaultBrushList = new BBrush[]{
      BColor.make(-65536).toBrush(),
      BColor.make(-12582912).toBrush(),
      BColor.make(-8388608).toBrush(),
      BColor.make(-4194304).toBrush(),
      BColor.make(-49088).toBrush(),
      BColor.make(-32640).toBrush(),
      BColor.make(-16192).toBrush(),
      BColor.make(-16777216).toBrush(),
      BColor.make(-32768).toBrush(),
      BColor.make(-12574720).toBrush(),
      BColor.make(-8372224).toBrush(),
      BColor.make(-4235264).toBrush(),
      BColor.make(-24512).toBrush(),
      BColor.make(-16256).toBrush(),
      BColor.make(-8257).toBrush(),
      BColor.make(-14671840).toBrush(),
      BColor.make(-256).toBrush(),
      BColor.make(-12566528).toBrush(),
      BColor.make(-8355840).toBrush(),
      BColor.make(-4145152).toBrush(),
      BColor.make(-192).toBrush(),
      BColor.make(-128).toBrush(),
      BColor.make(-64).toBrush(),
      BColor.make(-12566464).toBrush(),
      BColor.make(-16711936).toBrush(),
      BColor.make(-16760832).toBrush(),
      BColor.make(-16744448).toBrush(),
      BColor.make(-16728064).toBrush(),
      BColor.make(-12517568).toBrush(),
      BColor.make(-8323200).toBrush(),
      BColor.make(-4128832).toBrush(),
      BColor.make(-10461088).toBrush(),
      BColor.make(-16711681).toBrush(),
      BColor.make(-16760768).toBrush(),
      BColor.make(-16744320).toBrush(),
      BColor.make(-16727872).toBrush(),
      BColor.make(-12517377).toBrush(),
      BColor.make(-8323073).toBrush(),
      BColor.make(-4128769).toBrush(),
      BColor.make(-8355712).toBrush(),
      BColor.make(-16776961).toBrush(),
      BColor.make(-16777152).toBrush(),
      BColor.make(-16777088).toBrush(),
      BColor.make(-16777024).toBrush(),
      BColor.make(-12566273).toBrush(),
      BColor.make(-8355585).toBrush(),
      BColor.make(-4144897).toBrush(),
      BColor.make(-4144960).toBrush(),
      BColor.make(-8388353).toBrush(),
      BColor.make(-14811072).toBrush(),
      BColor.make(-12844928).toBrush(),
      BColor.make(-10878784).toBrush(),
      BColor.make(-6667777).toBrush(),
      BColor.make(-4488961).toBrush(),
      BColor.make(-2244609).toBrush(),
      BColor.make(-2039584).toBrush(),
      BColor.make(-65281).toBrush(),
      BColor.make(-12582848).toBrush(),
      BColor.make(-8388480).toBrush(),
      BColor.make(-4194112).toBrush(),
      BColor.make(-48897).toBrush(),
      BColor.make(-32513).toBrush(),
      BColor.make(-16129).toBrush(),
      BColor.make(-1).toBrush()
   };

   public void rgbModified(BWidgetEvent parameter) {
      this.invoke(rgbModified, parameter, null);
   }

   public void hsbModified(BWidgetEvent parameter) {
      this.invoke(hsbModified, parameter, null);
   }

   public void xmlModified() {
      this.invoke(xmlModified, null, null);
   }

   public void nameModified() {
      this.invoke(nameModified, null, null);
   }

   public void alphaModified(BWidgetEvent parameter) {
      this.invoke(alphaModified, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BColor openInDialog(BWidget owner) {
      return openInDialog(owner, BColor.NULL);
   }

   public static BColor openInDialog(BWidget owner, BColor color) {
      return openInDialog(owner, text("title"), color);
   }

   public static BColor openInDialog(BWidget owner, String title, BColor color) {
      BColorChooser chooser = new BColorChooser(color);
      return 1 == BDialog.open(owner, title, new BBorderPane(chooser), 3) ? chooser.model.toRGB() : null;
   }

   public BColorChooser() {
      this(BColor.NULL);
   }

   public BColorChooser(BColor c) {
      Object[] obj = this.getColors().getChildren(BBrush.class);

      for (int i = 0; i < obj.length; i++) {
         this.customBrushes.add((BBrush)obj[i]);
      }

      this.model = new ColorModel();
      BSatBrightBox box = new BSatBrightBox(this.model);
      BHueBar bar = new BHueBar(this.model);
      this.swatch = new BBrushSwatch();
      this.swatch.setPreferredSize(48.0, 48.0);
      this.redSlider = new BSlider();
      this.greSlider = new BSlider();
      this.bluSlider = new BSlider();
      BGridPane all = new BGridPane(3);
      all.add(null, new BLabel(text("hue"), BHalign.right));
      all.add(null, this.hueField = new BTextField("", 4));
      all.add(null, new BLabel("°"));
      all.add(null, new BLabel(text("saturation"), BHalign.right));
      all.add(null, this.satField = new BTextField("", 4));
      all.add(null, new BLabel("%"));
      all.add(null, new BLabel(text("brightness"), BHalign.right));
      all.add(null, this.briField = new BTextField("", 4));
      all.add(null, new BLabel("%"));
      all.add(null, new BBorderPane(new BNullWidget(), 4.0, 0.0, 0.0, 0.0));
      all.add(null, new BNullWidget());
      all.add(null, new BNullWidget());
      all.add(null, new BLabel(text("red"), BHalign.right));
      all.add(null, this.redField = new BTextField("", 4));
      all.add(null, new BNullWidget());
      all.add(null, new BLabel(text("green"), BHalign.right));
      all.add(null, this.greField = new BTextField("", 4));
      all.add(null, new BNullWidget());
      all.add(null, new BLabel(text("blue"), BHalign.right));
      all.add(null, this.bluField = new BTextField("", 4));
      all.add(null, new BNullWidget());
      all.add(null, new BBorderPane(new BNullWidget(), 4.0, 0.0, 0.0, 0.0));
      all.add(null, new BNullWidget());
      all.add(null, new BNullWidget());
      all.add(null, new BLabel(text("alpha"), BHalign.right));
      all.add(null, this.alphaField = new BTextField("", 4));
      all.add(null, new BLabel("%"));
      this.hueSlider = new BSlider();
      this.satSlider = new BSlider();
      this.briSlider = new BSlider();
      this.alphaSlider = new BSlider();
      BGridPane xml = new BGridPane(2);
      xml.add(null, new BLabel(text("hex")));
      xml.add(null, this.xmlField = new BTextField("", 10));
      xml.add(null, new BLabel(text("name")));
      xml.add(null, this.nameField = new BTextDropDown("", 10, true));
      BList list = this.nameField.getList();
      list.setRenderer(new BColorChooser.ColorRenderer());
      BColor[] colors = BColor.getConstants();

      for (int i = 0; i < colors.length; i++) {
         if (!colors[i].isNull()) {
            list.addItem(colors[i].toString());
         }
      }

      BToolBar customToolbar = new BToolBar();
      customToolbar.add(null, this.addCommand = new BColorChooser.Add(this));
      customToolbar.add(null, new BColorChooser.Manage(this));
      this.customBrushList = new BBrushList(this.getCustomList());
      this.customBrushList.setController(new BColorChooser.CustomBrushListController());
      BScrollPane scroll = new BScrollPane(this.customBrushList);
      BWidget z = new BBorderPane(scroll, BBorder.inset, BInsets.make(0.0, 0.0, 0.0, 0.0));
      BGridPane gridLists = new BGridPane(1);
      gridLists.setStretchRow(2);
      gridLists.setRowAlign(BValign.fill);
      gridLists.add(null, BBrushList.makeScroll(this.defaultBrushList, new BColorChooser.DefaultBrushListController()));
      gridLists.add(null, customToolbar);
      BEdgePane lists = new BEdgePane();
      lists.setTop(gridLists);
      lists.setCenter(z);
      BGridPane controls = new BGridPane(1);
      controls.setValign(BValign.top);
      controls.setRowGap(10.0);
      controls.add(null, new BBorderPane(this.swatch, BBorder.inset, BInsets.make(0.0, 0.0, 0.0, 0.0)));
      controls.add(null, all);
      controls.add(null, xml);
      controls.add(null, this.nullColor = new BCheckBox(new BColorChooser.NullCommand()));
      BGridPane top = new BGridPane(4);
      top.setColumnGap(20.0);
      top.setRowAlign(BValign.fill);
      top.add(null, lists);
      top.add(null, box);
      top.add(null, bar);
      top.add(null, controls);
      this.setCenter(top);
      this.linkTo(this.redField, BTextField.textModified, rgbModified);
      this.linkTo(this.greField, BTextField.textModified, rgbModified);
      this.linkTo(this.bluField, BTextField.textModified, rgbModified);
      this.linkTo(this.hueField, BTextField.textModified, hsbModified);
      this.linkTo(this.satField, BTextField.textModified, hsbModified);
      this.linkTo(this.briField, BTextField.textModified, hsbModified);
      this.linkTo(this.alphaField, BTextField.textModified, alphaModified);
      this.linkTo(this.xmlField, BTextField.textModified, xmlModified);
      this.linkTo(this.nameField.getEditor(), BTextField.textModified, nameModified);
      this.model.setWithAlpha(c);
      if (c.isNull()) {
         this.model.setAlpha(1.0F);
         this.model.setNull(true);
      }

      this.model.addAgent(this);
      this.colorChanged();
      if (this.customBrushes.size() >= 88) {
         this.addCommand.setEnabled(false);
      }
   }

   private BBrush[] getCustomList() {
      return this.customBrushes.toArray(new BBrush[0]);
   }

   @Override
   public void colorChanged() {
      this.ignoreEvents = true;
      this.swatch.setBrush(this.model.toRGB().toBrush());
      int h = Math.round(this.model.getHue() * 360.0F);
      int s = Math.round(this.model.getSat() * 100.0F);
      int b = Math.round(this.model.getBri() * 100.0F);
      this.hueSlider.setValue(h);
      this.satSlider.setValue(s);
      this.briSlider.setValue(b);
      if (!this.fromHsb) {
         this.hueField.setText(Integer.toString(h));
         this.satField.setText(Integer.toString(s));
         this.briField.setText(Integer.toString(b));
      }

      BColor c = this.model.toRGB();
      int r = c.getRed();
      int g = c.getGreen();
      b = c.getBlue();
      this.redSlider.setValue(r);
      this.greSlider.setValue(g);
      this.bluSlider.setValue(b);
      if (!this.fromRgb) {
         this.redField.setText(Integer.toString(r));
         this.greField.setText(Integer.toString(g));
         this.bluField.setText(Integer.toString(b));
      }

      int a = Math.round(this.model.getAlpha() * 100.0F);
      this.alphaSlider.setValue(a);
      if (!this.fromAlpha) {
         this.alphaField.setText(Integer.toString(a));
      }

      this.nullColor.setSelected(this.model.isNull());
      if (!this.fromXml) {
         String hex = Integer.toHexString(c.getRGB());
         if (hex.length() == 1) {
            hex = "00000000";
         } else if (hex.length() == 7) {
            hex = "0" + hex;
         } else if (hex.length() == 6) {
            hex = "00" + hex;
         }

         this.xmlField.setText("#" + hex);
      }

      if (!this.fromName) {
         String str = c.toString();
         this.nameField.getEditor().getSelection().deselect();
         this.nameField.setText(c.isNull() ? "" : (BColor.getConstant(str) == null ? "" : str));
      }

      this.ignoreEvents = false;
      this.fromHsb = false;
      this.fromRgb = false;
      this.fromXml = false;
      this.fromName = false;
      this.fromAlpha = false;
      this.repaint();
   }

   public void doRgbModified(BWidgetEvent event) {
      if (!this.ignoreEvents) {
         BWidget source = event.getWidget();
         BColor c = this.model.toRGB();
         int red = c.getRed();
         int green = c.getGreen();
         int blue = c.getBlue();

         try {
            if (source == this.redSlider) {
               red = (int)this.redSlider.getValue();
            } else if (source == this.greSlider) {
               green = (int)this.greSlider.getValue();
            } else if (source == this.bluSlider) {
               blue = (int)this.bluSlider.getValue();
            } else if (source == this.redField) {
               red = Integer.parseInt(this.redField.getText());
               this.fromRgb = true;
            } else if (source == this.greField) {
               green = Integer.parseInt(this.greField.getText());
               this.fromRgb = true;
            } else if (source == this.bluField) {
               blue = Integer.parseInt(this.bluField.getText());
               this.fromRgb = true;
            }

            this.model.set(BColor.make(red, green, blue));
         } catch (Exception var8) {
         }
      }
   }

   public void doHsbModified(BWidgetEvent event) {
      if (!this.ignoreEvents) {
         BWidget source = event.getWidget();
         float hue = this.model.getHue();
         float sat = this.model.getSat();
         float bri = this.model.getBri();

         try {
            if (source == this.hueSlider) {
               hue = (float)this.hueSlider.getValue() / 360.0F;
            } else if (source == this.satSlider) {
               sat = (float)this.satSlider.getValue() / 100.0F;
            } else if (source == this.briSlider) {
               bri = (float)this.briSlider.getValue() / 100.0F;
            } else if (source == this.hueField) {
               hue = Integer.parseInt(this.hueField.getText()) / 360.0F;
               this.fromHsb = true;
            } else if (source == this.satField) {
               sat = Integer.parseInt(this.satField.getText()) / 100.0F;
               this.fromHsb = true;
            } else if (source == this.briField) {
               bri = Integer.parseInt(this.briField.getText()) / 100.0F;
               this.fromHsb = true;
            }

            this.model.set(hue, sat, bri);
         } catch (Exception var7) {
         }
      }
   }

   public void doAlphaModified(BWidgetEvent event) {
      if (!this.ignoreEvents) {
         BWidget source = event.getWidget();
         if (source == this.alphaSlider) {
            float alpha = (float)this.alphaSlider.getValue() / 100.0F;
            this.model.setAlpha(alpha);
         } else if (source == this.alphaField) {
            try {
               float alpha = Integer.parseInt(this.alphaField.getText()) / 100.0F;
               this.model.setAlpha(alpha);
            } catch (Exception var4) {
            }
         }
      }
   }

   public void doXmlModified() {
      if (!this.ignoreEvents) {
         try {
            String s = this.xmlField.getText();
            if (!s.startsWith("#")) {
               s = "#" + s;
            }

            BColor c = BColor.make(s);
            this.fromXml = true;
            this.model.setWithAlpha(c);
         } catch (Exception var3) {
         }
      }
   }

   public void doNameModified() {
      if (!this.ignoreEvents) {
         try {
            String s = this.nameField.getText();
            BColor c = BColor.getConstant(s);
            if (c != null) {
               this.fromName = true;
               this.model.setWithAlpha(c);
            }
         } catch (Exception var3) {
         }
      }
   }

   private BOptions getColors() {
      return BOptions.load("colors", BOptions.TYPE);
   }

   private static String text(String s) {
      return lex.getText("colorChooser." + s);
   }

   class Add extends Command {
      public Add(BWidget owner) {
         super(owner, BColorChooser.module, "colorChooser.add");
      }

      public CommandArtifact doInvoke() {
         BBrush b = BColorChooser.this.model.toRGB().toBrush();
         BColorChooser.this.getColors().add(null, b);
         BColorChooser.this.getColors().save();
         BColorChooser.this.customBrushes.add(b);
         BColorChooser.this.customBrushList.setList(BColorChooser.this.getCustomList());
         if (BColorChooser.this.customBrushes.size() >= 88) {
            BColorChooser.this.addCommand.setEnabled(false);
         }

         return null;
      }
   }

   class ColorRenderer extends ListRenderer {
      public void paintItem(Graphics g, Item item) {
         this.paintItemBackground(g, item);
         String s = (String)item.value;
         double h = this.getItemHeight();
         g.setBrush(BColor.getConstant(s).toBrush());
         g.fillRect(2.0, 2.0, h - 4.0, h - 4.0);
         g.setBrush(BColor.black);
         g.strokeRect(2.0, 2.0, h - 4.0, h - 4.0);
         BFont f = Theme.table().getCellFont();
         g.setFont(f);
         g.drawString(s, h + 3.0, f.getAscent() + 2.0);
      }

      public double getPreferredItemWidth(Item item) {
         BFont f = Theme.table().getCellFont();
         return f.width(item.value.toString()) + this.getItemHeight() + 17.0;
      }
   }

   class CustomBrushListController extends BBrushList.Controller {
      @Override
      public void swatchPressed(BBrushSwatch swatch) {
         BBrush b = swatch.getBrush();
         if (b.getPaint() instanceof Solid) {
            BColorChooser.this.model.setWithAlpha(((Solid)b.getPaint()).getColor());
         }
      }
   }

   class DefaultBrushListController extends BBrushList.Controller {
      @Override
      public void swatchPressed(BBrushSwatch swatch) {
         BBrush b = swatch.getBrush();
         if (b.getPaint() instanceof Solid) {
            BColorChooser.this.model.set(((Solid)b.getPaint()).getColor());
         }
      }
   }

   class Manage extends Command {
      public Manage(BWidget owner) {
         super(owner, BColorChooser.module, "colorChooser.manage");
      }

      public CommandArtifact doInvoke() {
         BBrush[] list = BPaletteManager.openInDialog(this.getOwner(), BColorChooser.this.getCustomList());
         if (list != null) {
            BColorChooser.this.customBrushes.clear();
            BOptions colors = BColorChooser.this.getColors();
            colors.removeAll();

            for (int i = 0; i < list.length; i++) {
               BColorChooser.this.customBrushes.add(list[i]);
               colors.add(null, list[i]);
            }

            colors.save();
            BColorChooser.this.customBrushList.setList(list);
            BColorChooser.this.customBrushList.relayout();
            if (BColorChooser.this.customBrushes.size() < 88) {
               BColorChooser.this.addCommand.setEnabled(true);
            }
         }

         return null;
      }
   }

   class NullCommand extends ToggleCommand {
      public NullCommand() {
         super(BColorChooser.this, BColorChooser.text("useNullColor"));
      }

      public CommandArtifact doInvoke() {
         if (!BColorChooser.this.ignoreEvents) {
            BColorChooser.this.model.setNull(this.isSelected());
         }

         return null;
      }
   }
}
