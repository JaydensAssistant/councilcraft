package com.github.quiltservertools.ledger.registry;

import com.github.quiltservertools.ledger.actions.ActionType;
import java.util.function.Supplier;

/** Compile-only stub; INSTANCE matches Kotlin object. */
public final class ActionRegistry {
    public static final ActionRegistry INSTANCE = new ActionRegistry();

    private ActionRegistry() {}

    public void registerActionType(Supplier<ActionType> supplier) {}
}
