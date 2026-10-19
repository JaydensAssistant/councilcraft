package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;

/**
 * One five-minute activity window for one Reborn player.
 * The server records camera samples and actions as they happen, then asks
 * {@link #counts()} once, at the end. There is no distance check.
 *
 * <p>A window counts only when the camera moved at least
 * {@link CouncilCraftRules#REBORN_CAMERA_DEGREES} and that movement was in
 * more than one direction, and the player also did a qualifying action.
 * Chat, eating and drinking never qualify. A held use or attack on one
 * target does not qualify while the player is standing still. Twelve or more
 * gestures whose gaps all sit within {@link CouncilCraftRules#REBORN_MACRO_TOLERANCE_TICKS}
 * of each other are a macro and do not qualify. The last few gesture ticks sit
 * in a ring. Several actions on the same tick are one gesture.
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

    private float yawPositive;
    private float yawNegative;
    private float pitchPositive;
    private float pitchNegative;
    private boolean hasLook;
    private float lastYaw;
    private float lastPitch;

    private boolean streaming;
    private Action streamAction = Action.USE;
    private int streamTarget;
    private int streamCount;
    private int streamTick;
    private boolean streamStill = true;

    /** Last gesture ticks, oldest overwritten once the ring is full. */
    private final int[] ring = new int[CouncilCraftRules.REBORN_ACTION_RING];
    private int ringCount;
    private int ringNext;
    private int qualifying;
    private int previousTick;
    private int minGap = Integer.MAX_VALUE;
    private int maxGap = Integer.MIN_VALUE;
    private boolean hasPos;
    private double lastX;
    private double lastY;
    private double lastZ;
    private long age;

    /** Yaw and pitch after a look packet. The first sample only sets the anchor. */
    public void look(float yaw, float pitch) {
        if (!this.hasLook) {
            this.lastYaw = yaw;
            this.lastPitch = pitch;
            this.hasLook = true;
            return;
        }
        addAxis(wrap(yaw - this.lastYaw), true);
        addAxis(pitch - this.lastPitch, false);
        this.lastYaw = yaw;
        this.lastPitch = pitch;
    }

    /** Standing still. The first sample has nothing to compare, so it counts as still. */
    public boolean still(double x, double y, double z) {
        if (!this.hasPos) {
            this.hasPos = true;
            this.lastX = x;
            this.lastY = y;
            this.lastZ = z;
            return true;
        }
        double dx = x - this.lastX;
        double dy = y - this.lastY;
        double dz = z - this.lastZ;
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
        double limit = CouncilCraftRules.REBORN_STATIONARY_BLOCKS;
        return dx * dx + dy * dy + dz * dz <= limit * limit;
    }

    public void act(Action action, int target, int tick) {
        act(action, target, tick, true);
    }

    public void act(Action action, int target, int tick, boolean stationary) {
        if (action == Action.EAT || action == Action.CHAT) {
            return;
        }
        if (action == Action.USE || action == Action.ATTACK) {
            noteStream(action, target, tick, stationary);
            return;
        }
        finishStream();
        addQualifying(tick);
    }

    /** One server tick. True when this window has reached its end. */
    public boolean advance() {
        this.age++;
        return this.age >= CouncilCraftRules.REBORN_WINDOW_TICKS;
    }

    /** True when this window should credit five active minutes. Call once, at the end. */
    public boolean counts() {
        finishStream();
        return cameraOk() && actionsOk();
    }

    private void noteStream(Action action, int target, int tick, boolean stationary) {
        if (this.streaming
            && action == this.streamAction
            && target == this.streamTarget
            && tick - this.streamTick <= CouncilCraftRules.REBORN_HELD_GAP_TICKS) {
            this.streamCount++;
            this.streamTick = tick;
            if (!stationary) {
                this.streamStill = false;
            }
            return;
        }
        finishStream();
        this.streaming = true;
        this.streamAction = action;
        this.streamTarget = target;
        this.streamCount = 1;
        this.streamTick = tick;
        this.streamStill = stationary;
    }

    /**
     * One click is a real gesture. A longer run on one target is a held button
     * and does not count while the player stayed still. The same run while
     * moving counts, unless it is also a 12-long macro.
     */
    private void finishStream() {
        if (!this.streaming) {
            return;
        }
        if (this.streamCount == 1) {
            addQualifying(this.streamTick);
        } else if (!this.streamStill && this.streamCount < CouncilCraftRules.REBORN_MACRO_ACTIONS) {
            addQualifying(this.streamTick);
        }
        this.streaming = false;
        this.streamCount = 0;
        this.streamStill = true;
    }

    private void addQualifying(int tick) {
        if (this.qualifying > 0) {
            if (tick == this.previousTick) {
                return;
            }
            int gap = tick - this.previousTick;
            if (gap < this.minGap) {
                this.minGap = gap;
            }
            if (gap > this.maxGap) {
                this.maxGap = gap;
            }
        }
        this.previousTick = tick;
        this.qualifying++;
        this.ring[this.ringNext] = tick;
        this.ringNext = (this.ringNext + 1) % this.ring.length;
        if (this.ringCount < this.ring.length) {
            this.ringCount++;
        }
    }

    private boolean cameraOk() {
        float total = this.yawPositive + this.yawNegative + this.pitchPositive + this.pitchNegative;
        if (total < CouncilCraftRules.REBORN_CAMERA_DEGREES) {
            return false;
        }
        float floor = CouncilCraftRules.REBORN_CAMERA_DIRECTION_DEGREES;
        int directions = 0;
        if (this.yawPositive >= floor) {
            directions++;
        }
        if (this.yawNegative >= floor) {
            directions++;
        }
        if (this.pitchPositive >= floor) {
            directions++;
        }
        if (this.pitchNegative >= floor) {
            directions++;
        }
        return directions >= CouncilCraftRules.REBORN_CAMERA_DIRECTIONS;
    }

    private boolean actionsOk() {
        if (this.qualifying <= 0) {
            return false;
        }
        if (this.qualifying < CouncilCraftRules.REBORN_MACRO_ACTIONS) {
            return true;
        }
        int spread = this.qualifying <= this.ringCount ? this.ringSpread() : this.maxGap - this.minGap;
        return spread > CouncilCraftRules.REBORN_MACRO_TOLERANCE_TICKS;
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

    /** Small samples add up. A direction counts later, once its total clears the floor. */
    private void addAxis(float delta, boolean yaw) {
        if (delta > 0f) {
            if (yaw) {
                this.yawPositive += delta;
            } else {
                this.pitchPositive += delta;
            }
        } else if (delta < 0f) {
            if (yaw) {
                this.yawNegative -= delta;
            } else {
                this.pitchNegative -= delta;
            }
        }
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
