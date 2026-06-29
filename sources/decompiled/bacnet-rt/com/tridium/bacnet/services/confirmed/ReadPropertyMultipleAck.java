package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NReadAccessResult;
import com.tridium.bacnet.services.BacnetComplexAck;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Vector;
import javax.baja.bacnet.io.AsnException;

public class ReadPropertyMultipleAck extends BacnetComplexAck {
   private Vector listOfReadAccessResults;

   public ReadPropertyMultipleAck() {
      super(14);
      this.listOfReadAccessResults = new Vector();
   }

   public ReadPropertyMultipleAck(Vector listOfReadAccessResults) {
      super(14);
      this.listOfReadAccessResults = listOfReadAccessResults;
   }

   public ReadPropertyMultipleAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      super(serviceChoice);
      this.listOfReadAccessResults = new Vector();
      this.readEncoded(inputStream);
   }

   public void addReadAccessResult(NReadAccessResult readAccessResult) {
      this.listOfReadAccessResults.addElement(readAccessResult);
   }

   public ListIterator getReadAccessResults() {
      return this.listOfReadAccessResults.listIterator();
   }

   public Vector getListOfReadAccessResults() {
      return this.listOfReadAccessResults;
   }

   public Iterator getResults() {
      return new ReadPropertyMultipleAck.ResultIterator(this.listOfReadAccessResults.iterator());
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      for (NReadAccessResult readAccessResult : this.listOfReadAccessResults) {
         readAccessResult.writeAsn(outputStream);
      }
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      while (inputStream.peekTag() != -1) {
         NReadAccessResult rar = new NReadAccessResult();
         rar.readAsn(inputStream);
         this.listOfReadAccessResults.addElement(rar);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("ReadPropertyMultipleAck: ");

      for (NReadAccessResult readAccessResult : this.listOfReadAccessResults) {
         sb.append(readAccessResult.toString());
      }

      return sb.toString();
   }

   static class ResultIterator implements Iterator {
      Iterator arIt = null;
      Iterator prIt = null;

      ResultIterator(Iterator it) {
         this.arIt = it;
      }

      @Override
      public boolean hasNext() {
         if (this.arIt == null) {
            return false;
         } else if (this.prIt != null && this.prIt.hasNext()) {
            return true;
         } else {
            while (this.arIt.hasNext()) {
               this.prIt = ((NReadAccessResult)this.arIt.next()).getResults();
               if (this.prIt.hasNext()) {
                  return true;
               }
            }

            return false;
         }
      }

      @Override
      public Object next() {
         if (this.arIt == null) {
            throw new NoSuchElementException();
         } else if (this.prIt != null && this.prIt.hasNext()) {
            return this.prIt.next();
         } else if (this.arIt.hasNext()) {
            this.prIt = ((NReadAccessResult)this.arIt.next()).getResults();
            return this.prIt.hasNext() && this.prIt != null ? this.prIt.next() : null;
         } else {
            throw new NoSuchElementException();
         }
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }
   }
}
