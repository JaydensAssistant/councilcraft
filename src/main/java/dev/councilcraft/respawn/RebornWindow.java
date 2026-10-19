package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;

/**
 * One five-minute activity window for one Reborn player.
 * The server records camera samples and actions as they happen, then asks
 * {@link #counts()} once, at the end. There is no distance check. The window
 * is not saved: logout drops it.
 *
 * <p>A window counts only when the camera moved at least
 * {@link CouncilCraftRules#REBORN_CAMERA_DEGREES} and both yaw and pitch moved,
 * and the player also did a qualifying action. Chat, eating and drinking never
 * qualify. A held use or attack on one target does not qualify. Twelve or more
 * actions whose gaps all sit within {@link CouncilCraftRules#REBORN_MACRO_TOLERANCE_TICKS}
 * of each other are a macro and do not qualify. Another action outside that run
 * still can. The current run's ticks sit in a ring.
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

    private boolean streaming;
    private Action streamAction = Action.USE;
    private int streamTarget;
    private int streamCount;
    private int streamTick;

    /** Ticks in the current rhythmic run. Oldest overwritten once the ring is full. */
    private final int[] ring = new int[CouncilCraftRules.REBORN_ACTION_RING];
    private int ringCount;
    private int ringNext;
    private int streak;
    private int previousTick;
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
        if (action == Action.USE || action == Action.ATTACK) {
            noteStream(action, target, tick);
            return;
        }
        finishStream();
        noteStreak(tick);
    }

    /** One server tick of online time. True when this window has reached its end. */
    public boolean advance() {
        this.age++;
        return this.age >= CouncilCraftRules.REBORN_WINDOW_TICKS;
    }

    /** True when this window should credit five active minutes. Call once, at the end. */
    public boolean counts() {
        finishStream();
        closeStreak();
        return cameraOk() && this.hasQualifying;
    }

    private void noteStream(Action action, int target, int tick) {
        if (this.streaming && action == this.streamAction && target == this.streamTarget) {
            int gap = tick - this.streamTick;
            if (gap == 0) {
                return;
            }
            if (gap > 0 && gap <= CouncilCraftRules.REBORN_HELD_GAP_TICKS) {
                this.streamCount++;
                this.streamTick = tick;
                return;
            }
        }
        finishStream();
        this.streaming = true;
        this.streamAction = action;
        this.streamTarget = target;
        this.streamCount = 1;
        this.streamTick = tick;
    }

    /** One click is a real gesture. A longer continuous run on one target is a held button. */
    private void finishStream() {
        if (!this.streaming) {
            return;
        }
        if (this.streamCount < 2) {
            noteStreak(this.streamTick);
        }
        this.streaming = false;
        this.streamCount = 0;
    }

    private void noteStreak(int tick) {
        if (this.streak > 0 && tick == this.previousTick) {
            return;
        }
        if (this.streak == 0) {
            this.streak = 1;
            this.previousTick = tick;
            pushRing(tick);
            return;
        }
        int gap = tick - this.previousTick;
        if (this.streak == 1) {
            this.minGap = gap;
            this.maxGap = gap;
            this.streak = 2;
            this.previousTick = tick;
            pushRing(tick);
            return;
        }
        int newMin = Math.min(this.minGap, gap);
        int newMax = Math.max(this.maxGap, gap);
        if (gap < 0 || newMax - newMin > CouncilCraftRules.REBORN_MACRO_TOLERANCE_TICKS) {
            closeStreak();
            this.streak = 1;
            this.previousTick = tick;
            pushRing(tick);
            return;
        }
        this.minGap = newMin;
        this.maxGap = newMax;
        this.streak++;
        this.previousTick = tick;
        pushRing(tick);
    }

    /** A short run counts. A run of 12 or more at one cadence does not. */
    private void closeStreak() {
        if (this.streak <= 0) {
            return;
        }
        if (this.streak < CouncilCraftRules.REBORN_MACRO_ACTIONS) {
            this.hasQualifying = true;
        } else {
            int spread = this.streak <= this.ringCount ? ringSpread() : this.maxGap - this.minGap;
            if (spread > CouncilCraftRules.REBORN_MACRO_TOLERANCE_TICKS) {
                this.hasQualifying = true;
            }
        }
        this.streak = 0;
        this.ringCount = 0;
        this.ringNext = 0;
        this.minGap = Integer.MAX_VALUE;
        this.maxGap = Integer.MIN_VALUE;
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

    /** Widest minus narrowest gap in the ring. One gesture is not a macro. */
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
