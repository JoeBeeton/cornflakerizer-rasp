# Cornflakerizer_Java8
## Versions
Cornflakerizer relies on the ObjectInputStream filtering added by JEP-290, which is in Java 9. 
It was backported to Java 8 update 121 and so Cornflakerizer works.
However the way it was backported means there are differences between the Java 8 and Java 9+ version.
In Java 8 ObjectInputFilter is under sun.misc.ObjectInputFilter compared to Java 9+ which has it under java.io.ObjectInputFilter.

Also the way the ObjectInputFilter is injected into the ObjectInputStream class is different, in Java 9 a setter method was added to ObjectInputStream.
in Java 8 a call to sun.misc.ObjectInputFilter.Config.setObjectInputFilter(ObjectInputStream var0, ObjectInputFilter var1)
sets the stream's filter.
### The "fix"
So the fix is to create two versions of Cornflakerizer, Cornflakerizer_Java8 and Cornflakerizer. With Cornflakerizer ued for Java9+ and Cornflakerizer_Java8 used for... well you get the idea.
##Why
Cornflakerizer is a tool to improve the security of using Java's Serialization functionality. It builds on the serialization whitelisting introduced in JEP-290, which works but is difficult to configure and maintain. 
As it requires the developer to create and maintain a list of acceptable classes via a regex in a properties file/system property. This "works" but in reality is not a maintainable solution as any change to the data being deserialized would require the developer to remember to update the jdk.serialFilter list. And for a large application that list would be massive. 
##How
Cornflakerizer builds on the whitelisting technology in JEP-290 by automatically generating the whitelist based on what the type of expected dersialized classes.
This should block Java serialization attacks by ensuring that only the expected class is deserialized rather than a gadget class with a malicious payload.
For example rather than using :

`ObjectInputStream ois = new ObjectInputStream(dataStream);
 String deserializedString = (String) ois.readObject();
`
You call
`ObjectInputStream ois = new FilteredObjectInputStream(dataStream,String.class);
 String deserializedString = (String) ois.readObject();
`
By specifying the class we are expecting to be returned, Cornflakerizer builds a whitelist. In this case a whitelist of just String.class.

For more complex data structures, for example :
`public class SomePojo implements Serializable {
    private String val1;
    private BigDecimal val2;
    private AnotherPojo val3;
}
`
`public class AnotherPojo implements Serializable {
    private byte[] val1;
    private int val2;
    private Color val3;
}`

Then calling
`ObjectInputStream ois = new FilteredObjectInputStream(dataStream,SomePojo.class);
 SomePojo deserializedPojo = (SomePojo) ois.readObject();
`
Will generate a whitelist of SamplePojo, BigDecimal, AnotherPojo, Color, String. ( Primitive values are autowhitelisted ).
This is a "best endeavour", due to type erasure it is not always possible to know with certainty what classes are expected.
Or if for example the SamplePojo contained a variable of 
`private Serializable val1` or 
`private Object val2`
It would not be able to safely whitelist the serializable interface or the base Object class and therefore it wont.
However where the class being deserialized contains values like that, it is possible to add the list of expected classes when constructing the FilteredObjectInputStream.
`ObjectInputStream ois = new FilteredObjectInputStream(dataStream,SomePojo.class,SomeOther.class,AndAnother.class,YouGetTheIdea.class);`


###### What this does not protect against
This does not effectively protect against the billion laughs style denial of service attack as this only checks that the classes are what we expect. Rather than checking for things like maxdepth, array size, instance count etc.

###### Don't use this in new projects!
Where you are building a new system that requires deserialization functionality, I strongly urge you not to use Java Serialization, even with Cornflakerizer.
There are far better solutions out there. Cornflakerizer was designed to quickly add some level of protection to existing applications where removing Java serialization is not possible.