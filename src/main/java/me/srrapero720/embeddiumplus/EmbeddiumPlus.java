package me.srrapero720.embeddiumplus;

import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.slf4j.LoggerFactory;

@Mod(EmbeddiumPlus.ID)
public class EmbeddiumPlus {
    public static final String ID = "embeddiumplus";
    public static final Logger LOGGER = LogManager.getLogger("Embeddium++");

    public EmbeddiumPlus() {
        ArtifactVersion version = getForgeVersion();
        if (version == null) {
            return;
        }
        if (version.compareTo(new DefaultArtifactVersion("41.1.0")) < 0) {
            // On later versions this is done via the mods.toml entry
            markAsOneSided();
        }}


    private static final org.slf4j.Logger LOGGER2 = LoggerFactory.getLogger("MixinExtras|Forge");
    private static final String IGNORESERVERONLY = "OHNOES\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31";


    private ArtifactVersion getForgeVersion() {
        return ModList.get().getModContainerById("forge").map(it -> it.getModInfo().getVersion()).orElse(null);
    }

    private void markAsOneSided() {
        try {
            ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> IGNORESERVERONLY, (a, b) -> true));
        } catch (Exception e) {
            // Not hugely important
            LOGGER2.warn("Failed to mark MixinExtras as a one-sided mod", e);
        }
    }
}