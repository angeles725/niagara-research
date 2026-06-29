package javax.baja.lonworks.datatypes;

import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "entry0",
      type = "BAddressEntry",
      defaultValue = "BAddressEntry.DEFAULT"
   ), @NiagaraProperty(
      name = "entry1",
      type = "BAddressEntry",
      defaultValue = "BAddressEntry.DEFAULT"
   ), @NiagaraProperty(
      name = "entry2",
      type = "BAddressEntry",
      defaultValue = "BAddressEntry.DEFAULT"
   ), @NiagaraProperty(
      name = "entry3",
      type = "BAddressEntry",
      defaultValue = "BAddressEntry.DEFAULT"
   ), @NiagaraProperty(
      name = "entry4",
      type = "BAddressEntry",
      defaultValue = "BAddressEntry.DEFAULT"
   ), @NiagaraProperty(
      name = "entry5",
      type = "BAddressEntry",
      defaultValue = "BAddressEntry.DEFAULT"
   ), @NiagaraProperty(
      name = "entry6",
      type = "BAddressEntry",
      defaultValue = "BAddressEntry.DEFAULT"
   ), @NiagaraProperty(
      name = "entry7",
      type = "BAddressEntry",
      defaultValue = "BAddressEntry.DEFAULT"
   ), @NiagaraProperty(
      name = "entry8",
      type = "BAddressEntry",
      defaultValue = "BAddressEntry.DEFAULT"
   ), @NiagaraProperty(
      name = "entry9",
      type = "BAddressEntry",
      defaultValue = "BAddressEntry.DEFAULT"
   ), @NiagaraProperty(
      name = "entry10",
      type = "BAddressEntry",
      defaultValue = "BAddressEntry.DEFAULT"
   ), @NiagaraProperty(
      name = "entry11",
      type = "BAddressEntry",
      defaultValue = "BAddressEntry.DEFAULT"
   ), @NiagaraProperty(
      name = "entry12",
      type = "BAddressEntry",
      defaultValue = "BAddressEntry.DEFAULT"
   ), @NiagaraProperty(
      name = "entry13",
      type = "BAddressEntry",
      defaultValue = "BAddressEntry.DEFAULT"
   ), @NiagaraProperty(
      name = "entry14",
      type = "BAddressEntry",
      defaultValue = "BAddressEntry.DEFAULT"
   )})
public class BAddressTable extends BStruct {
   public static final Property entry0 = newProperty(0, BAddressEntry.DEFAULT, null);
   public static final Property entry1 = newProperty(0, BAddressEntry.DEFAULT, null);
   public static final Property entry2 = newProperty(0, BAddressEntry.DEFAULT, null);
   public static final Property entry3 = newProperty(0, BAddressEntry.DEFAULT, null);
   public static final Property entry4 = newProperty(0, BAddressEntry.DEFAULT, null);
   public static final Property entry5 = newProperty(0, BAddressEntry.DEFAULT, null);
   public static final Property entry6 = newProperty(0, BAddressEntry.DEFAULT, null);
   public static final Property entry7 = newProperty(0, BAddressEntry.DEFAULT, null);
   public static final Property entry8 = newProperty(0, BAddressEntry.DEFAULT, null);
   public static final Property entry9 = newProperty(0, BAddressEntry.DEFAULT, null);
   public static final Property entry10 = newProperty(0, BAddressEntry.DEFAULT, null);
   public static final Property entry11 = newProperty(0, BAddressEntry.DEFAULT, null);
   public static final Property entry12 = newProperty(0, BAddressEntry.DEFAULT, null);
   public static final Property entry13 = newProperty(0, BAddressEntry.DEFAULT, null);
   public static final Property entry14 = newProperty(0, BAddressEntry.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BAddressTable.class);

   public BAddressEntry getEntry0() {
      return (BAddressEntry)this.get(entry0);
   }

   public void setEntry0(BAddressEntry v) {
      this.set(entry0, v, null);
   }

   public BAddressEntry getEntry1() {
      return (BAddressEntry)this.get(entry1);
   }

   public void setEntry1(BAddressEntry v) {
      this.set(entry1, v, null);
   }

   public BAddressEntry getEntry2() {
      return (BAddressEntry)this.get(entry2);
   }

   public void setEntry2(BAddressEntry v) {
      this.set(entry2, v, null);
   }

   public BAddressEntry getEntry3() {
      return (BAddressEntry)this.get(entry3);
   }

   public void setEntry3(BAddressEntry v) {
      this.set(entry3, v, null);
   }

   public BAddressEntry getEntry4() {
      return (BAddressEntry)this.get(entry4);
   }

   public void setEntry4(BAddressEntry v) {
      this.set(entry4, v, null);
   }

   public BAddressEntry getEntry5() {
      return (BAddressEntry)this.get(entry5);
   }

   public void setEntry5(BAddressEntry v) {
      this.set(entry5, v, null);
   }

   public BAddressEntry getEntry6() {
      return (BAddressEntry)this.get(entry6);
   }

