package com.tridium.lonworks.netmgmt;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;
import javax.baja.sys.BObject;

public class PTable {
   public static final int CENTER = 0;
   public static final int RIGHT = 1;
   public static final int LEFT = 2;
   private static final int PAD = 3;
   private int numColumns;
   private Vector<PTable.Row> table = new Vector<>();
   private PTable.Row row;
   private int[] colAttr;

   public PTable(String[] headers) {
      this(headers, 0);
   }

   public PTable(String[] headers, int defAttr) {
      this.table.addElement(new PTable.Row(headers));
      this.numColumns = headers.length;
      this.row = new PTable.Row(this.numColumns);
      this.colAttr = new int[this.numColumns];

      for (int i = 0; i < this.numColumns; i++) {
         this.colAttr[i] = defAttr;
      }
   }

   public void setColumnAttribute(int col, int attr) {
      if (col < this.numColumns) {
         this.colAttr[col] = attr;
      }
   }

   public void add(int entry) {
      this.add(Integer.toString(entry));
   }

   public void add(BObject entry) {
      this.add(entry.toString());
   }

   public void add(String entry) {
      if (this.row == null) {
         this.row = new PTable.Row(this.numColumns);
      }

      this.row.add(entry);
      if (this.row.index >= this.numColumns) {
         this.endRow();
      }
   }

   public void add(String entry, int mult) {
      while (mult-- > 0) {
         this.add(entry);
      }
   }

   public void addBanner(String ban) {
      if (this.row == null) {
         this.row = new PTable.Row(this.numColumns);
      }

      this.row.banner = ban;
      this.endRow();
   }

   public void newRow() {
      if (this.row != null) {
         this.table.addElement(this.row);
      }

      this.row = new PTable.Row(this.numColumns);
   }

   public void endRow() {
      this.table.addElement(this.row);
      this.row = null;
   }

   public void toString(PrintWriter out) {
      int numRows = this.table.size();
      PTable.Row[] tab = new PTable.Row[numRows];
      int[] colWidth = new int[this.numColumns];
      int tableWidth = 0;
      this.table.copyInto(tab);

      for (int n = 0; n < this.numColumns; n++) {
         colWidth[n] = 0;
      }

      for (int i = 0; i < numRows; i++) {
         PTable.Row r = tab[i];

         for (int j = 0; j < r.index; j++) {
            int len = r.entries[j].length();
            if (len > colWidth[j]) {
               colWidth[j] = len;
            }
         }
      }

      try {
         for (int n = 0; n < this.numColumns; n++) {
            tableWidth += colWidth[n] + 3;
         }

         this.fill(out, tableWidth, '=');
         out.println();

         for (int i = 0; i < numRows; i++) {
            PTable.Row r = tab[i];

            for (int jx = 0; jx < r.index; jx++) {
               String e = r.entries[jx];
               int pad = colWidth[jx] - e.length() + 3;
               int leftPad;
               if (i == 0) {
                  leftPad = pad - pad / 2;
               } else if (this.colAttr[jx] == 2) {
                  leftPad = 1;
               } else if (this.colAttr[jx] == 1) {
                  leftPad = pad - 1;
               } else {
                  leftPad = pad - pad / 2;
               }

               this.fill(out, leftPad, ' ');
               out.print(e);
               this.fill(out, pad - leftPad, ' ');
            }

            if (r.banner != null) {
               out.print(r.banner);
            }

            out.println();
            if (i == 0) {
               this.fill(out, tableWidth, '-');
               out.println();
            }
         }

         this.fill(out, tableWidth, '=');
         out.println();
      } catch (Throwable var12) {
         throw new RuntimeException("error building ptable string " + var12);
      }
   }

   private void fill(PrintWriter out, int len, char c) throws IOException {
      String s = Character.valueOf(c).toString();

      while (len-- > 0) {
         out.print(s);
      }
   }

   private static class Row {
      String[] entries;
      int index;
      String banner = null;

      Row(int n) {
         this.entries = new String[n];
         this.index = 0;
      }

      Row(String[] r) {
         this.entries = r;
         this.index = r.length;
      }

      void add(String e) {
         this.entries[this.index++] = e;
      }
   }
}
