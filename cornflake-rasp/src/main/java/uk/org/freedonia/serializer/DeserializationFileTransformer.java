package uk.org.freedonia.serializer;


import com.google.common.primitives.Bytes;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.H_PUTFIELD;
import static org.objectweb.asm.Opcodes.H_PUTSTATIC;

public class DeserializationFileTransformer implements ClassFileTransformer {


    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)  {
        // checks if the class contains a ObjectInputStream
        if(isDeserializationUsedInClass(classfileBuffer)&&ignoreJavaBaseClasses(className)) {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            CLSVisitor adapter = new CLSVisitor(className,writer);
            reader.accept(adapter, ClassReader.EXPAND_FRAMES);
            byte[] classData = writer.toByteArray();
            try {
                Files.write(Paths.get("/tmp/Controller.class"),classData);
            } catch (IOException e) {
                    e.printStackTrace();
            }
            return classData;
        } else {
            return classfileBuffer;
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
                ListIterator<AbstractInsnNode> itr = instructions.iterator();
                List<AbstractInsnNode> ndeList = new ArrayList<>();
                while(itr.hasNext()) {
                    ndeList.add(itr.next());
                }
                Optional<MethodInsnNode> readObject = isReadObjectCalled(ndeList);
                // check to see if a call to ObjectInputStream.readObject() occurs within this method
                if(readObject.isPresent()) {
                    MethodInsnNode readObjectCall = readObject.get();
                    // check to see if the next instruction after readObject() is a Cast
                    // if it isn't this isnt going to work!!!
                    if(readObjectCall.getNext().getOpcode()==Opcodes.CHECKCAST && readObjectCall.getNext() instanceof TypeInsnNode) {
                        TypeInsnNode castObject = (TypeInsnNode) readObjectCall.getNext();
                        String classCastTo = castObject.desc;
                        System.out.println(classCastTo);
                        Optional<MethodInsnNode> constructorOpt = getObjectInputStreamConstructor(ndeList);
                        if(constructorOpt.isPresent()) {
                            MethodInsnNode constructor = constructorOpt.get();
                            Optional<TypeInsnNode> type = getObjectInputStreamType(ndeList);
                            // Change the ObjectInputStream to a FilteredObjectInputStream
                            if(type.isPresent()) {
                                type.get().desc = "uk/org/freedonia/serializer/FilteredObjectInputStream";
                            }
                            // The Constructor instruction needs to change to FilteredObjectInputStream.
                            constructor.owner = "uk/org/freedonia/serializer/FilteredObjectInputStream";
                            // As the constructor signature is different, we need to add the array of classes to the arguments.
                            constructor.desc = "(Ljava/io/InputStream;[Ljava/lang/Class;)V";
                            constructor.name = "<init>";
                            // Create a list of instructions. This will become the second half of the contructor args.
                            // From "new ObjectInputStream(inputStream)"
                            // to "new FilteredObjectInputStream(inputStream, new Class[]{NameOfClass.class})"
                            InsnList insertList = new InsnList();
                            insertList.add(new InsnNode(H_PUTSTATIC));
                            insertList.add(new TypeInsnNode(ANEWARRAY,"java/lang/Class"));
                            insertList.add(new InsnNode(DUP));
                            insertList.add(new InsnNode(H_PUTFIELD));
                            insertList.add(new LdcInsnNode(Type.getType("L"+classCastTo+";")));
                            insertList.add(new InsnNode(AASTORE));
                            // This gets inserted just before the constructor. ( which is after the list of arguments to the constructor )
                            instructions.insertBefore(constructor,insertList);
                        }
                    }
                }
                accept(mv);
            }

            private Optional<MethodInsnNode> getObjectInputStreamConstructor(List<AbstractInsnNode> ndeList) {
                return ndeList.stream()
                        .filter(node-> node instanceof MethodInsnNode)
                        .map( node -> (MethodInsnNode)node )
                        .filter( node -> "java/io/ObjectInputStream".equals(node.owner))
                        .filter( node -> "<init>".equals(node.name)).findFirst();
            }

            private Optional<TypeInsnNode> getObjectInputStreamType(List<AbstractInsnNode> ndeList) {
                return ndeList.stream()
                        .filter(node-> node instanceof TypeInsnNode)
                        .map( node -> (TypeInsnNode)node )
                        .filter( node -> "java/io/ObjectInputStream".equals(node.desc))
                        .findFirst();
            }

            private Optional<MethodInsnNode> isReadObjectCalled(List<AbstractInsnNode> ndeList ) {
                return ndeList.stream()
                        .filter(node-> node instanceof MethodInsnNode)
                        .map( node -> (MethodInsnNode)node )
                        .filter( node -> "java/io/ObjectInputStream".equals(node.owner))
                        .filter( node -> "readObject".equals(node.name)).findFirst();
            }

        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            return new MethodAdapter(access, name, desc, signature, exceptions, mv);
        }
    }


    private boolean ignoreJavaBaseClasses(String className) {
        if(className!=null && (className.startsWith("java/") || className.startsWith("sun/"))) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isDeserializationUsedInClass(byte[] classBytes) {
        byte[] search = "ObjectInputStream".getBytes(StandardCharsets.UTF_8);
        if(Bytes.indexOf(classBytes,search) == -1 ) {
            return false;
        }
        return true;
    }


}
