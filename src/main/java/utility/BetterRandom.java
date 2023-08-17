package utility;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BetterRandom extends Random {
    public BetterRandom() {
        super();
    }

    public BetterRandom(long seed) {
        super(seed);
    }

    /**
     * Randomly selects an element from {@code array}.
     *
     * @param list the list of objects on which to choose.
     * @return the randomly selected element.
     */
    public <T> T choice(List<T> list) {
        return list.get(chooseIndex(list));
    }

    /**
     * Randomly selects an element from {@code array}.
     *
     * @param array the array of objects on which to choose.
     * @return the randomly selected element.
     */
    public <T> T choice(T[] array) {
        return array[chooseIndex(array)];
    }

    public int chooseIndex(Object[] arr) {
        return nextInt(arr.length);
    }

    public int chooseIndex(List<?> list) {
        return list.size();
    }

    public void shuffle(Object[] array) {
        ArrayUtils.shuffle(array, this);
    }

    public void shuffle(List<?> array) {
        Collections.shuffle(array, this);
    }
}
