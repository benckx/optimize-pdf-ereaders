package be.enceladus.scan2ereader.core.utils;

public class Counter {

    private float value;

    public void increment() {
        value++;
    }

    public void increment(float value) {
        this.value += value;
    }

    public float getValue() {
        return value;
    }

}
