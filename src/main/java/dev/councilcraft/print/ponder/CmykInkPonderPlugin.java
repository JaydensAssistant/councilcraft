package dev.councilcraft.print.ponder;

import dev.councilcraft.print.ProcessInk;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.util.Identifier;

/**
 * PT36 (d). Ponder scene for CMYK Ink, shown on the bucket the way Create Dragons Plus shows its dye fluids
 * (CDPPonderPlugin). Registered from CouncilCraftClient only when Create (which carries Ponder) is loaded.
 */
public final class CmykInkPonderPlugin implements PonderPlugin {
    public static void register() {
        PonderIndex.addPlugin(new CmykInkPonderPlugin());
    }

    @Override
    public String getModId() {
        return "councilcraft";
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<Identifier> helper) {
        helper.forComponents(ProcessInk.BUCKET_ID).addStoryBoard(CmykInkScene.ID, CmykInkScene::scene);
    }
}
