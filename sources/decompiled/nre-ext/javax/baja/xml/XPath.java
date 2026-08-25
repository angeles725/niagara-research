package javax.baja.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class XPath {
   private static final Logger logger = Logger.getLogger(XPath.class.getName());
   private final List<XPathElem> pathElements;
   private boolean matchChildren;

   public XPath(XPath path) {
      this.pathElements = new ArrayList<>(path.pathElements);
      this.setMatchChildren();
   }

   public XPath(List<XPathElem> pathElements) {
      if (pathElements.size() < 1) {
         throw new IllegalArgumentException("XPath must have one or more XPathElems");
      }

      this.pathElements = new ArrayList<>(pathElements);
      this.setMatchChildren();
   }

   public XPath(XPathElem pathElement, XPathElem... pathElements) {
      this.pathElements = new ArrayList<>(pathElements.length + 1);
      this.pathElements.add(pathElement);
      this.pathElements.addAll(Arrays.asList(pathElements));
      this.setMatchChildren();
   }

   private void setMatchChildren() {
      for (XPathElem pathElem : this.pathElements) {
         if (pathElem.isMatchChildren()) {
            this.matchChildren = true;
            break;
         }
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         XPath xPath = (XPath)o;
         return this.pathElements != null ? this.pathElements.equals(xPath.pathElements) : xPath.pathElements == null;
      } else {
         return false;
      }
   }

   public List<XPathElem> getPathElements() {
      return this.pathElements;
   }

   public XPathMatcher getMatcher(XElemLocation location) {
      return this.matchChildren ? new XPath.DynamicXPathMatcher(this, location) : new XPath.SimpleXPathMatcher(this, location);
   }

   public XPathMatcher getMatcher(List<XElem> locationElements) {
      return this.getMatcher(new XElemLocation(locationElements));
   }

   public XPathMatcher getMatcher() {
      return this.getMatcher(new XElemLocation());
   }

   public int size() {
      return this.pathElements.size();
   }

   @Override
   public int hashCode() {
      return this.pathElements != null ? this.pathElements.hashCode() : 0;
   }

   @Override
   public String toString() {
      return "XPath{pathElements=" + this.pathElements + '}';
   }

   private abstract static class AbstractXPathMatcher implements XPathMatcher {
      protected final XPath xPath;
      protected int numComparedElems;
      protected XElemLocation location;

      protected AbstractXPathMatcher(XPath xPath, XElemLocation location) {
         this.xPath = xPath;
         this.location = location;
         location.addElementRemovalListener(this::elementRemoved);
      }

      protected void elementRemoved(XElemLocation location) {
         if (location.size() < this.numComparedElems) {
            this.numComparedElems = location.size();
         }
      }

      @Override
      public XElemLocation getLocation() {
         return this.location;
      }
   }

   private static class DynamicXPathMatcher extends XPath.AbstractXPathMatcher {
      private static final byte UNKNOWN = 0;
      private static final byte MATCH = 1;
      private static final byte MISMATCH = 2;
      private static final int[] EMPTY_INT_ARRAY = new int[0];
      private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
      private final int[] lastCol;
      private byte[] matchMatrix = EMPTY_BYTE_ARRAY;
      private int[] colStack = EMPTY_INT_ARRAY;
      private int[] rowStack = EMPTY_INT_ARRAY;
      private int stackSize = 0;

      private DynamicXPathMatcher(XPath xPath, XElemLocation location) {
         super(xPath, location);
         this.matchMatrix = new byte[xPath.size() * location.size()];
         this.lastCol = new int[xPath.size()];
      }

      private void push(int rows, int cols, int row, int col) {
         if (row < rows) {
            if (this.lastCol[row] < col) {
               this.lastCol[row] = col;
            }

            if (col < cols) {
               this.rowStack[this.stackSize] = row;
               this.colStack[this.stackSize] = col;
               this.stackSize++;
            }
         }
      }

      private int popRow() {
         return this.rowStack[--this.stackSize];
      }

      private int popCol() {
         return this.colStack[this.stackSize];
      }

      @Override
      public boolean matches() {
         if (this.xPath.size() > this.location.size()) {
            return false;
         }

         if (this.location.size() > this.numComparedElems) {
            this.updateMatrix();
         }

         return this.matchMatrix[(this.location.size() - 1) * this.xPath.size() + this.xPath.size() - 1] == 1;
      }

      protected void updateMatrix() {
         int rows = this.xPath.size();
         int cols = this.location.size();
         this.initMatrix();

         for (int i = 0; i < rows; i++) {
            if (this.lastCol[i] >= i) {
               this.push(rows, cols, i, Math.min(this.lastCol[i], this.numComparedElems));
            }
         }

         while (this.stackSize > 0) {
            int row = this.popRow();
            int col = this.popCol();
            if (this.matchMatrix[row + col * rows] == 0) {
               boolean match = this.xPath.pathElements.get(row).matches(this.location.get(col));
               this.matchMatrix[row + col * rows] = (byte)(match ? 1 : 2);
               if (match) {
                  this.push(rows, cols, row + 1, col + 1);
               }

               if (this.xPath.pathElements.get(row).isMatchChildren()) {
                  this.push(rows, cols, row, col + 1);
               }
            }
         }

         this.numComparedElems = cols;
      }

      private void initMatrix() {
         if (this.rowStack.length < this.xPath.size() + this.location.size()) {
            this.rowStack = new int[this.xPath.size() + this.location.size()];
            this.colStack = new int[this.xPath.size() + this.location.size()];
         }

         if (this.matchMatrix.length < this.location.size() * this.xPath.size()) {
            byte[] newMatrix = new byte[this.location.size() * this.xPath.size()];
            System.arraycopy(this.matchMatrix, 0, newMatrix, 0, this.numComparedElems * this.xPath.size());
            this.matchMatrix = newMatrix;
         } else if (this.location.size() > this.numComparedElems) {
            Arrays.fill(this.matchMatrix, this.numComparedElems * this.xPath.size(), this.location.size() * this.xPath.size(), (byte)0);
         }
      }
   }

   private static class SimpleXPathMatcher extends XPath.AbstractXPathMatcher {
      private boolean[] match;

      private SimpleXPathMatcher(XPath xPath, XElemLocation location) {
         super(xPath, location);
         this.match = new boolean[xPath.size()];
      }

      @Override
      public boolean matches() {
         if (this.location.size() > this.xPath.size()) {
            return false;
         }

         for (int i = this.numComparedElems; i < this.location.size(); i++) {
            if (i == 0) {
               this.match[0] = this.xPath.pathElements.get(0).matches(this.location.get(0));
            } else {
               this.match[i] = this.match[i - 1] && this.xPath.pathElements.get(i).matches(this.location.get(i));
            }
         }

         this.numComparedElems = this.location.size();
         return this.location.size() == this.xPath.size() && this.match[this.xPath.size() - 1];
      }
   }
}
