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
 * target does not qualify. Twelve or more separate actions whose gaps all
 * sit within {@link CouncilCraftRules#REBORN_MACRO_TOLERANCE_TICKS} of each
 * other are a macro and do not qualify. Several actions on the same tick are
 * one gesture, so filling a crafting grid is not a macro.
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

    private int qualifying;
    private int previousTick;
    private int minGap = Integer.MAX_VALUE;
    private int maxGap = Integer.MIN_VALUE;
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

    public void act(Action action, int target, int tick) {
        if (action == Action.EAT || action == Action.CHAT) {
            return;
        }
        if (action == Action.USE || action == Action.ATTACK) {
            noteStream(action, target, tick);
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

    private void noteStream(Action action, int target, int tick) {
        if (this.streaming
            && action == this.streamAction
            && target == this.streamTarget
            && tick - this.streamTick <= CouncilCraftRules.REBORN_HELD_GAP_TICKS) {
            this.streamCount++;
            this.streamTick = tick;
            return;
        }
        finishStream();
        this.streaming = true;
        this.streamAction = action;
        this.streamTarget = target;
        this.streamCount = 1;
        this.streamTick = tick;
    }

    /** A run of one click is a real action. A longer run on one target is a held button. */
    private void finishStream() {
        if (!this.streaming) {
            return;
        }
        if (this.streamCount == 1) {
            addQualifying(this.streamTick);
        }
        this.streaming = false;
        this.streamCount = 0;
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
        return this.maxGap - this.minGap > CouncilCraftRules.REBORN_MACRO_TOLERANCE_TICKS;
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
