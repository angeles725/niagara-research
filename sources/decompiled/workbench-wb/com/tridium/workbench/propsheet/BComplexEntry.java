package com.tridium.workbench.propsheet;

import com.tridium.ui.theme.Theme;
import com.tridium.workbench.util.WbViewEventWorker;
import javax.baja.agent.AgentInfo;
import javax.baja.agent.AgentList;
import javax.baja.gx.Graphics;
import javax.baja.gx.IRectGeom;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BAction;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIPropertyContainer;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelation;
import javax.baja.sys.BTopic;
import javax.baja.sys.BValue;
import javax.baja.sys.DuplicateSlotException;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BDialog;
import javax.baja.ui.BWidgetShell;
import javax.baja.ui.HyperlinkInfo;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.util.BWsAnnotation;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
public class BComplexEntry extends BPropertyEntry {
   public static final Type TYPE = Sys.loadType(BComplexEntry.class);
   private boolean builtExpansion = false;
   private Object lock = new Object();

   @Override
   public Type getType() {
      return TYPE;
   }

   public BComplexEntry() {
   }

   public BComplexEntry(BFieldEditorSheet sheet, BComplexEntry parent, Property prop, BObject target) {
      super(sheet, parent, prop, target);
   }

   void layoutEntries(int depth) {
      BPropertyEntry[] kids = this.getChildEntries();
      double w = this.getWidth();
      double expanderDim = EXPANDER_DIM;
      double expanderX = 5.0 + depth * INDENT;
      double labelX = expanderX + expanderDim + 10.0;
      double maxWidth = 0.0;
      double maxLabel = 0.0;

      for (int i = 0; i < kids.length; i++) {
         maxLabel = Math.max(maxLabel, (double)kids[i].computeSizes());
      }

      double cx = labelX + maxLabel + 20.0;
      double y = this.computeHeight() + 4.0;

      for (int i = 0; i < kids.length; i++) {
         BPropertyEntry kid = kids[i];
         double ch = Math.max(kid.content.height, kid.label.height) + 4.0;
         kid.expander.x = expanderX;
         kid.expander.y = (ch - expanderDim) / 2.0;
         kid.label.x = labelX;
         kid.label.y = (ch - kid.label.height) / 2.0;
         kid.content.x = cx;
         kid.content.y = 2.0;
         if (kid instanceof BComplexEntry && kid.isExpanded) {
            ((BComplexEntry)kid).layoutEntries(depth + 1);
            kid.setBounds(0.0, y, w, kid.getPreferredHeight());
            y += kid.getPreferredHeight();
            maxWidth = Math.max(maxWidth, kid.getPreferredWidth());
         } else {
            kid.setBounds(0.0, y, w, ch);
            y += kid.getHeight();
            maxWidth = Math.max(maxWidth, kid.content.x + kid.content.width);
         }
      }

      for (int ix = 0; ix < kids.length; ix++) {
         kids[ix].positionsComputed();
      }

      this.setPreferredSize(maxWidth, y);
   }

   @Override
   void computeContentSize() {
      this.content.width = 250.0;
      this.content.height = this.computeHeight();
   }

   @Override
   void paintContent(Graphics g) {
      String summary;
      try {
         summary = this.target.toString(this.sheet.getCurrentContext());
      } catch (Throwable var13) {
         summary = var13.toString();
      }

      double w = font.width(summary);
      g.setBrush(Theme.propertySheetTree().getTextBrush(this));
      if (w > 250.0) {
         IRectGeom oldClip = g.getClipBounds();
         String dots = "...";
         double dotsWidth = font.width(dots);
         g.push();

         try {
            g.clip(this.content.x, oldClip.y(), 250.0 - dotsWidth, oldClip.height());
            g.drawString(summary, this.content.x, this.content.y + textBaseline);
         } finally {
            g.pop();
         }

         g.drawString("...", this.content.x + 250.0 - dotsWidth, this.content.y + textBaseline);
      } else {
         g.drawString(summary, this.content.x, this.content.y + (this.content.height + font.getAscent() - font.getDescent()) / 2.0);
      }

      this.paintChildren(g);
   }

   @Override
   boolean isHyperlinkSupported() {
      return !this.disableHyperlink && this.target.isComponent() && this.getShell() instanceof BWbShell;
   }

   @Override
   void hyperlink(BMouseEvent event) {
      if (!this.disableHyperlink) {
         BWidgetShell shell = this.getShell();
         if (shell instanceof BWbShell) {
            ((BWbShell)shell).hyperlink(new HyperlinkInfo(((BINavNode)this.target).getNavOrd(), event));
         }
      }
   }

