package com.honeywell.easybinding.service;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.collection.BITable;
import javax.baja.collection.TableCursor;
import javax.baja.control.BControlPoint;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.file.BDirectory;
import javax.baja.file.BIFile;
import javax.baja.file.types.text.BPxFile;
import javax.baja.job.BSimpleJob;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BValueBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.px.PxDecoder;
import javax.baja.ui.px.PxEncoder;

@NiagaraType
public class BEasyBindingNiagaraVirtualSupportJob extends BSimpleJob {
   public static final Type TYPE;
   private static final Logger a;
   private List<BControlPoint> b;
   private List<BComponent> c;
   private StringBuilder d = new StringBuilder();
   private List<String> e = new ArrayList<>();
   private static final String[] z;

   public Type getType() {
      return TYPE;
   }

   public BEasyBindingNiagaraVirtualSupportJob() {
      this.b = new ArrayList<>();
      this.c = new ArrayList<>();
   }

   public void run(Context var1) {
      this.e.add(z[24]);
      this.e.add(z[21]);
      this.e.add(z[22]);
      BDirectory var2 = (BDirectory)BOrd.make(z[23]).resolve().get();
      this.b = this.e();
      this.c = this.d();
      this.processAllPxFiles(var2);
      this.a();
   }

   private void a(String var1) {
      this.d.append(var1);
      this.d.append("\n");
   }

   private void a() {
      try {
         this.b();
      } catch (Exception var3) {
         Exception var1 = var3;

         try {
            if (a.isLoggable(Level.FINER)) {
               a.log(Level.FINER, var1.getMessage(), (Throwable)var1);
            }
         } catch (Exception var2) {
            throw var2;
         }
      }
   }

   // $VF: Could not verify finally blocks. A semaphore variable has been added to preserve control flow.
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private void b() {
      try {
         FileWriter var21 = new FileWriter(this.c());
         Throwable var2 = null;
         boolean var13 = false /* VF: Semaphore variable */;

         try {
            var13 = true;
            var21.write(this.d.toString());
            var13 = false;
         } catch (Throwable var17) {
            var2 = var17;
            throw var17;
         } finally {
            if (var13) {
               label82: {
                  label81: {
                     try {
                        if (var21 == null) {
                           break label82;
                        }

                        if (var2 == null) {
                           break label81;
                        }
                     } catch (Throwable var18) {
                        throw var18;
                     }

                     try {
                        var21.close();
                     } catch (Throwable var15) {
                        var2.addSuppressed(var15);
                     }
                     break label82;
                  }

                  var21.close();
               }
            }
         }

         if (var21 != null) {
            if (var2 != null) {
               try {
                  var21.close();
               } catch (Throwable var16) {
                  var2.addSuppressed(var16);
               }
            } else {
               var21.close();
            }
         }
      } catch (IOException var20) {
         IOException var1 = var20;

         try {
            if (a.isLoggable(Level.FINER)) {
               a.log(Level.FINER, var1.getMessage(), (Throwable)var1);
            }
         } catch (Throwable var14) {
            throw var14;
         }
      }
   }

   private String c() {
      StringBuilder var1 = new StringBuilder();
      var1.append(Sys.getNiagaraSharedUserHome().toPath().toString());
      var1.append("\\");
      var1.append(z[14]);
      var1.append(BAbsTime.now().getMillis());
      var1.append(z[15]);
      return var1.toString();
   }

   private List<BComponent> d() {
      ArrayList var1 = new ArrayList();
      String var2 = z[26];
      TableCursor var3 = ((BITable)BOrd.make(var2).get(Sys.getStation())).cursor();

      while (var3.next()) {
         BComponent var4 = (BComponent)var3.get();
         var1.add(var4);
      }

      return var1;
   }

   private List<BControlPoint> e() {
      ArrayList var1 = new ArrayList();
      String var2 = z[13];
      TableCursor var3 = ((BITable)BOrd.make(var2).get(Sys.getStation())).cursor();

      while (var3.next()) {
         BControlPoint var4 = (BControlPoint)var3.get();
         var1.add(var4);
      }

      return var1;
   }

   public void processAllPxFiles(BDirectory var1) {
      BIFile[] var2 = var1.listFiles();

      for (BIFile var6 : var2) {
         try {
            try {
               if (!var6.isDirectory()) {
                  this.a(var6);
                  continue;
               }
            } catch (Exception var9) {
               throw var9;
            }

            this.processAllPxFiles((BDirectory)var6);
         } catch (Exception var10) {
            Exception var7 = var10;

            try {
               if (a.isLoggable(Level.FINER)) {
                  a.log(Level.FINER, () -> z[12] + var6.getFileName());
                  a.log(Level.FINER, var7.getMessage(), (Throwable)var7);
               }
            } catch (Exception var8) {
               throw var8;
            }
         }
      }
   }

