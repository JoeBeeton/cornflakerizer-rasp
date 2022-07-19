package uk.org.freedonia.deserializationdemo;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.org.freedonia.serializer.FilteredObjectInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

@RestController
public class Controller {


    /**
     * Payload is
     * rO0ABXNyABxjb20uZXhhbXBsZS5kZW1vLlByb2NCdWlsZGVynZqx242Ae0UCAAFMAANjbWR0ABJMamF2YS9sYW5nL1N0cmluZzt4cHQAAmxz
     * which executes a ls
     * PayloadGeneratorTest contains a method for generating the above payload and can be modified to change the executed command
     * @param messageBody
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @PostMapping("/poc-java-two")
    public String doRequestJavaTwo(@RequestBody String messageBody) throws IOException, ClassNotFoundException {
        byte[] data = Base64.decodeBase64(messageBody);
        ObjectInputStream ois = new FilteredObjectInputStream(new ByteArrayInputStream(data),AnotherClass.class);
        AnotherClass object = (AnotherClass) ois.readObject();
        return object.toString();
    }

    /**
     * Payload is
     * rO0ABXNyABxjb20uZXhhbXBsZS5kZW1vLlByb2NCdWlsZGVynZqx242Ae0UCAAFMAANjbWR0ABJMamF2YS9sYW5nL1N0cmluZzt4cHQAAmxz
     * which executes a ls
     * PayloadGeneratorTest contains a method for generating the above payload and can be modified to change the executed command
     * @param messageBody
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @PostMapping("/poc-java")
    public String doRequestJava(@RequestBody String messageBody) throws IOException, ClassNotFoundException {
        byte[] data = Base64.decodeBase64(messageBody);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        AnotherClass object = (AnotherClass) ois.readObject();
        return object.toString();
    }



}
