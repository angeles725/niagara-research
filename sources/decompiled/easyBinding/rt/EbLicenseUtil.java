package com.honeywell.easybinding.util;

import com.tridium.platform.BSystemPlatformService;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.baja.license.Feature;
import javax.baja.license.FeatureNotLicensedException;
import javax.baja.license.LicenseManager;
import javax.baja.nre.util.SystemFiles;
import javax.baja.sys.Sys;

public class EbLicenseUtil {
   private static List<String> a;
   private static final String[] z;

   private EbLicenseUtil() {
   }

   public static String getBrandFromLicenseFile() {
      LicenseManager var0 = Sys.getLicenseManager();
      Feature var1 = var0.getFeature(z[70], z[68]);
      return var1.get(z[69]);
   }

   public static Feature obtainEBFeatureFromLicense(String var0) {
      Feature var1 = null;
      if (var0 != null) {
         String var2 = var0;
         byte var3 = -1;

         label76: {
            label75: {
               label74: {
                  label73: {
                     label72: {
                        label71: {
                           label70: {
                              label69: {
                                 try {
                                    switch (var2.hashCode()) {
                                       case -1835030945:
                                          break label69;
                                       case -783283573:
                                          break label70;
                                       case -165290253:
                                          break label73;
                                       case 81876:
                                          if (!var2.equals(z[18])) {
                                             break label76;
                                          }
                                          break label75;
                                       case 2692031:
                                          break label71;
                                       case 81072509:
                                          break;
                                       case 216503108:
                                          break label74;
                                       case 1680063346:
                                          break label72;
                                       default:
                                          break label76;
                                    }
                                 } catch (FeatureNotLicensedException var4) {
                                    throw var4;
                                 }

                                 if (var0.equals(z[6])) {
                                    var3 = 1;
                                 }
                                 break label76;
                              }

                              if (var0.equals(z[14])) {
                                 var3 = 2;
                              }
                              break label76;
                           }

                           if (var0.equals(z[15])) {
                              var3 = 3;
                           }
                           break label76;
                        }

                        if (var0.equals(z[16])) {
                           var3 = 4;
                        }
                        break label76;
                     }

                     if (var0.equals(z[2])) {
                        var3 = 5;
                     }
                     break label76;
                  }

                  if (var0.equals(z[12])) {
                     var3 = 6;
                  }
                  break label76;
               }

               if (var0.equals(z[19])) {
                  var3 = 7;
               }
               break label76;
            }

            var3 = 0;
         }

         switch (var3) {
            case 0:
               var1 = Sys.getLicenseManager().getFeature(z[5], z[10]);
               break;
            case 1:
               var1 = Sys.getLicenseManager().getFeature(z[20], z[0]);
               break;
            case 2:
               var1 = Sys.getLicenseManager().getFeature(z[4], z[23]);
               break;
            case 3:
               var1 = Sys.getLicenseManager().getFeature(z[17], z[1]);
               break;
            case 4:
               var1 = Sys.getLicenseManager().getFeature(z[22], z[13]);
               break;
            case 5:
               var1 = Sys.getLicenseManager().getFeature(z[7], z[8]);
               break;
            case 6:
               var1 = Sys.getLicenseManager().getFeature(z[11], z[3]);
               break;
            case 7:
               if (a()) {
                  var1 = Sys.getLicenseManager().getFeature(z[21], z[9]);
               } else {
                  var1 = null;
               }
               break;
            default:
               var1 = null;
         }
      }

      return var1;
   }

   private static boolean a() {
      LicenseManager var0 = Sys.getLicenseManager();
      Feature var1 = var0.getFeature(z[71], z[72]);
      long var2 = var1.getExpiration();

      try {
         if (Long.MAX_VALUE != var2) {
            return true;
         }
      } catch (FeatureNotLicensedException var4) {
         throw var4;
      }

      return false;
   }

