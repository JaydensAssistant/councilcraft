package dev.councilcraft.supporter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.PersistentState;

/**
 * PPM102. Donors, roles and role assignments, saved with the world (data/councilcraft_supporters.dat in the
 * overworld). Maps are concurrent because display names can be read off the server thread (chat relays).
 */
public final class SupporterState extends PersistentState {
    public static final String ID = "councilcraft_supporters";

    /** A donor's tier in dollars and last known name (so offline donors still show in the footer). */
    public record Donor(String name, int tier) {
    }

    public record Role(int color, String prefix) {
    }

    /** A player's role id and last known name. */
    public record Assignment(String name, String roleId) {
    }

    final Map<UUID, Donor> donors = new ConcurrentHashMap<>();
    final Map<String, Role> roles = new ConcurrentHashMap<>();
    final Map<UUID, Assignment> assignments = new ConcurrentHashMap<>();

    public static SupporterState fromNbt(NbtCompound nbt) {
        SupporterState state = new SupporterState();
        for (NbtElement e : nbt.getList("donors", NbtElement.COMPOUND_TYPE)) {
            NbtCompound c = (NbtCompound) e;
            state.donors.put(c.getUuid("uuid"), new Donor(c.getString("name"), c.getInt("tier")));
        }
        for (NbtElement e : nbt.getList("roles", NbtElement.COMPOUND_TYPE)) {
            NbtCompound c = (NbtCompound) e;
            state.roles.put(c.getString("id"), new Role(c.getInt("color"), c.getString("prefix")));
        }
        for (NbtElement e : nbt.getList("assignments", NbtElement.COMPOUND_TYPE)) {
            NbtCompound c = (NbtCompound) e;
            state.assignments.put(c.getUuid("uuid"), new Assignment(c.getString("name"), c.getString("role")));
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList donorList = new NbtList();
        this.donors.forEach((uuid, d) -> {
            NbtCompound c = new NbtCompound();
            c.putUuid("uuid", uuid);
            c.putString("name", d.name());
            c.putInt("tier", d.tier());
            donorList.add(c);
        });
        NbtList roleList = new NbtList();
        this.roles.forEach((id, r) -> {
            NbtCompound c = new NbtCompound();
            c.putString("id", id);
            c.putInt("color", r.color());
            c.putString("prefix", r.prefix());
            roleList.add(c);
        });
        NbtList assignmentList = new NbtList();
        this.assignments.forEach((uuid, a) -> {
            NbtCompound c = new NbtCompound();
            c.putUuid("uuid", uuid);
            c.putString("name", a.name());
            c.putString("role", a.roleId());
            assignmentList.add(c);
        });
        nbt.put("donors", donorList);
        nbt.put("roles", roleList);
        nbt.put("assignments", assignmentList);
        return nbt;
    }

    /** The player's role, or null (also null if the role was deleted). */
    public Role roleOf(UUID uuid) {
        Assignment a = this.assignments.get(uuid);
        return a == null ? null : this.roles.get(a.roleId());
    }

    /** Keeps stored names current when a donor or role holder changes their name. */
    void updateName(UUID uuid, String name) {
        Donor d = this.donors.get(uuid);
        if (d != null && !d.name().equals(name)) {
            this.donors.put(uuid, new Donor(name, d.tier()));
            this.markDirty();
        }
        Assignment a = this.assignments.get(uuid);
        if (a != null && !a.name().equals(name)) {
            this.assignments.put(uuid, new Assignment(name, a.roleId()));
            this.markDirty();
        }
    }
}
