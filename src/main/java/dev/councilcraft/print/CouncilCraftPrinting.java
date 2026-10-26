package dev.councilcraft.print;

import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import plus.dragons.createenchantmentindustry.common.fluids.printer.behaviour.PrintingBehaviour;
import plus.dragons.createenchantmentindustry.common.fluids.printer.behaviour.PrintingBehaviourProvider;

/** PT14. Registers the photo and signed-album printing through CEI's public printing-behaviour registry. */
public final class CouncilCraftPrinting {
    private CouncilCraftPrinting() {
    }

    public static void register() {
        ProcessInk.register();
        Registry.register(PrintingBehaviour.REGISTRY, new Identifier("councilcraft", "photograph"),
            new PrintingBehaviourProvider(PrintingBehaviourProvider.DEFAULT_PRIORITY, PhotoPrintingBehaviour::create));
        Registry.register(PrintingBehaviour.REGISTRY, new Identifier("councilcraft", "signed_album"),
            new PrintingBehaviourProvider(PrintingBehaviourProvider.DEFAULT_PRIORITY, SignedAlbumPrintingBehaviour::create));
    }
}
