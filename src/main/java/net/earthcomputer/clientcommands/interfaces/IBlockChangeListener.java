package net.earthcomputer.clientcommands.interfaces;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface IBlockChangeListener {

    List<IBlockChangeListener> LISTENERS = new ArrayList<>();

    void onBlockChange(BlockPos pos, BlockState oldState, BlockState newState);

}
