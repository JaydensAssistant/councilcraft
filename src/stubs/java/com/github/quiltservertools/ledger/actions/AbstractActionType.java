package com.github.quiltservertools.ledger.actions;

import com.mojang.authlib.GameProfile;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Compile-only stub; runtime uses Ledger's AbstractActionType. */
public abstract class AbstractActionType implements ActionType {
    public void setPos(BlockPos pos) {}
    public void setWorld(Identifier world) {}
    public void setSourceProfile(GameProfile profile) {}
    public void setSourceName(String name) {}
    public void setExtraData(String data) {}
    public void setObjectResourceLocation(Identifier id) {}
    public void setObjectState(String state) {}
}
