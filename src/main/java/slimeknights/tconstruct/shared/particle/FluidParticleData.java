package slimeknights.tconstruct.shared.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.Codec;
import io.github.fabricators_of_create.porting_lib.util.FluidAttributes;
import io.github.fabricators_of_create.porting_lib.util.FluidStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

/** Particle data for a fluid particle */
@SuppressWarnings("removal")
@RequiredArgsConstructor
public class FluidParticleData implements ParticleOptions {
  private static final DynamicCommandExceptionType UNKNOWN_FLUID = new DynamicCommandExceptionType(arg -> Component.translatable("command.tconstruct.fluid.not_found", arg));
  private static final ParticleOptions.Deserializer<FluidParticleData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
    @Override
    public FluidParticleData fromCommand(ParticleType<FluidParticleData> type, StringReader reader) throws CommandSyntaxException {
      reader.expect(' ');
      int i = reader.getCursor();
      ResourceLocation id = ResourceLocation.read(reader);
      Fluid fluid = Registry.FLUID.getOptional(id).orElseThrow(() -> {
        reader.setCursor(i);
        return UNKNOWN_FLUID.createWithContext(reader, id.toString());
      });
      CompoundTag nbt = null;
      if (reader.canRead() && reader.peek() == '{') {
        nbt = new TagParser(reader).readStruct();
      }
      return new FluidParticleData(type, new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME, nbt));
    }

    @Override
    public FluidParticleData fromNetwork(ParticleType<FluidParticleData> type, FriendlyByteBuf buffer) {
      return new FluidParticleData(type, FluidStack.fromBuffer(buffer));
    }
  };

  @Getter
  private final ParticleType<FluidParticleData> type;
  @Getter
  private final FluidStack fluid;

  @Override
  public void writeToNetwork(FriendlyByteBuf buffer) {
    fluid.toBuffer(buffer);
  }

  @Override
  public String writeToString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getType().getRegistryName());
    builder.append(" ");
    builder.append(fluid.getFluid().getRegistryName());
    CompoundTag nbt = fluid.getTag();
    if (nbt != null) {
      builder.append(nbt);
    }
    return builder.toString();
  }

  /** Particle type for a fluid particle */
  public static class Type extends ParticleType<FluidParticleData> {
    public Type() {
      super(false, DESERIALIZER);
    }

    @Override
    public Codec<FluidParticleData> codec() {
      return FluidStack.CODEC.xmap(fluid -> new FluidParticleData(this, fluid), data -> data.fluid);
    }
  }
}
