package com.github.talrey.createdeco.blocks;

import com.github.talrey.createdeco.Registration;
import com.simibubi.create.content.contraptions.wrench.IWrenchable;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Map;

public class CatwalkStairBlock extends Block implements IWrenchable, SimpleWaterloggedBlock {

  private static final VoxelShape BOX_NORTH = Shapes.join(
    Block.box(0d, 14d, 8d, 16d, 16d, 16d),
    Block.box(0d, 6d, 0d, 16d, 8d, 8d),
    BooleanOp.OR
  );
  private static final VoxelShape BOX_SOUTH = Shapes.join(
    Block.box(0d, 14d, 0d, 16d, 16d, 8d),
    Block.box(0d, 6d, 8d, 16d, 8d, 16d),
    BooleanOp.OR
  );
  private static final VoxelShape BOX_WEST = Shapes.join(
    Block.box(8d, 14d, 0d, 16d, 16d, 16d),
    Block.box(0d, 6d, 0d, 8d, 8d, 16d),
    BooleanOp.OR
  );
  private static final VoxelShape BOX_EAST = Shapes.join(
    Block.box(0d, 14d, 0d, 8d, 16d, 16),
    Block.box(8d, 6d, 0d, 16d, 8d, 16d),
    BooleanOp.OR
  );

  public CatwalkStairBlock (Properties props) {
    super(props);
    this.registerDefaultState(this.defaultBlockState()
      .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
      .setValue(BlockStateProperties.WATERLOGGED, false)
    );
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement (BlockPlaceContext ctx) {
    Direction facing = ctx.getHorizontalDirection();
    FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());

    BlockState state = defaultBlockState()
      .setValue(BlockStateProperties.HORIZONTAL_FACING, facing.getOpposite())
      .setValue(BlockStateProperties.WATERLOGGED, fluid.getType() == Fluids.WATER);

    BlockState below = ctx.getLevel().getBlockState(ctx.getClickedPos().below());
    for (Map.Entry<String, BlockEntry<CatwalkStairBlock>> keypair : Registration.CATWALK_STAIRS.entrySet()) {
      if (keypair.getValue().get().asItem() == below.getBlock().asItem()
      && ctx.getPlayer() != null && !ctx.getPlayer().isCrouching()
      ) { // it's another stair
        BlockPos next = ctx.getClickedPos().relative(facing);
        if (ctx.getLevel().getBlockState(next).canBeReplaced(ctx)) {
          state = state.setValue(
            BlockStateProperties.WATERLOGGED,
            ctx.getLevel().getBlockState(next).getFluidState().is(Fluids.WATER)
          );
          ctx.getLevel().setBlockAndUpdate(next, state);
          ctx.getLevel().playSound(
            null, ctx.getClickedPos(), SoundEvents.NETHERITE_BLOCK_PLACE,
            SoundSource.BLOCKS, 0.5f, 1.25f
          );
          if (!ctx.getPlayer().isCreative()) {
            ctx.getPlayer().getItemInHand(ctx.getHand()).shrink(1);
          }
          state = ctx.getLevel().getBlockState(ctx.getClickedPos()); // keep previous
        }
      }
    }
    return state;
  }

  @Override
  protected void createBlockStateDefinition (StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(BlockStateProperties.HORIZONTAL_FACING);
    builder.add(BlockStateProperties.WATERLOGGED);
  }

  @Override
  public boolean canPlaceLiquid (BlockGetter world, BlockPos pos, BlockState state, Fluid fluid) {
    return !state.getValue(BlockStateProperties.WATERLOGGED) && fluid == Fluids.WATER;
  }

  @Override
  public FluidState getFluidState(BlockState state) {
    return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
  }

  @Override
  public VoxelShape getShape (BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
    return switch(state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
      case SOUTH -> BOX_SOUTH;
      case EAST  -> BOX_EAST;
      case WEST  -> BOX_WEST;
      default    -> BOX_NORTH;
    };
  }
}
