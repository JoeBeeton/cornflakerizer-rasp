package uk.org.freedonia.serializer;



import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

public class Rasp {

    public static void main(String[] args) throws Exception{
        if(args.length>0) {
            String pid = args[0];
            prepareAttach();
            System.out.println("attaching to jvm " + pid);
            VirtualMachine jvm = VirtualMachine.attach(pid);
            System.out.println("Agent jar path : " + getAgentPath().getAbsolutePath());
            jvm.loadAgent(getAgentPath().getAbsolutePath());
            System.out.println("agent loaded " + pid);

            jvm.detach();
            System.out.println("detached " + pid);

        }


    }

    private static File getAgentPath() throws URISyntaxException {
        return new File(Rasp.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()
                .getPath());
    }

    private static void prepareAttach() throws NoSuchMethodException, MalformedURLException, InvocationTargetException, IllegalAccessException {
        String binPath = System.getProperty("sun.boot.library.path");
        // remove jre/bin, replace with lib
        String libPath = binPath.substring(0, binPath.length() - 7) + "lib";
        URLClassLoader loader = (URLClassLoader) Rasp.class.getClassLoader();
        Method addURLMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addURLMethod.setAccessible(true);
        File toolsJar = new File(libPath + "/tools.jar");
        if (!toolsJar.exists()) throw new RuntimeException(toolsJar.getAbsolutePath() + " does not exist");
        addURLMethod.invoke(loader, new File(libPath + "/tools.jar").toURI().toURL());
    }




    public static void premain(String args, Instrumentation inst) {
        transform( inst );
    }

    public static void agentmain(String args, Instrumentation inst) {
        transform( inst );
        try {
            for(Class cls : inst.getAllLoadedClasses()) {
                System.out.println(cls.getName());
                if(cls != null && ignoreJavaBaseClasses(cls.getName())&&inst.isModifiableClass(cls)) {
                    inst.retransformClasses(cls);
                }
            }
        } catch (UnmodifiableClassException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Finished transformation");

    }

    private static boolean ignoreJavaBaseClasses(String className) {
        if(className!=null && (className.startsWith("java.") || className.startsWith("sun.")
                || className.contains("$") || className.startsWith("com.sun")
                || className.startsWith("null") || className.contains("Initializer")
                || className.contains("Commons") || className.contains("autoconfigure")
                || className.contains("springframework"))) {
            return false;
        } else {
            return true;
        }
    }

    private static void transform(Instrumentation inst) {
        // Adds our ClassFileTransformer implementation
        inst.addTransformer(new DeserializationFileTransformer(),true);
    }

}
