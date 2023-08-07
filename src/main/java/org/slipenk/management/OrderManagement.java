package org.slipenk.management;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

    private final TreeMap<Integer, Integer> askMap = new TreeMap<>();
    private final TreeMap<Integer, Integer> bidMap = new TreeMap<>();
    private final StringBuilder outputBuilder = new StringBuilder();

    public void getJob() throws IOException {

        List<Predicate<String>> predicates = Arrays.asList(
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
                for (Predicate<String> predicate : predicates) {
                    if (predicate.test(line)) {
                        break;
                    }
                }
            }
        }

        Files.write(Path.of(FILE_OUTPUT_NAME), outputBuilder.toString().getBytes());
    }

    private boolean updateBid(String line) {
        return updateOffers(line, BID, bidMap);
    }

    private boolean updateAsk(String line) {
        return updateOffers(line, ASK, askMap);
    }

    private boolean updateOffers(String line, String operation, Map<Integer, Integer> map) {
        if (line.charAt(0) == U.name().toLowerCase().charAt(0)) {
            int[] priceAndSize = getPriceAndSize(line);
            if (line.contains(operation)) {
                if (priceAndSize[1] == 0) {
                    map.remove(priceAndSize[0]);
                } else {
                    map.put(priceAndSize[0], priceAndSize[1]);
                }
                return true;
            }
        }
        return false;
    }

    private int[] getPriceAndSize(String line) {
        int firstCommaIndex = line.indexOf(COMMA);
        int secondCommaIndex = line.indexOf(COMMA, firstCommaIndex + 1);
        int thirdCommaIndex = line.indexOf(COMMA, secondCommaIndex + 1);

        String priceStr = line.substring(firstCommaIndex + 1, secondCommaIndex);
        String sizeStr = line.substring(secondCommaIndex + 1, thirdCommaIndex);

        int price = Integer.parseInt(priceStr);
        int size = Integer.parseInt(sizeStr);

        return new int[]{price, size};
    }

    private boolean bestBid(String line) {
        if (line.contains(BEST_BID) && line.charAt(0) == Q.name().toLowerCase().charAt(0)) {
            writeToFileBestAskBid(bidMap.lastKey(), bidMap.lastEntry().getValue());
            return true;
        }
        return false;
    }

    private boolean bestAsk(String line) {
        if (line.contains(BEST_ASK) && line.charAt(0) == Q.name().toLowerCase().charAt(0)) {
            writeToFileBestAskBid(askMap.firstKey(), askMap.firstEntry().getValue());
            return true;
        }
        return false;
    }

    private void writeToFileBestAskBid(Integer key, Integer value) {
        outputBuilder.append(key);
        outputBuilder.append(COMMA);
        outputBuilder.append(value);
        outputBuilder.append(System.lineSeparator());
    }

    private boolean getSize(String line) {
        if (line.contains(SIZE) && line.charAt(0) == Q.name().toLowerCase().charAt(0)) {
            int price = Integer.parseInt(line.substring(7));
            Integer askValue = askMap.get(price);
            if (askValue != null) {
                writeSize(askValue);
            }
            Integer bidValue = bidMap.get(price);
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

    private boolean removeAsk(String line) {
        if (line.contains(BUY) && line.charAt(0) == O.name().toLowerCase().charAt(0)) {
            int result = Integer.parseInt(line.substring(6));
            do {
                result = getOrdersAsk(Math.abs(result));
            } while (result < 0);
            return true;
        }
        return false;
    }

    private boolean removeBid(String line) {
        if (line.contains(SELL) && line.charAt(0) == O.name().toLowerCase().charAt(0)) {
            int result = Integer.parseInt(line.substring(7));
            do {
                result = getOrdersBid(Math.abs(result));
            } while (result < 0);
            return true;
        }
        return false;
    }

    private int getOrdersAsk(int orderSize) {
        int result = askMap.firstEntry().getValue() - orderSize;
        if (result <= 0) {
            askMap.remove(askMap.firstKey());
        } else {
            askMap.replace(askMap.firstKey(), result);
        }
        return result;
    }

    private int getOrdersBid(int orderSize) {
        int result = bidMap.lastEntry().getValue() - orderSize;
        if (result <= 0) {
            bidMap.remove(bidMap.lastKey());
        } else {
            bidMap.replace(bidMap.lastKey(), result);
        }
        return result;
    }

}