   @Override
   boolean isExpandable() {
      return true;
   }

   @Override
   void doExpand() {
      if (!this.isBuiltExpansion()) {
         try {
            this.enterBusy();
            if (this.target instanceof BIPropertyContainer) {
               BIPropertyContainer target = (BIPropertyContainer)this.target;
               target.loadSlots();
               Property[] props = target.getPropertiesArray();

               for (int i = 0; i < props.length; i++) {
                  BValue value = target.get(props[i]);
                  if (value instanceof BComponent) {
                     try {
                        ((BComponent)value).lease();
                     } catch (Exception var18) {
                        continue;
                     }
                  }

                  this.add(target, props[i], value);
               }

               if (target instanceof BComponent) {
                  this.sheet.registerForComponentEvents((BComponent)target);
               }
            } else {
               BComplex target = (BComplex)this.target;
               target.loadSlots();
               Property[] props = target.getPropertiesArray();

               for (int i = 0; i < props.length; i++) {
                  this.add(target, props[i], target.get(props[i]));
               }

               if (target.isComponent()) {
                  this.sheet.registerForComponentEvents(target.asComponent());
               }
            }
         } catch (Throwable var19) {
            BDialog.error(this, BDialog.TITLE_ERROR, "Cannot expand", var19);
         } finally {
            synchronized (this.lock) {
               this.builtExpansion = true;
            }

            this.exitBusy();
         }
      }
   }

   void add(BComplex target, Property prop, BObject kid) {
      if (!Flags.isHidden(target, prop)) {
         if (!(kid instanceof BRelation)) {
            if (!(kid instanceof BWsAnnotation)) {
               if (!(kid instanceof BAction)) {
                  if (!(kid instanceof BTopic)) {
                     String name = prop.getName();

                     try {
                        BPropertyEntry entry = this.makeEntry(prop, kid);
                        String newName = "e_" + name;
                        Property p = this.getProperty(newName);
                        if (p == null) {
                           this.add(newName, entry, null);
                           if (this.getShell() != null) {
                              entry.init();
                           }
                        } else {
                           BPropertyEntry currentEntry = (BPropertyEntry)this.get(p);
                           currentEntry.reloadIcon();
                           currentEntry.repaint();
                        }
                     } catch (DuplicateSlotException var9) {
                        System.out.println("Duplicate slot exception " + name + " -> " + kid.getType());
                        var9.printStackTrace();
                     } catch (Throwable var10) {
                        System.out.println("Cannot add entry " + name + " -> " + kid.getType());
                        var10.printStackTrace();
                     }
                  }
               }
            }
         }
      }
   }

   void add(BIPropertyContainer target, Property prop, BObject kid) {
      if ((target.getFlags(prop) & 4) == 0) {
         if (!(kid instanceof BRelation)) {
            if (!(kid instanceof BWsAnnotation)) {
               if (!(kid instanceof BAction)) {
                  if (!(kid instanceof BTopic)) {
                     String name = prop.getName();

                     try {
                        BPropertyEntry entry = this.makeEntry(prop, kid);
                        String newName = "e_" + name;
                        Property p = this.getProperty(newName);
                        if (p == null) {
                           this.add(newName, entry, null);
                           if (this.getShell() != null) {
                              entry.init();
                           }
                        } else {
                           BPropertyEntry currentEntry = (BPropertyEntry)this.get(p);
                           currentEntry.reloadIcon();
                           currentEntry.repaint();
                        }
                     } catch (DuplicateSlotException var9) {
                        System.out.println("Duplicate slot exception " + name + " -> " + kid.getType());
                        var9.printStackTrace();
                     } catch (Throwable var10) {
                        System.out.println("Cannot add entry " + name + " -> " + kid.getType());
                        var10.printStackTrace();
                     }
                  }
               }
            }
         }
      }
   }

   BPropertyEntry makeEntry(Property prop, BObject kid) {
      AgentInfo editor = null;
      if (this.target instanceof BIPropertyContainer) {
         editor = this.getEditorType((BIPropertyContainer)this.target, prop, kid);
      } else {
         editor = this.getEditorType((BComplex)this.target, prop, kid);
      }

      boolean isEditorPropSheet = editor.getAgentType().is(BPropertySheetFE.TYPE);
      BPropertyEntry entry;
      if (!kid.isSimple() && isEditorPropSheet) {
         entry = new BComplexEntry(this.sheet, this, prop, kid);
      } else {
         entry = new BAtomicEntry(this.sheet, this, prop, kid, editor);
      }

      entry.disableHyperlink(this.disableHyperlink);
      return entry;
   }

