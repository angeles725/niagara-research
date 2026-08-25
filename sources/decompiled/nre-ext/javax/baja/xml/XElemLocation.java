package javax.baja.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class XElemLocation {
   private List<XElem> elements;
   private List<Consumer<XElemLocation>> removalListeners = null;

   public XElemLocation() {
      this.elements = new ArrayList<>();
   }

   public XElemLocation(List<XElem> elements) {
      this.elements = new ArrayList<>(elements);
   }

   public XElemLocation(XElem... elements) {
      this.elements = new ArrayList<>();
      this.elements.addAll(Arrays.asList(elements));
   }

   public void addElement(XElem element) {
      this.elements.add(element);
   }

   public void removeElement() {
      if (this.elements.size() == 0) {
         throw new IllegalStateException("attempted to remove an XElem from an empty XElemLocation");
      }

      this.elements.remove(this.elements.size() - 1);
      this.callRemovalListeners();
   }

   private void callRemovalListeners() {
      if (this.removalListeners != null) {
         this.removalListeners.forEach(listener -> listener.accept(this));
      }
   }

   public int size() {
      return this.elements.size();
   }

   public XElem get(int index) {
      return this.elements.get(index);
   }

   public void clear() {
      while (this.size() > 0) {
         this.removeElement();
      }
   }

   public void addElementRemovalListener(Consumer<XElemLocation> listener) {
      if (this.removalListeners == null) {
         this.removalListeners = new ArrayList<>();
      }

      this.removalListeners.add(listener);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         XElemLocation that = (XElemLocation)o;
         return this.elements != null ? this.elements.equals(that.elements) : that.elements == null;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.elements != null ? this.elements.hashCode() : 0;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("XElemLocation{");
      sb.append("elements=").append(this.elements);
      sb.append('}');
      return sb.toString();
   }
}