   public static Feature checkEasyBindingFeature() {
      String var0 = getBrandFromLicenseFile();
      if (var0 != null) {
         String var2 = var0;
         byte var3 = -1;

         label78: {
            label77: {
               label76: {
                  label75: {
                     label74: {
                        label73: {
                           label72: {
                              label71: {
                                 try {
                                    switch (var2.hashCode()) {
                                       case -1835030945:
                                          break label71;
                                       case -783283573:
                                          break label72;
                                       case -165290253:
                                          break label75;
                                       case 81876:
                                          if (!var2.equals(z[55])) {
                                             break label78;
                                          }
                                          break label77;
                                       case 2692031:
                                          break label73;
                                       case 81072509:
                                          break;
                                       case 216503108:
                                          break label76;
                                       case 1680063346:
                                          break label74;
                                       default:
                                          break label78;
                                    }
                                 } catch (FeatureNotLicensedException var4) {
                                    throw var4;
                                 }

                                 if (var0.equals(z[64])) {
                                    var3 = 1;
                                 }
                                 break label78;
                              }

                              if (var0.equals(z[50])) {
                                 var3 = 2;
                              }
                              break label78;
                           }

                           if (var0.equals(z[42])) {
                              var3 = 3;
                           }
                           break label78;
                        }

                        if (var0.equals(z[43])) {
                           var3 = 4;
                        }
                        break label78;
                     }

                     if (var0.equals(z[62])) {
                        var3 = 5;
                     }
                     break label78;
                  }

                  if (var0.equals(z[44])) {
                     var3 = 6;
                  }
                  break label78;
               }

               if (var0.equals(z[54])) {
                  var3 = 7;
               }
               break label78;
            }

            var3 = 0;
         }

         Feature var1;
         switch (var3) {
            case 0:
               var1 = Sys.getLicenseManager().getFeature(z[53], z[66]);
               break;
            case 1:
               var1 = Sys.getLicenseManager().getFeature(z[59], z[47]);
               break;
            case 2:
               var1 = Sys.getLicenseManager().getFeature(z[45], z[61]);
               break;
            case 3:
               var1 = Sys.getLicenseManager().getFeature(z[46], z[67]);
               break;
            case 4:
               var1 = Sys.getLicenseManager().getFeature(z[48], z[58]);
               break;
            case 5:
               var1 = Sys.getLicenseManager().getFeature(z[49], z[51]);
               break;
            case 6:
               var1 = Sys.getLicenseManager().getFeature(z[65], z[60]);
               break;
            case 7:
               if (a()) {
                  var1 = Sys.getLicenseManager().getFeature(z[52], z[63]);
               } else {
                  var1 = null;
               }
               break;
            default:
               throw new FeatureNotLicensedException(z[56]);
         }

         return var1;
      } else {
         throw new FeatureNotLicensedException(z[57]);
      }
   }

   public static String obtainBrandBasedIcon(String var0) {
      String var1 = z[27];
      File var2 = SystemFiles.getNiagaraHomeDirectory();
      Object var3 = z[28];
      if (var0 != null) {
         String var4 = var0;
         byte var5 = -1;

         label62: {
            label61: {
               label60: {
                  label59: {
                     label58: {
                        try {
                           switch (var4.hashCode()) {
                              case -783283573:
                                 break label58;
                              case 81876:
                                 if (!var4.equals(z[32])) {
                                    break label62;
                                 }
                                 break label61;
                              case 2692031:
                                 break label59;
                              case 81072509:
                                 break;
                              case 1680063346:
                                 break label60;
                              default:
                                 break label62;
                           }
                        } catch (FeatureNotLicensedException var11) {
                           throw var11;
                        }

                        if (var0.equals(z[34])) {
                           var5 = 1;
                        }
                        break label62;
                     }

                     if (var0.equals(z[37])) {
                        var5 = 2;
                     }
                     break label62;
                  }

                  if (var0.equals(z[38])) {
                     var5 = 3;
                  }
                  break label62;
               }

               if (var0.equals(z[40])) {
                  var5 = 4;
               }
               break label62;
            }

            var5 = 0;
         }

         switch (var5) {
            case 0:
               File var6 = new File(var2 + var3 + z[35]);
               if (var6.exists()) {
                  var1 = z[33];
               }
               break;
            case 1:
               File var7 = new File(var2 + var3 + z[30]);
               if (var7.exists()) {
                  var1 = z[29];
               }
               break;
            case 2:
               File var8 = new File(var2 + var3 + z[25]);
               if (var8.exists()) {
                  var1 = z[26];
               }
               break;
            case 3:
               File var9 = new File(var2 + var3 + z[24]);
               if (var9.exists()) {
                  var1 = z[31];
               }
               break;
            case 4:
               File var10 = new File(var2 + var3 + z[39]);
               if (var10.exists()) {
                  var1 = z[41];
               }
               break;
            default:
               var1 = z[36];
         }
      }

      return var1;
   }

