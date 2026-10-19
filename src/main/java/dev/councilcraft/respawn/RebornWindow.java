package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;

/**
 * One five-minute activity window for one Reborn player.
 * The server records camera samples and actions as they happen, then asks
 * {@link #counts()} once, at the end. The window is not saved: logout drops it.
 * Movement is not judged here. {@link RebornMotion} does that.
 *
 * <p>A window's activity passes when the camera moved at least
 * {@link CouncilCraftRules#REBORN_CAMERA_DEGREES} and each of yaw and pitch
 * moved at least {@link CouncilCraftRules#REBORN_CAMERA_DIRECTION_DEGREES},
 * and the player also did a qualifying action. Chat, eating and drinking never
 * qualify. The same action on one target is one held stream when consecutive
 * gaps stay within {@link CouncilCraftRules#REBORN_HELD_JITTER_TICKS} of each
 * other, and that stream does not qualify. Twelve or more actions are a macro
 * when their gap spread is within {@link CouncilCraftRules#REBORN_MACRO_TOLERANCE_TICKS}
 * or their standard deviation is within {@link CouncilCraftRules#REBORN_MACRO_STDEV_TICKS}.
 * Another action outside that run still can. The current run's ticks sit in a ring.
 */
public final class RebornWindow {
    public enum Action {
        BREAK,
        PLACE,
        USE,
        ATTACK,
        CRAFT,
        TRADE,
        MENU,
        EAT,
        CHAT
    }

    private float yawAbs;
    private float pitchAbs;
    private boolean hasLook;
    private float lastYaw;
    private float lastPitch;

    private boolean holding;
    private Action holdAction = Action.USE;
    private int holdTarget;
    private int holdCount;
    private int holdTick;
    private int holdPrevGap;

    /** Ticks in the current rhythmic run. Oldest overwritten once the ring is full. */
    private final int[] ring = new int[CouncilCraftRules.REBORN_ACTION_RING];
    private int ringCount;
    private int ringNext;
    private int rhythm;
    private int rhythmTick;
    private int gaps;
    private long sumGap;
    private long sumSq;
    private int minGap = Integer.MAX_VALUE;
    private int maxGap = Integer.MIN_VALUE;
    private boolean hasQualifying;
    private long age;

    /** Yaw and pitch after a look packet. The first sample only sets the anchor. */
    public void look(float yaw, float pitch) {
        if (!this.hasLook) {
            this.lastYaw = yaw;
            this.lastPitch = pitch;
            this.hasLook = true;
            return;
        }
        this.yawAbs += Math.abs(wrap(yaw - this.lastYaw));
        this.pitchAbs += Math.abs(pitch - this.lastPitch);
        this.lastYaw = yaw;
        this.lastPitch = pitch;
    }

    public void act(Action action, int target, int tick) {
        if (action == Action.EAT || action == Action.CHAT) {
            return;
        }
        if (this.holding && action == this.holdAction && target == this.holdTarget) {
            int gap = tick - this.holdTick;
            if (gap == 0) {
                return;
            }
            if (gap > 0 && this.holdCount == 1) {
                this.holdPrevGap = gap;
                this.holdCount = 2;
                this.holdTick = tick;
                return;
            }
            if (gap > 0 && Math.abs(gap - this.holdPrevGap) <= CouncilCraftRules.REBORN_HELD_JITTER_TICKS) {
                this.holdPrevGap = gap;
                this.holdCount++;
                this.holdTick = tick;
                return;
            }
        }
        closeHold();
        this.holding = true;
        this.holdAction = action;
        this.holdTarget = target;
        this.holdCount = 1;
        this.holdTick = tick;
    }

    /** One server tick of online time. True when this window has reached its end. */
    public boolean advance() {
        this.age++;
        return this.age >= CouncilCraftRules.REBORN_WINDOW_TICKS;
    }

    /**
     * True when the camera and the actions would credit five minutes.
     * The caller also requires a recent self-driven move.
     */
    public boolean counts() {
        closeHold();
        closeRhythm();
        return cameraOk() && this.hasQualifying;
    }

    /** Three or more clicks on one cadence are a held button and are dropped. */
    private void closeHold() {
        if (!this.holding) {
            return;
        }
        if (this.holdCount >= 3) {
            // Steady by construction: a gap that jumped by more than the jitter ended the run.
        } else if (this.holdCount == 2) {
            noteRhythm(this.holdTick - this.holdPrevGap);
            noteRhythm(this.holdTick);
        } else if (this.holdCount == 1) {
            noteRhythm(this.holdTick);
        }
        this.holding = false;
        this.holdCount = 0;
    }

