/**
 * HelloWorld.java
 */

public class HelloWorld
{
    private Bonobo none;

    public TestType dummy(Foo f) {
        Bar b = new Bar(f);
        return (TestType)b;
    }

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}