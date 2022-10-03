package uk.org.freedonia.serializer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

public class Log4JFileTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)  {
        if((className!=null&&className.equals("org/apache/logging/log4j/core/lookup/JndiLookup"))) {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            CLSVisitor adapter = new CLSVisitor(className,writer);
            reader.accept(adapter, ClassReader.EXPAND_FRAMES);
            byte[] classData = writer.toByteArray();
            writeControllerClassToDisk(classData);
            return classData;
        }
        return classfileBuffer;
    }

    private void writeControllerClassToDisk(byte[] data) {
            try {
                Files.write(Paths.get("/tmp/JndiLookup.class"), data);
            } catch (IOException e) {
                e.printStackTrace();
            }

    }




    private class CLSVisitor extends ClassVisitor {

        private final String className;

        public CLSVisitor(String className, ClassWriter writer) {
            super(Opcodes.ASM9,writer);
            this.className = className;
        }

        public CLSVisitor(String className, ClassVisitor classVisitor) {
            super(Opcodes.ASM9,classVisitor);
            this.className = className;
        }

        private class MethodAdapter extends MethodNode {

            public MethodAdapter(int access, String name, String desc,
                                 String signature, String[] exceptions, MethodVisitor mv) {
                super(Opcodes.ASM9, access, name, desc, signature, exceptions);
                this.mv = mv;
            }

            @Override
            public void visitEnd() {
                if(name!=null&&name.equals("lookup")) {
                    instructions.clear();
                    InsnList insertList = new InsnList();
                    insertList.add(new InsnNode(Opcodes.ACONST_NULL));
                    insertList.add(new InsnNode(Opcodes.ARETURN));
                    instructions.insert(insertList);
                }
                accept(mv);
            }
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            return new MethodAdapter(access, name, desc, signature, exceptions, mv);
        }
    }



}
