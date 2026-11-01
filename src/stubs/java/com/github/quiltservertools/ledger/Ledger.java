package com.github.quiltservertools.ledger;

import com.github.quiltservertools.ledger.api.LedgerApi;

/** Compile-only stub; INSTANCE/getApi match Kotlin object binary names. */
public final class Ledger {
    public static final Ledger INSTANCE = new Ledger();
    private static LedgerApi api;

    private Ledger() {}

    public static LedgerApi getApi() {
        return api;
    }
}
