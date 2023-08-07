package org.slipenk.management;

import org.slipenk.exceptions.OrderException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Predicate;

import static org.slipenk.dictionary.Dictionary.ASK;
import static org.slipenk.dictionary.Dictionary.BEST_ASK;
import static org.slipenk.dictionary.Dictionary.BEST_BID;
import static org.slipenk.dictionary.Dictionary.BID;
import static org.slipenk.dictionary.Dictionary.BUY;
import static org.slipenk.dictionary.Dictionary.COMMA;
import static org.slipenk.dictionary.Dictionary.FILE_INPUT_NAME;
import static org.slipenk.dictionary.Dictionary.FILE_OUTPUT_NAME;
import static org.slipenk.dictionary.Dictionary.SELL;
import static org.slipenk.dictionary.Dictionary.SIZE;
import static org.slipenk.operation.Operation.O;
import static org.slipenk.operation.Operation.Q;
import static org.slipenk.operation.Operation.U;

public class OrderManagement {

    private static final String ERROR_MESSAGE = "Problem with data. Check the input file.";
    private final TreeMap<Integer, Integer> askMap = new TreeMap<>();
    private final TreeMap<Integer, Integer> bidMap = new TreeMap<>();
    private final StringBuilder outputBuilder = new StringBuilder();

    public void getJob() throws IOException {

        List<Predicate<String[]>> predicates = Arrays.asList(
                this::updateBid,
                this::updateAsk,
                this::bestBid,
                this::bestAsk,
                this::getSize,
                this::removeAsk,
                this::removeBid
        );

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_INPUT_NAME))) {
            for (String line; (line = br.readLine()) != null; ) {
                String[] data = convertIntoArray(line);
                for (Predicate<String[]> predicate : predicates) {
                    if (predicate.test(data)) {
                        break;
                    }
                }
            }
        }

        Files.write(Path.of(FILE_OUTPUT_NAME), outputBuilder.toString().getBytes());
    }

    private boolean updateBid(String[] items) {
        return updateOffers(items, BID, askMap, bidMap);
    }

    private boolean updateAsk(String[] items) {
        return updateOffers(items, ASK, bidMap, askMap);
    }

    private boolean updateOffers(String[] items, String operation, Map<Integer, Integer> firstMap, Map<Integer, Integer> secondMap) {
        if (items[0].equals(U.name().toLowerCase())) {
            int price = Integer.parseInt(items[1]);
            if (items[3].equals(operation) && firstMap.get(price) == null) {
                secondMap.put(price, Integer.parseInt(items[2]));
                return true;
            }
        }
        return false;
    }

    private boolean bestBid(String[] items) {
        if (items[1].equals(BEST_BID) && items[0].equals(Q.name().toLowerCase())) {
            writeToFileBestAskBid(getBestBid(), bidMap);
            return true;
        }
        return false;
    }

    private boolean bestAsk(String[] items) {
        if (items[1].equals(BEST_ASK) && items[0].equals(Q.name().toLowerCase())) {
            writeToFileBestAskBid(getBestAsk(), askMap);
            return true;
        }
        return false;
    }

    private void writeToFileBestAskBid(Integer value, Map<Integer, Integer> map) {
        outputBuilder.append(value);
        outputBuilder.append(COMMA);
        outputBuilder.append(map.get(value));
        outputBuilder.append(System.lineSeparator());
    }

    private boolean getSize(String[] items) {
        if (items[1].equals(SIZE) && items[0].equals(Q.name().toLowerCase())) {
            Integer askValue = askMap.get(Integer.parseInt(items[2]));
            if (askValue != null) {
                writeSize(askValue);
            }
            Integer bidValue = bidMap.get(Integer.parseInt(items[2]));
            if (bidValue != null) {
                writeSize(bidValue);
            }
            if (askValue == null && bidValue == null) {
                writeSize(0);
            }
            return true;
        }
        return false;
    }

    private void writeSize(Integer value) {
        outputBuilder.append(value);
        outputBuilder.append(System.lineSeparator());
    }

    private boolean removeAsk(String[] items) {
        if (items[1].equals(BUY) && items[0].equals(O.name().toLowerCase())) {
            int result = Integer.parseInt(items[2]);
            do {
                result = getOrders(Math.abs(result), getBestAsk(), askMap);
            } while (result < 0);
            return true;
        }
        return false;
    }

    private boolean removeBid(String[] items) {
        if (items[1].equals(SELL) && items[0].equals(O.name().toLowerCase())) {
            int result = Integer.parseInt(items[2]);
            do {
                result = getOrders(Math.abs(result), getBestBid(), bidMap);
            } while (result < 0);
            return true;
        }
        return false;
    }

    private int getOrders(int orderSize, int bestOffer, Map<Integer, Integer> map) {
        int result = map.get(bestOffer) - orderSize;
        map.replace(bestOffer, Math.max(result, 0));
        return result;
    }

    private String[] convertIntoArray(String inputData) {
        return inputData.split(COMMA);
    }

    private Integer getBestAsk() {
        for (Map.Entry<Integer, Integer> entry : askMap.entrySet()) {
            if (entry.getValue() != 0) {
                return entry.getKey();
            }
        }

        throw new OrderException(ERROR_MESSAGE);
    }

    private Integer getBestBid() {
        NavigableMap<Integer, Integer> descendingBidMap = bidMap.descendingMap();

        for (Map.Entry<Integer, Integer> entry : descendingBidMap.entrySet()) {
            if (entry.getValue() != 0) {
                return entry.getKey();
            }
        }

        throw new OrderException(ERROR_MESSAGE);
    }
}
