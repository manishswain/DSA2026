package Playground;

public class GenericName<E> {
    private String data;

    GenericName(E e) {
        this.data = e.toString();
    }

    String GenericsMethod(E e) {
        System.out.println(e.toString());
        return e.toString();
    }

    public static void main(String[] args) {
        GenericName<String> stringGenericsName = new GenericName<>("Hi! Hello");
        stringGenericsName.GenericsMethod("Hello");
        GenericName<Number> integerGenericsName = new GenericName<>(25);
        integerGenericsName.GenericsMethod(25);
        GenericName<Float> floatGenericsName = new GenericName<>(22f);
        floatGenericsName.GenericsMethod(22f);
        GenericName<Boolean> booleanGenericsName = new GenericName<>(true);
        booleanGenericsName.GenericsMethod(true);
    }
}
