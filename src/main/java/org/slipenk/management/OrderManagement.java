package org.slipenk.management;

import org.slipenk.entity.Data;
import org.slipenk.exceptions.OrderException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final int MAX_STRING_BUILDER_SIZE = 500000;

    private final Map<Integer, Integer> askMap = new HashMap<>();
    private final Map<Integer, Integer> bidMap = new HashMap<>();
    StringBuilder output = new StringBuilder();
    BufferedWriter writer;

    public void getJob() throws IOException {
        writer = new BufferedWriter(new FileWriter(FILE_OUTPUT_NAME));

        List<Predicate<Data>> predicates = Arrays.asList(
                this::updateBid,
                this::updateAsk,
                lines1 -> {
                    try {
                        return bestBid(lines1);
                    } catch (IOException e) {
                        throw new OrderException(e.getMessage());
                    }
                },
                lines2 -> {
                    try {
                        return bestAsk(lines2);
                    } catch (IOException e) {
                        throw new OrderException(e.getMessage());
                    }
                },
                lines3 -> {
                    try {
                        return getSize(lines3);
                    } catch (IOException e) {
                        throw new OrderException(e.getMessage());
                    }
                },
                this::removeAsk,
                this::removeBid
        );

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_INPUT_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                Data d = convertIntoMap(line);
                for (Predicate<Data> predicate : predicates) {
                    if (predicate.test(d)) {
                        break;
                    }
                }
            }
        }

        writer.append(output);
        writer.close();
    }

    private boolean updateBid(Data lines) {
        return updateOffers(lines, BID, askMap, bidMap);
    }

    private boolean updateAsk(Data lines) {
        return updateOffers(lines, ASK, bidMap, askMap);
    }

    private boolean updateOffers(Data lines, String operation, Map<Integer, Integer> firstMap, Map<Integer, Integer> secondMap) {
        int price = lines.getPrice();
        if (lines.getType().equals(operation) && lines.getOperation().equals(U.name().toLowerCase()) && firstMap.get(price) == null) {
            secondMap.put(price, lines.getSize());
            return true;
        }
        return false;
    }

    private boolean bestBid(Data lines) throws IOException {
        if (lines.getType().equals(BEST_BID) && lines.getOperation().equals(Q.name().toLowerCase())) {
            writeToFileBestAskBid(getBestBid(), bidMap);
            return true;
        }
        return false;
    }

    private boolean bestAsk(Data lines) throws IOException {
        if (lines.getType().equals(BEST_ASK) && lines.getOperation().equals(Q.name().toLowerCase())) {
            writeToFileBestAskBid(getBestAsk(), askMap);
            return true;
        }
        return false;
    }

    private void writeToFileBestAskBid(Integer value, Map<Integer, Integer> map) throws IOException {
        output.append(value);
        output.append(COMMA);
        output.append(map.get(value));
        output.append(System.lineSeparator());
        flushStringBuilder();
    }

    private boolean getSize(Data lines) throws IOException {
        if (lines.getType().equals(SIZE) && lines.getOperation().equals(Q.name().toLowerCase())) {
            Integer askValue = askMap.get(lines.getPrice());
            if (askValue != null) {
                writeSize(askValue);
            }
            Integer bidValue = bidMap.get(lines.getPrice());
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

    private void writeSize(Integer value) throws IOException {
        output.append(value);
        output.append(System.lineSeparator());
        flushStringBuilder();
    }

    private boolean removeAsk(Data lines) {
        if (lines.getType().equals(BUY) && lines.getOperation().equals(O.name().toLowerCase())) {
            int result = lines.getSize();
            do {
                result = getOrders(Math.abs(result), getBestAsk(), askMap);
            } while (result < 0);
            return true;
        }
        return false;
    }

    private boolean removeBid(Data lines) {
        if (lines.getType().equals(SELL) && lines.getOperation().equals(O.name().toLowerCase())) {
            int result = lines.getSize();
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

    private Data convertIntoMap(String inputData) {
        return setData(inputData.split(COMMA));
    }

    private Data setData(String[] items) {
        switch (items.length) {
            case 2 -> {
                return new Data(items[0], items[1]);
            }
            case 3 -> {
                return new Data(items[0], Integer.parseInt(items[2]), items[1]);
            }
            case 4 -> {
                return new Data(items[0], Integer.parseInt(items[1]), Integer.parseInt(items[2]), items[3]);
            }
            default -> {
                return new Data();
            }
        }
    }

    private Integer getBestAsk() {
        Integer value = askMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() != 0)
                .min(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .orElse(-1);
        checkValue(value);
        return value;
    }

    private Integer getBestBid() {
        Integer value = bidMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() != 0)
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .orElse(-1);
        checkValue(value);
        return value;
    }

    private void checkValue(Integer value) {
        if (value == -1) {
            throw new OrderException(ERROR_MESSAGE);
        }
    }

    private void flushStringBuilder() throws IOException {
        if (!output.isEmpty() && output.length() >= MAX_STRING_BUILDER_SIZE) {
            writer.append(output);
            output.setLength(0);
        }
    }
}