   private void a(BIFile var1) {
      try {
         if (var1 instanceof BPxFile) {
            this.a(z[9] + var1.getFileName());
            PxDecoder var7 = new PxDecoder(var1);
            BWidget var2 = var7.decodeDocument();

            label30: {
               try {
                  if (this.a(var2)) {
                     this.a(var1, var2, var7);
                     break label30;
                  }
               } catch (Exception var5) {
                  throw var5;
               }

               this.a(z[11] + var1.getFileName());
            }

            this.a(z[10] + var1.getFileName());
         }
      } catch (Exception var6) {
         Exception var3 = var6;

         try {
            if (a.isLoggable(Level.FINER)) {
               a.log(Level.FINER, var3.getMessage(), (Throwable)var3);
            }
         } catch (Exception var4) {
            throw var4;
         }
      }
   }

   private boolean a(BWidget var1) {
      BWidget[] var2 = var1.getChildWidgets();
      boolean var3 = false;

      for (BWidget var7 : var2) {
         if (var7.getType().toString().equals(z[25])) {
            var3 = true;
            this.b(var7);
         } else if (var7.getChildWidgets().length != 0) {
            boolean var8 = this.a(var7);
            if (!var3 && var8) {
               var3 = true;
            }
         }
      }

      return var3;
   }

   private void b(BWidget var1) {
      BValueBinding[] var2 = (BValueBinding[])var1.getChildren(BValueBinding.class);

      for (BValueBinding var6 : var2) {
         if (this.e.contains(var6.getType().toString())) {
            this.a(var6);
         }
      }
   }

   private void a(BBinding var1) {
      BOrd var2 = var1.getOrd();
      if (this.a(var2)) {
         if (!this.b(var2)) {
            BOrd var3 = this.c(var2);
            if (!var3.equals(BOrd.NULL)) {
               var1.setOrd(var3);
            } else {
               this.a(z[16] + var2.toString());
            }
         }
      } else {
         a.log(Level.INFO, () -> z[8] + var2.toString());
      }
   }

   private boolean a(BOrd var1) {
      boolean var2 = false;

      try {
         String var14 = "";
         if (var1 != null) {
            String var4 = var1.toString();

            String var10000;
            label73: {
               try {
                  if (var4.indexOf(z[4]) > -1) {
                     var10000 = var4.substring(var4.indexOf(z[1]) + z[3].length());
                     break label73;
                  }
               } catch (Exception var12) {
                  throw var12;
               }

               var10000 = "";
            }

            var14 = var10000;
         }

         for (BComponent var5 : this.c) {
            String var6 = z[0] + var5.getSlotPath().toString() + z[5] + var14;
            BObject var7 = BOrd.make(var6).resolve(Sys.getStation()).get();

            boolean var16;
            label59: {
               try {
                  if (var7 != null) {
                     var16 = true;
                     break label59;
                  }
               } catch (Exception var11) {
                  throw var11;
               }

               var16 = false;
            }

            var2 = var16;

            try {
               if (var2) {
                  break;
               }
            } catch (Exception var10) {
               throw var10;
            }
         }

         try {
            if (!var2) {
               this.a(z[2] + var14);
            }
         } catch (Exception var9) {
            throw var9;
         }
      } catch (Exception var13) {
         Exception var3 = var13;

         try {
            if (a.isLoggable(Level.FINER)) {
               a.info(var3.getMessage());
            }
         } catch (Exception var8) {
            throw var8;
         }
      }

      return var2;
   }

   private boolean b(BOrd var1) {
      boolean var2 = false;

      try {
         BObject var8 = var1.resolve(Sys.getStation()).get();

         boolean var10000;
         label38: {
            try {
               if (var8 != null) {
                  var10000 = true;
                  break label38;
               }
            } catch (Exception var6) {
               throw var6;
            }

            var10000 = false;
         }

         var2 = var10000;

         try {
            if (var2) {
               this.a(z[17] + var1.toString() + z[18]);
            }
         } catch (Exception var5) {
            throw var5;
         }
      } catch (Exception var7) {
         Exception var3 = var7;

         try {
            if (a.isLoggable(Level.FINER)) {
               a.info(var3.getMessage());
            }
         } catch (Exception var4) {
            throw var4;
         }
      }

      return var2;
   }

