/*
 * SP-G10a Java agent — "license mirror" without Frida (ASM 9.6 from the
 * install's bin/ext/asm-9.6.jar).
 *
 * premain(String args, Instrumentation): mode = "log" | "force".
 *   log   — passive observer: the registered transformer logs when
 *           com/tridium/sys/license/LicenseUtil is defined (and leaves its
 *           bytes untouched).
 *   force — the same transformer REWRITES every public static
 *           boolean verify(...) on LicenseUtil to `return true`, at class
 *           DEFINITION time (so the verifier is born truthful-to-us).
 *
 * Reimplemented observer only (METHODOLOGY §19); instruments the installed
 * class in a DISPOSABLE nre.exe; redistributes nothing.
 */
package spg10;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class LicenseMirrorAgent {

    static boolean FORCE = false;

    public static void premain(String args, Instrumentation inst) {
        FORCE = "force".equals(args);
        System.err.println("[spg10-agent] premain mode=" + (FORCE ? "force" : "log") +
            " retransform=" + inst.isRetransformClassesSupported());
        inst.addTransformer(new LicenseTransformer(), true);
        System.err.println("[spg10-agent] transformer registered (class-definition-time)");
    }

    static final class LicenseTransformer implements ClassFileTransformer {
        public byte[] transform(ClassLoader loader, String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain pd,
                                byte[] classfileBuffer) throws IllegalClassFormatException {
            if (!"com/tridium/sys/license/LicenseUtil".equals(className)) {
                return null;
            }
            System.err.println("[spg10-agent] LicenseUtil transform hook: " + className +
                " loader=" + (loader == null ? "bootstrap" : loader.getClass().getName()) +
                " redef=" + (classBeingRedefined != null));
            if (!FORCE) {
                System.err.println("[spg10-agent] log mode: passthrough, no rewrite");
                return null; // observer only
            }
            try {
                org.objectweb.asm.ClassReader cr = new org.objectweb.asm.ClassReader(classfileBuffer);
                // COMPUTE_FRAMES recomputes the StackMapTable the JVM verifier demands
                org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(cr,
                        org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
                org.objectweb.asm.ClassVisitor cv = new org.objectweb.asm.ClassVisitor(
                        org.objectweb.asm.Opcodes.ASM9, cw) {
                    @Override
                    public org.objectweb.asm.MethodVisitor visitMethod(
                            int access, String name, String desc, String sig, String[] ex) {
                        org.objectweb.asm.MethodVisitor mv = super.visitMethod(access, name, desc, sig, ex);
                        if (name.equals("verify") && desc.startsWith("([B") && desc.endsWith(")Z")) {
                            System.err.println("[spg10-agent] rewriting verify " + name + desc);
                            return new org.objectweb.asm.MethodVisitor(
                                    org.objectweb.asm.Opcodes.ASM9, mv) {
                                @Override
                                public void visitCode() {
                                    super.visitCode();
                                    super.visitInsn(org.objectweb.asm.Opcodes.ICONST_1);
                                    super.visitInsn(org.objectweb.asm.Opcodes.IRETURN);
                                    super.visitMaxs(1, 1);
                                    super.visitEnd();
                                }
                            };
                        }
                        return mv;
                    }
                };
                cr.accept(cv, 0);
                System.err.println("[spg10-agent] LicenseUtil rewritten (" +
                    classfileBuffer.length + " -> " + cw.toByteArray().length + " bytes)");
                return cw.toByteArray();
            } catch (Throwable t) {
                System.err.println("[spg10-agent] ASM rewrite failed: " + t);
                return null;
            }
        }
    }
}
