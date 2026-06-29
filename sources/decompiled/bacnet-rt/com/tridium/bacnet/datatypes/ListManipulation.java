package com.tridium.bacnet.datatypes;

import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.sys.BComplex;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.util.Lexicon;

public class ListManipulation implements Runnable {
   private static final Logger logger = Logger.getLogger("bacnet.client");
   private BBacnetObject object;
   private Property property;
   private BValue listElement;
   private boolean add;
   private static final Lexicon lex = Lexicon.make("bacnet");

   public ListManipulation(BBacnetObject o, Property p, BValue v, boolean add) {
      this.object = o;
      this.property = p;
      this.listElement = v;
      this.add = add;
   }

   protected boolean checkEquals(BObject o, BValue v) {
      return o.equivalent(v);
   }

   @Override
   public void run() {
      if (this.object.get(this.property).getType().is(BBacnetListOf.TYPE)) {
         BBacnetListOf list = (BBacnetListOf)this.object.get(this.property);
         if (this.add) {
            boolean added = false;
            if (this.object.device().isServiceSupported("addListElement")) {
               try {
                  this.object.addListElement(this.property, this.listElement);
                  added = true;
               } catch (BacnetException var7) {
                  logger.warning(
                     "Unable to add list element "
                        + this.listElement
                        + " in property "
                        + this.property
                        + " of "
                        + this.object
                        + " using AddListElement:"
                        + var7
                  );
               }
            }

            if (!added) {
               try {
                  if (!this.object.device().isServiceSupported("writeProperty")) {
                     throw new UnsupportedOperationException(lex.getText("serviceNotSupported.writeProperty"));
                  }

                  this.object.readProperty(this.property);
                  SlotCursor<Property> c = list.getProperties();
                  boolean toAdd = true;

                  while (c.next()) {
                     if (c.get().getType().is(list.getListType()) && this.checkEquals(c.get(), this.listElement)) {
                        toAdd = false;
                        break;
                     }
                  }

                  if (toAdd) {
                     list.add(null, this.listElement);
                     this.object.writeProperty(this.property);
                  }
               } catch (Exception var9) {
                  logger.warning(
                     "Unable to add list element " + this.listElement + " in property " + this.property + " of " + this.object + " using WriteProperty:" + var9
                  );
               }
            }
         } else {
            boolean removed = false;
            if (this.object.device().isServiceSupported("removeListElement")) {
               try {
                  this.object.removeListElement(this.property, this.listElement);
                  removed = true;
               } catch (BacnetException var6) {
                  logger.warning(
                     "Unable to remove list element "
                        + this.listElement
                        + " in property "
                        + this.property
                        + " of "
                        + this.object
                        + " using RemoveListElement:"
                        + var6
                  );
               }
            }

            if (!removed) {
               try {
                  if (!this.object.device().isServiceSupported("writeProperty")) {
                     throw new UnsupportedOperationException(lex.getText("serviceNotSupported.writeProperty"));
                  }

                  this.object.readProperty(this.property);
                  Object[] elements = list.getChildren(list.getListType().getTypeClass());
                  boolean found = false;

                  for (int i = 0; i < elements.length; i++) {
                     if (this.checkEquals((BObject)elements[i], this.listElement)) {
                        found = true;
                        list.remove((BComplex)elements[i]);
                        break;
                     }
                  }

                  if (found) {
                     this.object.writeProperty(this.property);
                  }
               } catch (Exception var8) {
                  logger.warning(
                     "Unable to remove list element "
                        + this.listElement
                        + " in property "
                        + this.property
                        + " of "
                        + this.object
                        + " using WriteProperty:"
                        + var8
                  );
               }
            }
         }
      }
   }
}
