package org.slipenk.entity;

import java.util.Objects;

import static org.slipenk.dictionary.Dictionary.SIZE;

public class Data {

    private String operation;
    private int price;
    private int size;
    private String type;

    public Data(String operation, int price, int size, String type) {
        this.operation = operation;
        this.price = price;
        this.size = size;
        this.type = type;
    }

    public Data(String operation, String type) {
        this.operation = operation;
        this.type = type;
    }

    public Data(String operation, int temp, String type) {
        if (Objects.equals(type, SIZE)) {
            this.price = temp;
        } else {
            this.size = temp;
        }
        this.operation = operation;
        this.type = type;
    }

    public Data() {
    }

    public String getOperation() {
        return operation;
    }

    public int getPrice() {
        return price;
    }

    public int getSize() {
        return size;
    }

    public String getType() {
        return type;
    }

}