   private BOrd c(BOrd var1) {
      BOrd var2 = BOrd.NULL;

      for (BControlPoint var4 : this.b) {
         BAbstractProxyExt var5 = var4.getProxyExt();
         if (var5 != null) {
            BValue var6 = var5.get(z[20]);
            String var7 = var6 == null ? "" : z[19] + var6.toString();
            if (!var7.equals("") && var7.equals(var1.toString())) {
               var2 = this.a(var4);
               break;
            }
         }
      }

      return var2;
   }

   private BOrd a(BControlPoint var1) {
      String var2 = var1.getSlotPathOrd().toString();
      if (!var2.toLowerCase(Locale.ROOT).contains(z[7])) {
         var2 = z[6] + var2;
      }

      return BOrd.make(var2);
   }

   private void a(BIFile var1, BWidget var2, PxDecoder var3) {
      ByteArrayOutputStream var4 = new ByteArrayOutputStream();

      try {
         new PxEncoder(var4).encodeDocument(var2, var3.getPxProperties(), var3.getPxLayers(), null);
         this.a(var1, var4);
      } catch (IOException var6) {
         a.log(Level.SEVERE, var6.getMessage(), (Throwable)var6);
      }
   }

   // $VF: Could not verify finally blocks. A semaphore variable has been added to preserve control flow.
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private void a(BIFile var1, ByteArrayOutputStream var2) throws IOException {
      OutputStream var3 = var1.getOutputStream();
      Throwable var4 = null;
      boolean var13 = false /* VF: Semaphore variable */;

      try {
         var13 = true;
         var3.write(var2.toByteArray());
         var13 = false;
      } catch (Throwable var16) {
         var4 = var16;
         throw var16;
      } finally {
         if (var13) {
            label68: {
               label67: {
                  try {
                     if (var3 == null) {
                        break label68;
                     }

                     if (var4 == null) {
                        break label67;
                     }
                  } catch (Throwable var17) {
                     throw var17;
                  }

                  try {
                     var3.close();
                  } catch (Throwable var14) {
                     var4.addSuppressed(var14);
                  }
                  break label68;
               }

               var3.close();
            }
         }
      }

      if (var3 != null) {
         if (var4 != null) {
            try {
               var3.close();
            } catch (Throwable var15) {
               var4.addSuppressed(var15);
            }
         } else {
            var3.close();
         }
      }
   }

