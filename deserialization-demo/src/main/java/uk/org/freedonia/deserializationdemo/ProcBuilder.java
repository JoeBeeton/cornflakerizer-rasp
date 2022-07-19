package uk.org.freedonia.deserializationdemo;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class ProcBuilder implements Serializable {

    private String cmd;

    public String getCmd() {
        return cmd;
    }

    public void addCommandInNotBeanStandardWay(String string ){
        cmd = string;
    }


    public void setCmd(String cmd) {
        this.cmd = cmd;
        try {
            Process proc = Runtime.getRuntime().exec(cmd);
            System.out.println(new String(IOUtils.toByteArray(proc.getInputStream())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        setCmd(cmd);
    }
}
