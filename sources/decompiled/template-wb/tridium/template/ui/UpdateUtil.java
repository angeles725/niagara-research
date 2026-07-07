package com.tridium.template.ui;

import com.tridium.sys.transfer.DeployToComp;
import com.tridium.template.TemplateConst;
import com.tridium.template.ui.file.BWbDeployableNtplFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.file.BDirectory;
import javax.baja.file.FilePath;
import javax.baja.file.BIDeployable.Step;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.UnresolvedException;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;

class UpdateUtil implements TemplateConst {
   public static final Logger log = Logger.getLogger("ntpl");

   static boolean updateNtplFile(BWbDeployableNtplFile ntplFile, BComponent target) {
      try {
         Step[] transferSteps = ntplFile.makeNtplTransferStep(target);

         for (Step transferStep : transferSteps) {
            if (transferStep != null) {
               BObject tgt = getTarget(transferStep.destination, target);
               transferStep.mark.copyTo(tgt, new BComponent(), DeployToComp.NoPostLink);
            }
         }

         return true;
      } catch (Exception var8) {
         log.log(Level.WARNING, "Error updating template file " + ntplFile.getFileName(), (Throwable)var8);
         return false;
      }
   }

   private static BObject getTarget(BOrd ord, BObject target) throws Exception {
      BObject obj;
      try {
         obj = ord.resolve(target).get();
      } catch (UnresolvedException var11) {
         OrdQuery[] oqs = ord.parse();

         for (OrdQuery oq : oqs) {
            if (oq instanceof FilePath) {
               FilePath fp = (FilePath)oq;
               BDirectory rootDir = (BDirectory)BOrd.make("file:^").resolve(target).get();
               rootDir.getFileSpace().makeDir(fp);
            }
         }

         obj = ord.resolve(target).get();
      }

      return obj;
   }
}
