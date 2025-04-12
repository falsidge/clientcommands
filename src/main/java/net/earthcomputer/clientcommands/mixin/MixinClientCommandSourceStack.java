package net.earthcomputer.clientcommands.mixin;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.suggestion.Suggestion;
import net.earthcomputer.clientcommands.command.Flag;
import net.earthcomputer.clientcommands.interfaces.IClientSuggestionsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;
@Pseudo
@Mixin(CommandSourceStack.class)
public abstract class MixinClientCommandSourceStack implements IClientSuggestionsProvider {
    @Shadow
    @Final
    private Vec3 worldPosition;


    @Shadow
    public abstract CommandSourceStack withPosition(Vec3 p_81349_);

    @Unique
    private ImmutableMap<Flag<?>, Object> flags = ImmutableMap.of();

    @SuppressWarnings("unchecked")
    @Override
    public <T> T clientcommands_getFlag(Flag<T> flag) {
        return (T) this.flags.getOrDefault(flag, flag.getDefaultValue());
    }

    @Override
    public <T> IClientSuggestionsProvider clientcommands_withFlag(Flag<T> flag, T value) {
        MixinClientCommandSourceStack source = (MixinClientCommandSourceStack) (Object) this.withPosition(worldPosition);
        source.flags = ImmutableMap.<Flag<?>, Object>builderWithExpectedSize(this.flags.size() + 1).putAll(this.flags).put(flag, value).build();
        return source;
    }

    @Override
    @Nullable
    public List<Suggestion> clientcommands_filterSuggestions(List<Suggestion> suggestions) {
        if (flags.isEmpty()) {
            return null;
        } else {
            return suggestions.stream().filter(suggestion -> {
                String text = suggestion.getText();
                return !Flag.isFlag(text) || flags.keySet().stream().noneMatch(arg -> !arg.isRepeatable() && (text.equals(arg.getFlag()) || text.equals(arg.getShortFlag())));
            }).toList();
        }
    }
}