/*
 * SP-G9a observer agent — live Security.getProviders() order + which provider
 * owns "SHA1withDSA"/"DSA" Signature, to confirm the effective provider order
 * against bin/policy/java.security (B441).
 *
 * premain("providers"): schedule a daemon thread that, once the Niagara
 * bootstrap has set up the JVM security config, prints:
 *   [providers] 1: <name> <version>
 *   ...
 *   [dsa-signature] SHA1withDSA -> <provider-class-name>
 *   [dsa-signature] DSA        -> <provider-class-name>
 */
package spg10;

import java.lang.instrument.Instrumentation;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;

public class ProviderOrderProbe {

    public static void premain(String args, Instrumentation inst) {
        System.err.println("[g9a-probe] premain; scheduling provider dump");
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    // give the bootstrap time to set -Djava.security.properties
                    Thread.sleep(4000);
                    System.err.println("[g9a-probe] === Security.getProviders() effective order ===");
                    int i = 0;
                    for (Provider p : Security.getProviders()) {
                        i++;
                        System.err.println("[g9a-probe] " + i + ": " + p.getName() +
                            " " + p.getVersion() + "  (" + p.getClass().getName() + ")");
                    }
                    System.err.println("[g9a-probe] === Signature 'DSA' resolution ===");
                    for (String alg : new String[]{"DSA", "SHA1withDSA", "SHA256withDSA"}) {
                        try {
                            Signature s = Signature.getInstance(alg);
                            System.err.println("[g9a-probe] " + alg + " -> " +
                                s.getProvider().getName() + " (" + s.getProvider().getClass().getName() + ")");
                        } catch (Throwable t2) {
                            System.err.println("[g9a-probe] " + alg + " -> FAIL: " + t2);
                        }
                    }
                    System.err.println("[g9a-probe] === done ===");
                } catch (Throwable t) {
                    System.err.println("[g9a-probe] dump failed: " + t);
                }
            }
        }, "g9a-provider-probe");
        t.setDaemon(true);
        t.start();
    }
}