   public void setEntry6(BAddressEntry v) {
      this.set(entry6, v, null);
   }

   public BAddressEntry getEntry7() {
      return (BAddressEntry)this.get(entry7);
   }

   public void setEntry7(BAddressEntry v) {
      this.set(entry7, v, null);
   }

   public BAddressEntry getEntry8() {
      return (BAddressEntry)this.get(entry8);
   }

   public void setEntry8(BAddressEntry v) {
      this.set(entry8, v, null);
   }

   public BAddressEntry getEntry9() {
      return (BAddressEntry)this.get(entry9);
   }

   public void setEntry9(BAddressEntry v) {
      this.set(entry9, v, null);
   }

   public BAddressEntry getEntry10() {
      return (BAddressEntry)this.get(entry10);
   }

   public void setEntry10(BAddressEntry v) {
      this.set(entry10, v, null);
   }

   public BAddressEntry getEntry11() {
      return (BAddressEntry)this.get(entry11);
   }

   public void setEntry11(BAddressEntry v) {
      this.set(entry11, v, null);
   }

   public BAddressEntry getEntry12() {
      return (BAddressEntry)this.get(entry12);
   }

   public void setEntry12(BAddressEntry v) {
      this.set(entry12, v, null);
   }

   public BAddressEntry getEntry13() {
      return (BAddressEntry)this.get(entry13);
   }

   public void setEntry13(BAddressEntry v) {
      this.set(entry13, v, null);
   }

   public BAddressEntry getEntry14() {
      return (BAddressEntry)this.get(entry14);
   }

   public void setEntry14(BAddressEntry v) {
      this.set(entry14, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void clearTable() {
      this.setEntry0(BAddressEntry.DEFAULT);
      this.setEntry1(BAddressEntry.DEFAULT);
      this.setEntry2(BAddressEntry.DEFAULT);
      this.setEntry3(BAddressEntry.DEFAULT);
      this.setEntry4(BAddressEntry.DEFAULT);
      this.setEntry5(BAddressEntry.DEFAULT);
      this.setEntry6(BAddressEntry.DEFAULT);
      this.setEntry7(BAddressEntry.DEFAULT);
      this.setEntry8(BAddressEntry.DEFAULT);
      this.setEntry9(BAddressEntry.DEFAULT);
      this.setEntry10(BAddressEntry.DEFAULT);
      this.setEntry11(BAddressEntry.DEFAULT);
      this.setEntry12(BAddressEntry.DEFAULT);
      this.setEntry13(BAddressEntry.DEFAULT);
      this.setEntry14(BAddressEntry.DEFAULT);
   }

   public BAddressEntry getAddressEntry(int index) {
      switch (index) {
         case 0:
            return this.getEntry0();
         case 1:
            return this.getEntry1();
         case 2:
            return this.getEntry2();
         case 3:
            return this.getEntry3();
         case 4:
            return this.getEntry4();
         case 5:
            return this.getEntry5();
         case 6:
            return this.getEntry6();
         case 7:
            return this.getEntry7();
         case 8:
            return this.getEntry8();
         case 9:
            return this.getEntry9();
         case 10:
            return this.getEntry10();
         case 11:
            return this.getEntry11();
         case 12:
            return this.getEntry12();
         case 13:
            return this.getEntry13();
         case 14:
            return this.getEntry14();
         default:
            return BAddressEntry.DEFAULT;
      }
   }

   public void setAddressEntry(int index, BIAddressEntry ie) {
      this.setAddressEntry(index, ie, null);
   }

   public void setAddressEntry(int index, BIAddressEntry ie, Context c) {
      BAddressEntry e = BAddressEntry.make(ie);
      switch (index) {
         case 0:
            this.set(entry0, e, c);
            break;
         case 1:
            this.set(entry1, e, c);
            break;
         case 2:
            this.set(entry2, e, c);
            break;
         case 3:
            this.set(entry3, e, c);
            break;
         case 4:
            this.set(entry4, e, c);
            break;
         case 5:
            this.set(entry5, e, c);
            break;
         case 6:
            this.set(entry6, e, c);
            break;
         case 7:
            this.set(entry7, e, c);
            break;
         case 8:
            this.set(entry8, e, c);
            break;
         case 9:
            this.set(entry9, e, c);
            break;
         case 10:
            this.set(entry10, e, c);
            break;
         case 11:
            this.set(entry11, e, c);
            break;
         case 12:
            this.set(entry12, e, c);
            break;
         case 13:
            this.set(entry13, e, c);
            break;
         case 14:
            this.set(entry14, e, c);
      }
   }

   public BAddressEntry[] getAddresses() {
      return new BAddressEntry[]{
         this.getEntry0(),
         this.getEntry1(),
         this.getEntry2(),
         this.getEntry3(),
         this.getEntry4(),
         this.getEntry5(),
         this.getEntry6(),
         this.getEntry7(),
         this.getEntry8(),
         this.getEntry9(),
         this.getEntry10(),
         this.getEntry11(),
         this.getEntry12(),
         this.getEntry13(),
         this.getEntry14()
      };
   }
}