   static {
      String[] var10000 = new String[27];
      String[] var10001 = var10000;
      byte var10002 = 0;
      String var10003 = "\u0014\u0012A?M\b\b\u001a7";
      int var10004 = -1;

      while (true) {
         char[] var14 = var10003.toCharArray();
         int var10006 = var14.length;
         char[] var17 = var14;
         var10004 = var10006;

         for (int var0 = 0; var10004 > var0; var0++) {
            char var10008 = var17[var0];
            byte var10009;
            switch (var0 % 5) {
               case 0:
                  var10009 = 103;
                  break;
               case 1:
                  var10009 = 102;
                  break;
               case 2:
                  var10009 = 32;
                  break;
               case 3:
                  var10009 = 75;
                  break;
               default:
                  var10009 = 36;
            }

            var17[var0] = (char)(var10008 ^ var10009);
         }

         String var22 = new String(var17).intern();
         switch (var10004) {
            case 0:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 2;
               var10003 = "1\u000fR?Q\u0006\n\u0000;K\u000e\bTkJ\b\u0012\u0000-K\u0012\bDq";
               var10004 = 1;
               break;
            case 1:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 3;
               var10003 = "\u0014\nO?\u001eH";
               var10004 = 2;
               break;
            case 2:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 4;
               var10003 = "\u0014\nO?\u001eH";
               var10004 = 3;
               break;
            case 3:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 5;
               var10003 = "\u001b\u0010I9P\u0012\u0007Lq\u000b";
               var10004 = 4;
               break;
            case 4:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 6;
               var10003 = "\u0014\u0012A?M\b\b\u001a7";
               var10004 = 5;
               break;
            case 5:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 7;
               var10003 = "\u0014\u0012A?M\b\b\u001a";
               var10004 = 6;
               break;
            case 6:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 8;
               var10003 = ")\u000fA,E\u0015\u0007\u0000=M\u0015\u0012U*HG\u0016O\"J\u0013FN$PG\u0000O%@]F";
               var10004 = 7;
               break;
            case 7:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 9;
               var10003 = "4\u0012A9P\u0002\u0002\u0000>T\u0000\u0014A/M\t\u0001\u0000\u001b\\]";
               var10004 = 8;
               break;
            case 8:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 10;
               var10003 = "$\tM;H\u0002\u0012E/\u0004\u0012\u0016G9E\u0003\u000fN,\u00047\u001e\u001a";
               var10004 = 9;
               break;
            case 9:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 11;
               var10003 = "\t\t\u0000.E\u0014\u001f\u0000<M\u0003\u0001E?WG\u000fNq\u0004";
               var10004 = 10;
               break;
            case 10:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 12;
               var10003 = "!\u0007I'A\u0003FT$\u0004\u0012\u0016D*P\u0002FP3\u0004\u0001\u000fL.\u001e";
               var10004 = 11;
               break;
            case 11:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 13;
               var10003 = "\u0014\nO?\u001e\u001b\u0004Q'\u001e\u0014\u0003L.G\u0013F\nkB\u0015\tMkG\b\bT9K\u000b\\c$J\u0013\u0014O't\b\u000fN?";
               var10004 = 12;
               break;
            case 12:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 14;
               var10003 = "\u0002\u0007S2F\u000e\bD\"J\u00006X\tM\t\u0002I%C2\u0016D*P\u00025T*P\u0012\u0015";
               var10004 = 13;
               break;
            case 13:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 15;
               var10003 = "I\u0012X?";
               var10004 = 14;
               break;
            case 14:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 16;
               var10003 = "$\tU'@G\bO?\u0004\u0001\u000fN/\u0004\u0017\tI%PG\u0000O9\u001e";
               var10004 = 15;
               break;
            case 15:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 17;
               var10003 = "\u0004\u0013R9A\t\u0012\u0000$V\u0003\\";
               var10004 = 16;
               break;
            case 16:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 18;
               var10003 = "G\u000fSkV\u0002\u0015O'R\u0002\u0002\u0000\"JG\u0015T*P\u000e\tNkW\bFU;@\u0006\u0012EkW\f\u000fP;A\u0003";
               var10004 = 17;
               break;
            case 17:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 19;
               var10003 = "\u0014\u0012A?M\b\b\u001a7";
               var10004 = 18;
               break;
            case 18:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 20;
               var10003 = "\u0017\tI%P.\u0002";
               var10004 = 19;
               break;
            case 19:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 21;
               var10003 = "\u0002\u0007S2f\u000e\bD\"J\u0000\\e*W\u001e'L*V\n$I%@\u000e\bG";
               var10004 = 20;
               break;
            case 20:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 22;
               var10003 = "\u0002\u0007S2f\u000e\bD\"J\u0000\\e*W\u001e)V.V\u0015\u000fD.f\u000e\bD\"J\u0000";
               var10004 = 21;
               break;
            case 21:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 23;
               var10003 = "\u0001\u000fL.\u001e9";
               var10004 = 22;
               break;
            case 22:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 24;
               var10003 = "\u0002\u0007S2f\u000e\bD\"J\u0000\\e*W\u001e0A'Q\u0002$I%@\u000e\bG";
               var10004 = 23;
               break;
            case 23:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 25;
               var10003 = "\u0002\u0007S2f\u000e\bD\"J\u0000\\e*W\u001e$I%@\u000e\bG\u001cM\u0003\u0001E?";
               var10004 = 24;
               break;
            case 24:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 26;
               var10003 = "\u0014\nO?\u001e\u001b\u0004Q'\u001e\u0014\u0003L.G\u0013F\nkB\u0015\tMkJ\u000e\u0007G*V\u0006\"R\"R\u0002\u0014\u001a\u0005M\u0006\u0001A9E1\u000fR?Q\u0006\nd.R\u000e\u0005E\u000e\\\u0013";
               var10004 = 25;
               break;
            case 25:
               var10001[var10002] = var22;
               z = var10000;
               TYPE = Sys.loadType(BEasyBindingNiagaraVirtualSupportJob.class);
               char[] var2 = "\"\u0007S2f\u000e\bD\"J\u0000".toCharArray();
               int var9 = var2.length;
               char[] var6 = var2;
               int var3 = var9;

               for (int var1 = 0; var3 > var1; var1++) {
                  char var19 = var6[var1];
                  byte var23;
                  switch (var1 % 5) {
                     case 0:
                        var23 = 103;
                        break;
                     case 1:
                        var23 = 102;
                        break;
                     case 2:
                        var23 = 32;
                        break;
                     case 3:
                        var23 = 75;
                        break;
                     default:
                        var23 = 36;
                  }

                  var6[var1] = (char)(var19 ^ var23);
               }

               var10003 = new String(var6).intern();
               byte var5 = -1;
               a = Logger.getLogger(var10003);
               return;
            default:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 1;
               var10003 = "\u0014\nO?\u001eH";
               var10004 = 0;
         }
      }
   }
}
