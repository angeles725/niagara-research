package com.tridium.template.ui;

import com.tridium.template.manifest.TemplateManifest;
import javax.baja.driver.BDevice;
import javax.baja.driver.BDeviceFolder;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.SlotPath;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.Property;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.util.UiLexicon;
import javax.baja.workbench.nav.tree.BNavTree;
import javax.baja.workbench.nav.tree.NavTreeNode;

public final class MarkAsOptionalCommand extends Command {
   private final TemplateManifest manifest;

   public MarkAsOptionalCommand(BWidget owner, TemplateManifest manifest) {
      super(owner, UiLexicon.bajaui(), "commands.markAsOptional");
      this.manifest = manifest;
   }

   public CommandArtifact doInvoke() {
      if (this.getOwner().getType().is(BNavTree.TYPE)) {
         BNavTree tree = (BNavTree)this.getOwner();
         ApplicationNavTreeModel model = (ApplicationNavTreeModel)tree.getModel();
         var thisNode = (NavTreeNode & NavTreeNode)model.getSelection().getNode();
         if (thisNode instanceof ExcludedComponentNavTreeNode || thisNode instanceof ExcludedDescendantNavTreeNode) {
            return null;
         }

         if (!this.nodeCanBeOptional(thisNode)) {
            return null;
         }

         if (this.isNodeMarkedOptional(thisNode)) {
            this.markNodeNotOptional(model, thisNode);
         } else {
            this.markNodeOptional(model, thisNode);
         }
      }

      return null;
   }

   public static boolean canBeAnOptionalComponent(BComponent component, TemplateManifest manifest) {
      Property propertyInParent = component.getPropertyInParent();
      if (propertyInParent != null && !propertyInParent.isFrozen()) {
         for (BComplex parent = component.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof BComponent) {
               OrdQuery[] queries = ((BComponent)parent).getNavOrd().parse();

               for (OrdQuery query : queries) {
                  if (query instanceof SlotPath && manifest.optional.contains(BOrd.make(query))) {
                     return false;
                  }
               }
            }
         }

         return isValidOptionalComponentType(component);
      } else {
         return false;
      }
   }

   private static boolean isValidOptionalComponentType(BComponent component) {
      return component.getType().is(BDevice.TYPE) || component.getType().is(BDeviceFolder.TYPE);
   }

   private boolean nodeCanBeOptional(NavTreeNode node) {
      return node.getSubject() instanceof BComponent && canBeAnOptionalComponent((BComponent)node.getSubject(), this.manifest);
   }

   private boolean isNodeMarkedOptional(NavTreeNode node) {
      return node.getSubject() instanceof BComponent && this.manifest.optional.contains(((BComponent)node.getSubject()).getSlotPathOrd());
   }

   private void markNodeOptional(ApplicationNavTreeModel model, NavTreeNode thisNode) {
      this.markChildNodesNotOptional(model, thisNode);
      var parentNode = (NavTreeNode & NavTreeNode)thisNode.getParent();
      this.manifest.optional.add(((BComponent)thisNode.getSubject()).getSlotPathOrd());
      parentNode.replaceChild(thisNode, new OptionalComponentNavTreeNode(model, parentNode, thisNode.getNavNode()));
   }

   private void markChildNodesNotOptional(ApplicationNavTreeModel model, NavTreeNode thisNode) {
      for (NavTreeNode childNode : thisNode.getChildren()) {
         if (this.isNodeMarkedOptional(childNode)) {
            this.markNodeNotOptional(model, childNode);
         } else {
            this.markChildNodesNotOptional(model, childNode);
         }
      }
   }

   private void markNodeNotOptional(ApplicationNavTreeModel model, NavTreeNode thisNode) {
      var parentNode = (NavTreeNode & NavTreeNode)thisNode.getParent();
      this.manifest.optional.remove(((BComponent)thisNode.getSubject()).getSlotPathOrd());
      parentNode.replaceChild(thisNode, new IncludedComponentNavTreeNode(model, parentNode, thisNode.getNavNode()));
   }
}