   AgentInfo getEditorType(BComplex complex, Property prop, BObject kid) {
      BFacets facets = complex.getSlotFacets(prop);
      return this.getEditorType(kid, facets);
   }

   AgentInfo getEditorType(BIPropertyContainer container, Property prop, BObject kid) {
      BFacets facets = container.getSlotFacets(prop);
      return this.getEditorType(kid, facets);
   }

   private AgentInfo getEditorType(BObject kid, BFacets facets) {
      if (facets != null) {
         String explicit = facets.gets("fieldEditor", null);
         if (explicit != null) {
            return Sys.getRegistry().getType(explicit).getAgentInfo();
         }
      }

      BWbShell shell = null;

      try {
         shell = BWbShell.getWbShell(this.getShell());
      } catch (Exception var5) {
      }

      AgentList agents = kid.getAgents().filter(BWbFieldEditor.getAgentFilter(shell));
      return agents.getDefault();
   }

   void rebuildEntry(BPropertyEntry oldEntry, BObject newValue) {
      if (newValue.isComponent()) {
         WbViewEventWorker.getInstance()
            .registerForComponentEventsLater(this.sheet.getParentWbComponentView(), newValue.asComponent(), this.sheet.subscribeDepth);
      }

      BPropertyEntry newEntry = this.makeEntry(oldEntry.property, newValue);
      this.set(oldEntry.getPropertyInParent(), newEntry);
      if (this.getShell() != null) {
         newEntry.init();
      }
   }

   @Override
   void doCollapse() {
      if (this.sheet.unsubscribeOnCollapse && this.isBuiltExpansion()) {
         try {
            this.enterBusy();
            BPropertyEntry[] kids = this.getChildEntries();

            for (int i = 0; i < kids.length; i++) {
               if (kids[i].isExpandable() && kids[i].isExpanded) {
                  kids[i].collapse();
               }

               this.remove(kids[i]);
            }

            if (this.target.isComponent()) {
               this.sheet.unregisterForComponentEvents(this.target.asComponent());
            }
         } catch (Exception var14) {
            BDialog.error(this, BDialog.TITLE_ERROR, "Cannot collapse", var14);
         } finally {
            synchronized (this.lock) {
               this.builtExpansion = false;
            }

            this.exitBusy();
         }
      }
   }

   @Override
   public void init() {
      BPropertyEntry[] kids = this.getChildEntries();

      for (int i = 0; i < kids.length; i++) {
         kids[i].init();
      }
   }

   @Override
   void doUpdate(BObject target) {
      this.target = target;
      if (this.isBuiltExpansion()) {
         if (target instanceof BIPropertyContainer) {
            BIPropertyContainer targetContainer = (BIPropertyContainer)target;
            Property[] props = targetContainer.getPropertiesArray();

            for (int i = 0; i < props.length; i++) {
               BPropertyEntry kid = (BPropertyEntry)this.get("e_" + props[i].getName());
               if (kid != null) {
                  kid.update(targetContainer.get(props[i]));
               }
            }
         } else {
            BComplex targetComplex = target.asComplex();
            Property[] props = targetComplex.getPropertiesArray();

            for (int ix = 0; ix < props.length; ix++) {
               BPropertyEntry kid = (BPropertyEntry)this.get("e_" + props[ix].getName());
               if (kid != null) {
                  kid.update(targetComplex.get(props[ix]));
               }
            }
         }

         this.repaint();
      }
   }

   void reorder() {
      if (this.isBuiltExpansion()) {
         BPropertyEntry[] kids = this.getChildEntries();
         if (kids != null && kids.length != 0) {
            Property[] newOrder = new Property[kids.length];
            Property[] props = null;
            if (this.target instanceof BIPropertyContainer) {
               props = ((BIPropertyContainer)this.target).getPropertiesArray();
            } else {
               props = this.target.asComplex().getPropertiesArray();
            }

            int n = 0;

            for (int i = 0; i < props.length; i++) {
               String name = "e_" + props[i].getName();

               for (int j = 0; j < kids.length; j++) {
                  if (kids[j].getName().equals(name)) {
                     newOrder[n++] = kids[j].getPropertyInParent();
                     break;
                  }
               }
            }

            if (n != newOrder.length) {
               System.out.println("BPropertyEntry.reorder problems!!!");
            } else {
               this.reorder(newOrder);
            }
         }
      }
   }

   boolean isBuiltExpansion() {
      synchronized (this.lock) {
         return this.builtExpansion;
      }
   }
}
