package org.slipenk;

import org.slipenk.management.OrderManagement;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) {
        Logger logger = Logger.getGlobal();
        try {
            OrderManagement orderManagement = new OrderManagement();
            orderManagement.getJob();
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }
}