    private void noteRhythm(int tick) {
        if (this.rhythm > 0 && tick == this.rhythmTick) {
            return;
        }
        if (this.rhythm >= 2) {
            int gap = tick - this.rhythmTick;
            int newMin = Math.min(this.minGap, gap);
            int newMax = Math.max(this.maxGap, gap);
            double dev = deviation(this.gaps + 1, this.sumGap + gap, this.sumSq + (long) gap * gap);
            if (this.gaps >= 1
                && newMax - newMin > CouncilCraftRules.REBORN_MACRO_TOLERANCE_TICKS
                && dev > CouncilCraftRules.REBORN_MACRO_STDEV_TICKS) {
                closeRhythm();
            }
        }
        if (this.rhythm == 0) {
            this.rhythm = 1;
            this.rhythmTick = tick;
            pushRing(tick);
            return;
        }
        int gap = tick - this.rhythmTick;
        if (gap < 0) {
            closeRhythm();
            this.rhythm = 1;
            this.rhythmTick = tick;
            pushRing(tick);
            return;
        }
        if (this.rhythm == 1) {
            this.minGap = gap;
            this.maxGap = gap;
        } else {
            this.minGap = Math.min(this.minGap, gap);
            this.maxGap = Math.max(this.maxGap, gap);
        }
        this.gaps++;
        this.sumGap += gap;
        this.sumSq += (long) gap * gap;
        this.rhythm++;
        this.rhythmTick = tick;
        pushRing(tick);
    }

    /** A short or uneven run counts. A long steady run does not. */
    private void closeRhythm() {
        if (this.rhythm <= 0) {
            return;
        }
        boolean macro = this.rhythm >= CouncilCraftRules.REBORN_MACRO_ACTIONS && macroSpread();
        if (!macro) {
            this.hasQualifying = true;
        }
        this.rhythm = 0;
        this.gaps = 0;
        this.sumGap = 0L;
        this.sumSq = 0L;
        this.ringCount = 0;
        this.ringNext = 0;
        this.minGap = Integer.MAX_VALUE;
        this.maxGap = Integer.MIN_VALUE;
    }

    private boolean macroSpread() {
        int spread;
        double dev;
        if (this.rhythm <= this.ringCount) {
            spread = ringSpread();
            dev = ringDeviation();
        } else {
            spread = this.maxGap - this.minGap;
            dev = deviation(this.gaps, this.sumGap, this.sumSq);
        }
        return spread <= CouncilCraftRules.REBORN_MACRO_TOLERANCE_TICKS
            || dev <= CouncilCraftRules.REBORN_MACRO_STDEV_TICKS;
    }

    private void pushRing(int tick) {
        this.ring[this.ringNext] = tick;
        this.ringNext = (this.ringNext + 1) % this.ring.length;
        if (this.ringCount < this.ring.length) {
            this.ringCount++;
        }
    }

    private boolean cameraOk() {
        if (this.yawAbs + this.pitchAbs < CouncilCraftRules.REBORN_CAMERA_DEGREES) {
            return false;
        }
        float floor = CouncilCraftRules.REBORN_CAMERA_DIRECTION_DEGREES;
        return this.yawAbs >= floor && this.pitchAbs >= floor;
    }

    private int ringSpread() {
        if (this.ringCount < 2) {
            return Integer.MAX_VALUE;
        }
        int start = this.ringCount == this.ring.length ? this.ringNext : 0;
        int previous = this.ring[start];
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 1; i < this.ringCount; i++) {
            int tick = this.ring[(start + i) % this.ring.length];
            int gap = tick - previous;
            if (gap < min) {
                min = gap;
            }
            if (gap > max) {
                max = gap;
            }
            previous = tick;
        }
        return max - min;
    }

    private double ringDeviation() {
        if (this.ringCount < 3) {
            return Double.MAX_VALUE;
        }
        int start = this.ringCount == this.ring.length ? this.ringNext : 0;
        int previous = this.ring[start];
        int count = 0;
        long sum = 0L;
        long sumSq = 0L;
        for (int i = 1; i < this.ringCount; i++) {
            int tick = this.ring[(start + i) % this.ring.length];
            int gap = tick - previous;
            count++;
            sum += gap;
            sumSq += (long) gap * gap;
            previous = tick;
        }
        return deviation(count, sum, sumSq);
    }

    private static double deviation(int count, long sum, long sumSq) {
        if (count <= 0) {
            return Double.MAX_VALUE;
        }
        double mean = sum / (double) count;
        double variance = sumSq / (double) count - mean * mean;
        if (variance < 0.0) {
            variance = 0.0;
        }
        return Math.sqrt(variance);
    }

    private static float wrap(float degrees) {
        float wrapped = degrees % 360f;
        if (wrapped >= 180f) {
            wrapped -= 360f;
        }
        if (wrapped < -180f) {
            wrapped += 360f;
        }
        return wrapped;
    }
}
