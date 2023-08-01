package org.slipenk.exceptions;

public class OrderException extends RuntimeException {

    public OrderException(String errorMessage) {
        super(errorMessage);
    }
}