   public static boolean isConnectedToEdgeController() {
      boolean var0 = false;
      BSystemPlatformService var1 = (BSystemPlatformService)Sys.getService(BSystemPlatformService.TYPE);
      if (a.contains(var1.getOsName().toUpperCase())) {
         var0 = true;
      }

      return var0;
   }

   static {
      String[] var10000 = new String[73];
      String[] var10001 = var10000;
      int var10002 = 0;
      String var10003 = "$-Ug\u001f?;yK\u0010(+UE";
      int var10004 = -1;

      while (true) {
         char[] var15 = var10003.toCharArray();
         int var10006 = var15.length;
         char[] var22 = var15;
         var10004 = var10006;

         for (int var0 = 0; var10004 > var0; var0++) {
            char var10008 = var22[var0];
            byte var10009;
            switch (var0 % 5) {
               case 0:
                  var10009 = 76;
                  break;
               case 1:
                  var10009 = 66;
                  break;
               case 2:
                  var10009 = 59;
                  break;
               case 3:
                  var10009 = 34;
                  break;
               default:
                  var10009 = 126;
            }

            var22[var0] = (char)(var10008 ^ var10009);
         }

         String var26 = new String(var22).intern();
         switch (var10004) {
            case 0:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 2;
               var10003 = "\r.^P\n#,\u0016";
               var10004 = 1;
               break;
            case 1:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 3;
               var10003 = "$-Ug\u001f?;yK\u0010(+UE";
               var10004 = 2;
               break;
            case 2:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 4;
               var10003 = "\u0004-UG\u0007;'WN";
               var10004 = 3;
               break;
            case 3:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 5;
               var10003 = "\u001f#RC<90\\G\r?\u0001TL\n>-WQ";
               var10004 = 4;
               break;
            case 4:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 6;
               var10003 = "\u00180^L\u001a";
               var10004 = 5;
               break;
            case 5:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 7;
               var10003 = "\r.^P\n#,";
               var10004 = 6;
               break;
            case 6:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 8;
               var10003 = "$-Ug\u001f?;yK\u0010(+UE";
               var10004 = 7;
               break;
            case 7:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 9;
               var10003 = "$-Ug\u001f?;yK\u0010(+UE";
               var10004 = 8;
               break;
            case 8:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 10;
               var10003 = "$-Ug\u001f?;yK\u0010(+UE";
               var10004 = 9;
               break;
            case 9:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 11;
               var10003 = "\u0004-UG\u0007;'WN";
               var10004 = 10;
               break;
            case 10:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 12;
               var10003 = "\u000f-VD\u0011>6zL\u001a\t,^P\u00195";
               var10004 = 11;
               break;
            case 11:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 13;
               var10003 = "$-Ug\u001f?;yK\u0010(+UE";
               var10004 = 12;
               break;
            case 12:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 14;
               var10003 = "\u0004-UG\u0007;'WN<\u0001\u0011";
               var10004 = 13;
               break;
            case 13:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 15;
               var10003 = "\u000f'UV\f-\u000eRL\u001b";
               var10004 = 14;
               break;
            case 14:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 16;
               var10003 = "\u001b'YQ";
               var10004 = 15;
               break;
            case 15:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 17;
               var10003 = "\u0004-UG\u0007;'WN=),OP\u001f\u0000+UG";
               var10004 = 16;
               break;
            case 16:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 18;
               var10003 = "\u001f\u0000x";
               var10004 = 17;
               break;
            case 17:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 19;
               var10003 = "\u000f-VD\u0011>6kM\u0017\"6";
               var10004 = 18;
               break;
            case 18:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 20;
               var10003 = "\u00180^L\u001a\u0013\u0001TL\n>-W}-51OG\u0013?\u001dwV\u001a";
               var10004 = 19;
               break;
            case 19:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 21;
               var10003 = "\u0004-UG\u0007;'WN";
               var10004 = 20;
               break;
            case 20:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 22;
               var10003 = "\u0004-UG\u0007;'WN";
               var10004 = 21;
               break;
            case 21:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 23;
               var10003 = "$-Ug\u001f?;yK\u0010(+UE";
               var10004 = 22;
               break;
            case 22:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 24;
               var10003 = "8*^O\u001b\u0004-UG\u0007;'WNS9:\u0015H\u001f>";
               var10004 = 23;
               break;
            case 23:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 25;
               var10003 = "8*^O\u001b\u000f'UV\f-\u000eRL\u001ba7C\f\u0014-0";
               var10004 = 24;
               break;
            case 24:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 26;
               var10003 = "c/TF\u000b '\u0014V\u0016)/^a\u001b\"6IC2%,^\r\u0017!#\\G1:'IP\u0017('H\r\u0017/-UQQ4q\t\r\t#0P@\u001b\"!S\f\u000e\"%";
               var10004 = 25;
               break;
            case 25:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 27;
               var10003 = "c/TF\u000b '\u0014K\u001d#,H\r\u0006\u007fp\u0014U\u0011>)YG\u0010/*\u0015R\u0010+";
               var10004 = 26;
               break;
            case 26:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 28;
               var10003 = "\u0010/TF\u000b 'H~";
               var10004 = 27;
               break;
            case 27:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 29;
               var10003 = "c/TF\u000b '\u0014V\u0016)/^k/\u001a+HK\u0011\"mRO\u001f+'tT\u001b>0RF\u001b?mRA\u0011\"1\u0014ZM~mLM\f' ^L\u001d$lKL\u0019";
               var10004 = 28;
               break;
            case 28:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 30;
               var10003 = "8*^O\u001b\u0005\u0013mK\r%-U\u000f\u000b4lQC\f";
               var10004 = 29;
               break;
            case 29:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 31;
               var10003 = "c/TF\u000b '\u0014V\u0016)/^j\u0011\"'BU\u001b .\u0014K\u0013-%^m\b)0IK\u001a)1\u0014K\u001d#,H\r\u0006\u007fp\u0014U\u0011>)YG\u0010/*\u0015R\u0010+";
               var10004 = 30;
               break;
            case 30:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 32;
               var10003 = "\u001f\u0000x";
               var10004 = 31;
               break;
            case 31:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 33;
               var10003 = "c/TF\u000b '\u0014V\u0016)/^q<\u000fmRO\u001f+'tT\u001b>0RF\u001b?mRA\u0011\"1\u0014ZM~mLM\f' ^L\u001d$lKL\u0019";
               var10004 = 32;
               break;
            case 32:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 34;
               var10003 = "\u00180^L\u001a";
               var10004 = 33;
               break;
            case 33:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 35;
               var10003 = "8*^O\u001b\u001f\u0000x\u000f\u000b4lQC\f";
               var10004 = 34;
               break;
            case 34:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 36;
               var10003 = "c/TF\u000b '\u0014K\u001d#,H\r\u0006\u007fp\u0014U\u0011>)YG\u0010/*\u0015R\u0010+";
               var10004 = 35;
               break;
            case 35:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 37;
               var10003 = "\u000f'UV\f-\u000eRL\u001b";
               var10004 = 36;
               break;
            case 36:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 38;
               var10003 = "\u001b'YQ";
               var10004 = 37;
               break;
            case 37:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 39;
               var10003 = "8*^O\u001b\r.^P\n#,\u0016W\u0006b(ZP";
               var10004 = 38;
               break;
            case 38:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 40;
               var10003 = "\r.^P\n#,\u0016";
               var10004 = 39;
               break;
            case 39:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 41;
               var10003 = "c/TF\u000b '\u0014V\u0016)/^c\u0012)0OM\u0010c+VC\u0019)\rMG\f>+_G\rc+XM\u0010?mC\u0011Lc5TP\u0015.'UA\u0016b2UE";
               var10004 = 40;
               break;
            case 40:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 42;
               var10003 = "\u000f'UV\f-\u000eRL\u001b";
               var10004 = 41;
               break;
            case 41:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 43;
               var10003 = "\u001b'YQ";
               var10004 = 42;
               break;
            case 42:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 44;
               var10003 = "\u000f-VD\u0011>6zL\u001a\t,^P\u00195";
               var10004 = 43;
               break;
            case 43:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 45;
               var10003 = "\u0004-UG\u0007;'WN";
               var10004 = 44;
               break;
            case 44:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 46;
               var10003 = "\u0004-UG\u0007;'WN=),OP\u001f\u0000+UG";
               var10004 = 45;
               break;
            case 45:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 47;
               var10003 = "$-Ug\u001f?;yK\u0010(+UE";
               var10004 = 46;
               break;
            case 46:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 48;
               var10003 = "\u0004-UG\u0007;'WN";
               var10004 = 47;
               break;
            case 47:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 49;
               var10003 = "\r.^P\n#,";
               var10004 = 48;
               break;
            case 48:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 50;
               var10003 = "\u0004-UG\u0007;'WN<\u0001\u0011";
               var10004 = 49;
               break;
            case 49:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 51;
               var10003 = "$-Ug\u001f?;yK\u0010(+UE";
               var10004 = 50;
               break;
            case 50:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 52;
               var10003 = "\u0004-UG\u0007;'WN";
               var10004 = 51;
               break;
            case 51:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 53;
               var10003 = "\u001f#RC<90\\G\r?\u0001TL\n>-WQ";
               var10004 = 52;
               break;
            case 52:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 54;
               var10003 = "\u000f-VD\u0011>6kM\u0017\"6";
               var10004 = 53;
               break;
            case 53:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 55;
               var10003 = "\u001f\u0000x";
               var10004 = 54;
               break;
            case 54:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 56;
               var10003 = "\u0019,WK\u001d),HG\u001al\u0004^C\n90^\u0002\u0016#,~C\r5\u0000RL\u001a%,\\";
               var10004 = 55;
               break;
            case 55:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 57;
               var10003 = "\u0019,WK\u001d),HG\u001al\u0004^C\n90^\u0002\u0016#,~C\r5\u0000RL\u001a%,\\";
               var10004 = 56;
               break;
            case 56:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 58;
               var10003 = "$-Ug\u001f?;yK\u0010(+UE";
               var10004 = 57;
               break;
            case 57:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 59;
               var10003 = "\u00180^L\u001a\u0013\u0001TL\n>-W}-51OG\u0013?\u001dwV\u001a";
               var10004 = 58;
               break;
            case 58:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 60;
               var10003 = "$-Ug\u001f?;yK\u0010(+UE";
               var10004 = 59;
               break;
            case 59:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 61;
               var10003 = "$-Ug\u001f?;yK\u0010(+UE";
               var10004 = 60;
               break;
            case 60:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 62;
               var10003 = "\r.^P\n#,\u0016";
               var10004 = 61;
               break;
            case 61:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 63;
               var10003 = "$-Ug\u001f?;yK\u0010(+UE";
               var10004 = 62;
               break;
            case 62:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 64;
               var10003 = "\u00180^L\u001a";
               var10004 = 63;
               break;
            case 63:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 65;
               var10003 = "\u0004-UG\u0007;'WN";
               var10004 = 64;
               break;
            case 64:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 66;
               var10003 = "$-Ug\u001f?;yK\u0010(+UE";
               var10004 = 65;
               break;
            case 65:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 67;
               var10003 = "$-Ug\u001f?;yK\u0010(+UE";
               var10004 = 66;
               break;
            case 66:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 68;
               var10003 = ".0ZL\u001a";
               var10004 = 67;
               break;
            case 67:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 69;
               var10003 = ".0ZL\u001a\u0005&";
               var10004 = 68;
               break;
            case 68:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 70;
               var10003 = "\u00180RF\u00179/";
               var10004 = 69;
               break;
            case 69:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 71;
               var10003 = "\u00180RF\u00179/";
               var10004 = 70;
               break;
            case 70:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 72;
               var10003 = ";-II\u001c),XJ";
               var10004 = 71;
               break;
            case 71:
               var10001[var10002] = var26;
               z = var10000;
               a = new ArrayList<>();
               a.clear();
               List var2 = a;
               String var3 = "\u001d\fc";
               var10002 = (byte)-1;

               while (true) {
                  char[] var7 = var3.toCharArray();
                  var10004 = var7.length;
                  char[] var12 = var7;
                  var10002 = var10004;

                  for (int var1 = 0; var10002 > var1; var1++) {
                     char var27 = var12[var1];
                     byte var10007;
                     switch (var1 % 5) {
                        case 0:
                           var10007 = 76;
                           break;
                        case 1:
                           var10007 = 66;
                           break;
                        case 2:
                           var10007 = 59;
                           break;
                        case 3:
                           var10007 = 34;
                           break;
                        default:
                           var10007 = 126;
                     }

                     var12[var1] = (char)(var27 ^ var10007);
                  }

                  String var21 = new String(var12).intern();
                  switch (var10002) {
                     case 0:
                        var2.add(var21);
                        return;
                     default:
                        var2.add(var21);
                        var2 = a;
                        var3 = "\u0018\u000boc0";
                        var10002 = (byte)0;
                  }
               }
            default:
               var10001[var10002] = var26;
               var10001 = var10000;
               var10002 = 1;
               var10003 = "$-Ug\u001f?;yK\u0010(+UE";
               var10004 = 0;
         }
      }
   }
}
