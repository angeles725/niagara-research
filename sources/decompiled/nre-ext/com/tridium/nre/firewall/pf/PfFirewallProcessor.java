package com.tridium.nre.firewall.pf;

import com.tridium.nre.firewall.FirewallException;
import com.tridium.nre.firewall.FirewallProcessor;
import com.tridium.nre.firewall.FirewallRule;
import com.tridium.nre.firewall.RedirectRule;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.logging.Level;

public class PfFirewallProcessor extends FirewallProcessor {
   private String pfctlPath;
   private String pfConfPath;

   public PfFirewallProcessor(String pfctlPath, String pfConfPath) {
      this.pfConfPath = pfConfPath;
      this.pfctlPath = pfctlPath;
   }

   @Override
   public String getDescription() {
      return "An OpenBSD pf firewall engine that currently only supports rdr on qnx.";
   }

   @Override
   public FirewallRule validateRule(FirewallRule rule) throws InvalidRuleException {
      switch (rule.getRuleType()) {
         case REDIRECT_RULE:
            RedirectRule oldRdr = (RedirectRule)rule;
            if (oldRdr.getPublicServerPort() < 1024) {
               if (oldRdr.getLocalServerPort() <= 0) {
                  return new RedirectRule(oldRdr.getPublicServerPort(), oldRdr.getPublicServerPort() + 8000, oldRdr.getIpProtocol(), oldRdr.getAdapter(), true);
               }

               return new RedirectRule(oldRdr.getPublicServerPort(), oldRdr.getLocalServerPort(), oldRdr.getIpProtocol(), oldRdr.getAdapter(), true);
            }

            return new RedirectRule(oldRdr.getPublicServerPort(), oldRdr.getPublicServerPort(), oldRdr.getIpProtocol(), oldRdr.getAdapter());
         default:
            throw new InvalidRuleException("unrecognized rule");
      }
   }

   @Override
   public void processRules() {
      this.logger.fine("processing firewall rules");
      synchronized (this.ruleList) {
         StringBuilder out = new StringBuilder();

         for (FirewallRule rule : this.ruleList) {
            switch (rule.getRuleType()) {
               case REDIRECT_RULE:
                  RedirectRule rdrRule = (RedirectRule)rule;
                  if (rdrRule.getPublicServerPort() != rdrRule.getLocalServerPort()) {
                     out.append("rdr pass");
                     if (!rdrRule.getAdapter().equals("any")) {
                        out.append(" on ").append(rdrRule.getAdapter());
                     }

                     out.append(" proto ").append(rdrRule.getIpProtocol());
                     out.append(" from any to self port ")
                        .append(rdrRule.getPublicServerPort())
                        .append(" -> 127.0.0.1 port ")
                        .append(rdrRule.getLocalServerPort())
                        .append("\n");
                     out.append("rdr pass");
                     if (!rdrRule.getAdapter().equals("any")) {
                        out.append(" on ").append(rdrRule.getAdapter());
                     }

                     out.append(" inet6 proto ").append(rdrRule.getIpProtocol());
                     out.append(" from any to self port ")
                        .append(rdrRule.getPublicServerPort())
                        .append(" -> ::1 port ")
                        .append(rdrRule.getLocalServerPort())
                        .append("\n");
                  }
                  break;
               default:
                  this.logger.severe("unrecognized rule: " + rule.getRuleType().name());
            }
         }

         try {
            AccessController.doPrivileged(() -> {
               for (int i1 = 0; i1 <= 3; i1++) {
                  if (i1 > 0) {
                     this.logger.warning("unable to reset firewall, retry #" + i1 + "...");

                     try {
                        Thread.sleep(1000L);
                     } catch (Exception var4x) {
                     }
                  }

                  try {
                     this.doResetFirewall(out.toString());
                     break;
                  } catch (FirewallException fe) {
                     this.logger.log(Level.SEVERE, "exception occurred during firewall reset", fe);
                  }
               }

               return null;
            });
         } catch (PrivilegedActionException pae) {
            this.logger.log(Level.SEVERE, "fatal error occurred during firewall processing", pae);
         }
      }
   }

   private synchronized void doResetFirewall(String cmd) throws Exception {
      File pfConf = new File(this.pfConfPath);

      try (PrintWriter out = new PrintWriter(new FileOutputStream(pfConf))) {
         out.println(cmd);
         out.flush();
      }

      this.logger.fine("resetting packet firewall (pf)");
      this.logger.finer(cmd);
      ProcessBuilder procBuilder = new ProcessBuilder(this.pfctlPath, "-d");
      Process process = procBuilder.start();
      process.waitFor();
      String[] pfCommand = this.logger.isLoggable(Level.FINEST)
         ? new String[]{this.pfctlPath, "-e", "-vv", "-x", "loud", "-f", pfConf.getCanonicalPath()}
         : new String[]{this.pfctlPath, "-e", "-f", pfConf.getCanonicalPath()};
      procBuilder = new ProcessBuilder(pfCommand);
      StringBuilder cmdTxt = new StringBuilder();

      for (String arg : procBuilder.command()) {
         cmdTxt.append(arg).append(" ");
      }

      if (this.logger.isLoggable(Level.FINEST)) {
         this.logger.finest("pf firewall enable cmd : " + cmdTxt);
      }

      procBuilder.redirectErrorStream(true);
      process = procBuilder.start();
      PfFirewallProcessor.StreamGobbler procOut = new PfFirewallProcessor.StreamGobbler(process.getInputStream(), "pf_firewall");
      procOut.start();
      process.waitFor();
      int exitValue = process.exitValue();
      if (exitValue != 0) {
         this.logger.severe("error processing pf conf file at '" + pfConf + "'");
         this.logger.severe(cmd);
         if (pfConf.exists() && !pfConf.delete()) {
            this.logger.warning("failed to delete existing firewall configuration at '" + pfConf + "'");
         }

         throw new FirewallException("error processing pf conf file at '" + pfConf + "'");
      }
   }

   class StreamGobbler extends Thread {
      InputStream is;
      String type;

      StreamGobbler(InputStream is, String type) {
         this.is = is;
         this.type = type;
      }

      @Override
      public void run() {
         try {
            InputStreamReader isr = new InputStreamReader(this.is);
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
               if (PfFirewallProcessor.this.logger.isLoggable(Level.FINEST)) {
                  PfFirewallProcessor.this.logger.finest(this.type + " -> " + line);
               }
            }
         } catch (IOException ioe) {
            PfFirewallProcessor.this.logger.log(Level.SEVERE, "failed to process firewall input stream", ioe);
         }
      }
   }
}
