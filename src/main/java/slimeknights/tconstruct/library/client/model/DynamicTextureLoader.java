package slimeknights.tconstruct.library.client.model;

import io.github.fabricators_of_create.porting_lib.event.client.TextureStitchCallback;
import lombok.extern.log4j.Log4j2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Logic to handle dynamic texture scans. Takes advantage of the fact that we reuse some tool textures to reduce resource lookups
 */
@Log4j2
public class DynamicTextureLoader {
  /** Map of discovered textures */
  private static final Map<ResourceLocation,Boolean> EXISTING_TEXTURES = new HashMap<>();
  /** Set of all textures that are missing from the resource pack, to avoid logging twice */
  private static final Set<ResourceLocation> SKIPPED_TEXTURES = new HashSet<>();

  /** Clears all cached texture names */
  public static void clearCache() {
    EXISTING_TEXTURES.clear();
    SKIPPED_TEXTURES.clear();
  }

  /** Registers this manager */
  public static void init() {
    // clear cache on texture stitch, no longer need it then as its too late to lookup textures
    TextureStitchCallback.POST.register(e -> clearCache());
  }

  /** Checks if a texture exists */
  public static boolean textureExists(ResourceManager manager, ResourceLocation location) {
    Boolean found = EXISTING_TEXTURES.get(location);
    if (found == null) {
      found = manager.getResource(new ResourceLocation(location.getNamespace(), "textures/" + location.getPath() + ".png")).isPresent();
      EXISTING_TEXTURES.put(location, found);
    }
    return found;
  }

  /** Logs that a dynamic texture is missing, config option to disable */
  public static void logMissingTexture(ResourceLocation location) {
    if (!SKIPPED_TEXTURES.contains(location)) {
      SKIPPED_TEXTURES.add(location);
      log.debug("Skipping loading texture '{}' as it does not exist in the resource pack", location);
    }
  }

  /**
   * Gets a consumer to add textures to the given collection
   * @param allTextures         Collection of textures
   * @param logMissingTextures  If true, log textures that were not found
   * @return  Texture consumer
   */
  public static Predicate<Material> getTextureAdder(Collection<Material> allTextures, boolean logMissingTextures) {
    ResourceManager manager = Minecraft.getInstance().getResourceManager();
    return mat -> {
      // either must be non-blocks, or must exist. We have fallbacks if it does not exist
      ResourceLocation loc = mat.texture();
      if (!InventoryMenu.BLOCK_ATLAS.equals(mat.atlasLocation()) || textureExists(manager, loc)) {
        allTextures.add(mat);
        return true;
      }
      if (logMissingTextures) {
        logMissingTexture(loc);
      }
      return false;
    };
  }
}
