package dev.councilcraft.mixin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CC-142A. Redirect {@code @At} targets that mention {@code Entity} must use
 * {@code remap = true} so intermediary {@code class_1297} matches at runtime.
 * Method-level {@code remap = false} (NiftyCarts member names) is fine.
 */
class AbstractDrawnMixinRemapTest {
    /** Match {@code at = @At( ... )} with nested parens in the target descriptor. */
    private static final Pattern AT_ASSIGN = Pattern.compile(
        "at\\s*=\\s*@At\\((?:[^()]|\\([^()]*\\))*\\)",
        Pattern.MULTILINE
    );

    @Test
    void setPullingRedirectAtsUseRemapTrue() throws Exception {
        Path src = Path.of("src/main/java/dev/councilcraft/mixin/nifty/AbstractDrawnNoAutoUnhitchMixin.java");
        assertTrue(Files.isRegularFile(src), "missing " + src.toAbsolutePath());
        String text = Files.readString(src);

        int first = text.indexOf("councilcraft$noDistanceDetach");
        int second = text.indexOf("councilcraft$noStopDetach");
        assertTrue(first > 0 && second > first);

        String distanceBlock = text.substring(text.lastIndexOf("@Redirect", first), first);
        String stopBlock = text.substring(text.lastIndexOf("@Redirect", second), second);

        assertAtRemapsEntity(distanceBlock, "noDistanceDetach");
        assertAtRemapsEntity(stopBlock, "noStopDetach");
        assertTrue(stopBlock.contains("expect = 2"),
            "shouldStopPulledTick has two setPulling sites; expect = 2 required");
    }

    private static void assertAtRemapsEntity(String redirectBlock, String name) {
        Matcher m = AT_ASSIGN.matcher(redirectBlock);
        assertTrue(m.find(), name + " missing at = @At(...) assignment:\n" + redirectBlock);
        String at = m.group();
        assertTrue(at.contains("remap = true"),
            name + " @At must use remap = true; was:\n" + at);
        assertFalse(at.contains("remap = false"),
            name + " @At must not set remap = false; was:\n" + at);
        assertTrue(at.contains("setPulling(Lnet/minecraft/entity/Entity;)V"),
            name + " @At target must name Entity for remapping; was:\n" + at);
    }
}
