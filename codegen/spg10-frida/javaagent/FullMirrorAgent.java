/*
 * FullMirrorAgent — the COMPLETE license mirror: rewrites BOTH
 *   com.tridium.sys.license.LicenseUtil.verify(...)            -> return true
 *   NodeLockedLicenseManager.isLicenseHostIdValid() (inner)    -> return true
 * so a wrong-host, tampered, or even absent-signature license reports {valid}
 * on a disposable nre.exe. Defensive-reimpl observer (METHODOLOGY §19);
 * instruments the installed class in a DISPOSABLE process, redistributes nothing.
 *
 * Uses ASM 9.6 from the install's own bin/ext/asm-9.6.jar.
 */
package spg10;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class FullMirrorAgent {

    public static void premain(String args, Instrumentation inst) {
        final boolean force = "force".equals(args);
        System.err.println("[full-mirror] premain mode=" + (force ? "force" : "log"));
        if (!force) {
            return;
        }
        inst.addTransformer(new FullMirrorTransformer(), true);
        System.err.println("[full-mirror] transformer registered (class-definition-time)");
    }

    static final class FullMirrorTransformer implements ClassFileTransformer {
        public byte[] transform(ClassLoader loader, String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain pd,
                                byte[] classfileBuffer) throws IllegalClassFormatException {
            // Rewrite LicenseUtil.verify(...) -> return true
            if ("com/tridium/sys/license/LicenseUtil".equals(className)) {
                return rewriteVerifyTrue(classfileBuffer);
            }
            // Rewrite NodeLockedLicenseManager$1.isLicenseHostIdValid() -> return true
            if (className != null &&
                (className.equals("com/tridium/sys/license/NodeLockedLicenseManager$1") ||
                 className.equals("com/tridium/sys/license/NodeLockedLicenseManager$NodeLockedLicense") ||
                 className.startsWith("com/tridium/sys/license/NodeLockedLicense"))) {
                return rewriteHostIdTrue(classfileBuffer);
            }
            return null;
        }

        private byte[] rewriteVerifyTrue(byte[] bytes) {
            try {
                org.objectweb.asm.ClassReader cr = new org.objectweb.asm.ClassReader(bytes);
                org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(cr,
                        org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
                org.objectweb.asm.ClassVisitor cv = new org.objectweb.asm.ClassVisitor(
                        org.objectweb.asm.Opcodes.ASM9, cw) {
                    @Override
                    public org.objectweb.asm.MethodVisitor visitMethod(
                            int access, String name, String desc, String sig, String[] ex) {
                        org.objectweb.asm.MethodVisitor mv = super.visitMethod(access, name, desc, sig, ex);
                        if (name.equals("verify") && desc.startsWith("([B") && desc.endsWith(")Z")) {
                            System.err.println("[full-mirror] rewriting verify " + name + desc);
                            return new org.objectweb.asm.MethodVisitor(
                                    org.objectweb.asm.Opcodes.ASM9, mv) {
                                @Override public void visitCode() {
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
                System.err.println("[full-mirror] LicenseUtil rewritten (" + bytes.length + " -> " + cw.toByteArray().length + ")");
                return cw.toByteArray();
            } catch (Throwable t) {
                System.err.println("[full-mirror] LicenseUtil rewrite failed: " + t);
                return null;
            }
        }

        private byte[] rewriteHostIdTrue(byte[] bytes) {
            try {
                org.objectweb.asm.ClassReader cr = new org.objectweb.asm.ClassReader(bytes);
                org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(cr,
                        org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
                org.objectweb.asm.ClassVisitor cv = new org.objectweb.asm.ClassVisitor(
                        org.objectweb.asm.Opcodes.ASM9, cw) {
                    @Override
                    public org.objectweb.asm.MethodVisitor visitMethod(
                            int access, String name, String desc, String sig, String[] ex) {
                        org.objectweb.asm.MethodVisitor mv = super.visitMethod(access, name, desc, sig, ex);
                        if (name.equals("isLicenseHostIdValid") && desc.equals("()Z")) {
                            System.err.println("[full-mirror] rewriting isLicenseHostIdValid " + name + desc);
                            return new org.objectweb.asm.MethodVisitor(
                                    org.objectweb.asm.Opcodes.ASM9, mv) {
                                @Override public void visitCode() {
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
                System.err.println("[full-mirror] HostId class rewritten (" + bytes.length + " -> " + cw.toByteArray().length + ")");
                return cw.toByteArray();
            } catch (Throwable t) {
                System.err.println("[full-mirror] HostId rewrite failed: " + t);
                return null;
            }
        }
    }
}
