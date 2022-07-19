package uk.org.freedonia.serializer;



import java.lang.instrument.Instrumentation;

public class Rasp {

    public static void premain(String args, Instrumentation inst) {
        transform( inst );
    }

    public static void agentmain(String args, Instrumentation inst) {
        transform( inst );
    }

    private static void transform(Instrumentation inst) {
        inst.addTransformer(new DeserializationFileTransformer(),false);
    }

}
