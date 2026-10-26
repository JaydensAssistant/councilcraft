package dev.councilcraft.carry;

import dev.councilcraft.rules.CarryEscapeRules;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.PersistentState;

/**
 * PT22 amendment. Escape level and last escape time (overworld game time) per baby, saved with the world
 * (data/councilcraft_carry_escapes.dat in the overworld), so relogs and restarts keep the cooldown.
 */
public final class CarryEscapeState extends PersistentState {
    public static final String ID = "councilcraft_carry_escapes";

    record Escape(int level, long last) {
    }

    final Map<UUID, Escape> escapes = new HashMap<>();

    public static CarryEscapeState fromNbt(NbtCompound nbt) {
        CarryEscapeState state = new CarryEscapeState();
        for (NbtElement e : nbt.getList("escapes", NbtElement.COMPOUND_TYPE)) {
            NbtCompound c = (NbtCompound) e;
            state.escapes.put(c.getUuid("uuid"), new Escape(c.getInt("level"), c.getLong("last")));
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        this.escapes.forEach((uuid, e) -> {
            NbtCompound c = new NbtCompound();
            c.putUuid("uuid", uuid);
            c.putInt("level", e.level());
            c.putLong("last", e.last());
            list.add(c);
        });
        nbt.put("escapes", list);
        return nbt;
    }

    /** Records an escape and returns the new cooldown in ticks. */
    long escape(UUID baby, long now) {
        Escape old = this.escapes.get(baby);
        int level = old == null ? CarryEscapeRules.levelAfterEscape(0, 0, now)
            : CarryEscapeRules.levelAfterEscape(old.level(), old.last(), now);
        this.escapes.put(baby, new Escape(level, now));
        this.markDirty();
        return CarryEscapeRules.cooldownTicks(level);
    }

    long ticksLeft(UUID baby, long now) {
        Escape e = this.escapes.get(baby);
        return e == null ? 0 : CarryEscapeRules.ticksLeft(e.level(), e.last(), now);
    }

    /** Drops records whose quiet window has passed (they would restart at 5 s anyway). */
    void prune(long now) {
        if (this.escapes.values().removeIf(e -> CarryEscapeRules.expired(e.level(), e.last(), now))) {
            this.markDirty();
        }
    }
}
