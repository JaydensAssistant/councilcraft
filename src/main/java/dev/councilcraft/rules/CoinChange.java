package dev.councilcraft.rules;

/**
 * PM34. Paying a Numismatics price with whatever coins the player carries, by total value. Pure maths so
 * it can be tested without a world; values are listed smallest coin first.
 */
public final class CoinChange {
    private CoinChange() {
    }

    /**
     * Coins to take, per coin type, to cover {@code price}; null when the coins carried are worth less.
     * Largest coins first without going over the price, then the smallest coin that covers what is left
     * (or, if no single coin covers it, the smallest coins one at a time). Any overpayment comes back as
     * change, so the player never loses value.
     */
    public static int[] take(int[] values, int[] available, int price) {
        long total = 0;
        for (int i = 0; i < values.length; i++) {
            total += (long) values[i] * Math.max(0, available[i]);
        }
        if (price <= 0 || total < price) {
            return price <= 0 ? new int[values.length] : null;
        }
        int[] taken = new int[values.length];
        int remaining = price;
        for (int i = values.length - 1; i >= 0 && remaining > 0; i--) {
            int count = Math.min(Math.max(0, available[i]), remaining / values[i]);
            taken[i] = count;
            remaining -= count * values[i];
        }
        if (remaining > 0) {
            for (int i = 0; i < values.length; i++) {
                if (available[i] - taken[i] > 0 && values[i] >= remaining) {
                    taken[i]++;
                    return taken;
                }
            }
            for (int i = 0; i < values.length && remaining > 0; i++) {
                while (available[i] - taken[i] > 0 && remaining > 0) {
                    taken[i]++;
                    remaining -= values[i];
                }
            }
        }
        return taken;
    }

    /** Value of a set of coins. */
    public static int value(int[] values, int[] counts) {
        int sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i] * counts[i];
        }
        return sum;
    }

    /** {@code amount} as the largest coins possible (greedy; exact for 1/5/10/25/50/100). */
    public static int[] change(int[] values, int amount) {
        int[] counts = new int[values.length];
        int remaining = Math.max(0, amount);
        for (int i = values.length - 1; i >= 0; i--) {
            counts[i] = remaining / values[i];
            remaining -= counts[i] * values[i];
        }
        return counts;
    }
}
