package net.earthcomputer.clientcommands.command;

import java.util.Collection;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;

public class FakeCommandSource extends CommandSourceStack {
    public FakeCommandSource(LocalPlayer player) {
        super(player, player.position(), player.getRotationVector(), null, 314159265, player.getScoreboardName(), player.getName(), null, player);
    }

    @Override
    public Collection<String> getOnlinePlayerNames() {
        return Minecraft.getInstance().getConnection().getOnlinePlayers()
                .stream().map(e -> e.getProfile().getName()).collect(Collectors.toList());
    }

    @Override
    public RegistryAccess registryAccess() {
        return Minecraft.getInstance().getConnection().registryAccess();
    }
}
