package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NEnrollmentSummary;
import com.tridium.bacnet.services.BacnetComplexAck;
import java.util.ListIterator;
import java.util.Vector;
import javax.baja.bacnet.io.AsnException;

public class GetEnrollmentSummaryAck extends BacnetComplexAck {
   private Vector<NEnrollmentSummary> listOfEnrollmentSummaries;

   public GetEnrollmentSummaryAck() {
      super(4);
      this.listOfEnrollmentSummaries = new Vector<>();
   }

   public GetEnrollmentSummaryAck(Vector<NEnrollmentSummary> listOfEnrollmentSummaries) {
      super(4);
      this.listOfEnrollmentSummaries = listOfEnrollmentSummaries;
   }

   public GetEnrollmentSummaryAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      super(serviceChoice);
      this.listOfEnrollmentSummaries = new Vector<>();
      this.readEncoded(inputStream);
   }

   public void addEnrollmentSummary(NEnrollmentSummary enrollmentSummary) {
      this.listOfEnrollmentSummaries.addElement(enrollmentSummary);
   }

   public ListIterator<NEnrollmentSummary> getEnrollmentSummaries() {
      return this.listOfEnrollmentSummaries.listIterator();
   }

   public Vector<NEnrollmentSummary> getListOfEnrollmentSummaries() {
      return this.listOfEnrollmentSummaries;
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      for (NEnrollmentSummary enrollmentSummary : this.listOfEnrollmentSummaries) {
         enrollmentSummary.writeAsn(out);
      }
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException {
      while (in.peekTag() != -1) {
         NEnrollmentSummary esum = new NEnrollmentSummary();
         esum.readAsn(in);
         this.listOfEnrollmentSummaries.add(esum);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("GetEnrollmentSummaryAck: ");

      for (NEnrollmentSummary enrollmentSummary : this.listOfEnrollmentSummaries) {
         sb.append(enrollmentSummary.toString());
      }

      return sb.toString();
   }
}
