package uk.org.freedonia.deserializationdemo;

public class AnotherClass {

    private String someData;
    private int someNumber;

    public AnotherClass(String someData, int someNumber) {
        this.someData = someData;
        this.someNumber = someNumber;
    }

    public String getSomeData() {
        return someData;
    }

    public void setSomeData(String someData) {
        this.someData = someData;
    }

    public int getSomeNumber() {
        return someNumber;
    }

    public void setSomeNumber(int someNumber) {
        this.someNumber = someNumber;
    }

    @Override
    public String toString() {
        return "AnotherClass{" +
                "someData='" + someData + '\'' +
                ", someNumber=" + someNumber +
                '}';
    }
}
