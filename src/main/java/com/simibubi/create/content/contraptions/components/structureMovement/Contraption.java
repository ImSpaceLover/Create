package com.simibubi.create.content.contraptions.components.structureMovement;

import static com.simibubi.create.content.contraptions.components.structureMovement.piston.MechanicalPistonBlock.isExtensionPole;
import static com.simibubi.create.content.contraptions.components.structureMovement.piston.MechanicalPistonBlock.isPistonHead;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.simibubi.create.foundation.render.backend.instancing.IFlywheelWorld;
import com.simibubi.create.foundation.render.backend.light.GridAlignedBB;
import com.simibubi.create.foundation.utility.*;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.content.contraptions.base.IRotate;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.actors.SeatBlock;
import com.simibubi.create.content.contraptions.components.actors.SeatEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.MechanicalBearingBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.StabilizedContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.chassis.AbstractChassisBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.chassis.ChassisTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.chassis.StickerBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.gantry.GantryCarriageBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.glue.SuperGlueHandler;
import com.simibubi.create.content.contraptions.components.structureMovement.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.piston.MechanicalPistonBlock.PistonState;
import com.simibubi.create.content.contraptions.components.structureMovement.piston.MechanicalPistonHeadBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.pulley.PulleyBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.pulley.PulleyBlock.MagnetBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.pulley.PulleyBlock.RopeBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.pulley.PulleyTileEntity;
import com.simibubi.create.content.contraptions.fluids.tank.FluidTankTileEntity;
import com.simibubi.create.content.contraptions.relays.advanced.GantryShaftBlock;
import com.simibubi.create.content.contraptions.relays.belt.BeltBlock;
import com.simibubi.create.content.logistics.block.inventories.AdjustableCrateBlock;
import com.simibubi.create.content.logistics.block.redstone.RedstoneContactBlock;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.render.backend.light.EmptyLighter;
import com.simibubi.create.foundation.utility.worldWrappers.WrappedWorld;

import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.ChestType;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.state.properties.PistonType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.palette.PaletteHashMap;
import net.minecraft.village.PointOfInterestType;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants.BlockFlags;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.registries.GameData;

public abstract class Contraption {

	public AbstractContraptionEntity entity;
	public CombinedInvWrapper inventory;
	public CombinedTankWrapper fluidInventory;
	public AxisAlignedBB bounds;
	public BlockPos anchor;
	public boolean stalled;

	protected Map<BlockPos, BlockInfo> blocks;
	protected Map<BlockPos, MountedStorage> storage;
	protected Map<BlockPos, MountedFluidStorage> fluidStorage;
	protected List<MutablePair<BlockInfo, MovementContext>> actors;
	protected Set<Pair<BlockPos, Direction>> superglue;
	protected List<BlockPos> seats;
	protected Map<UUID, Integer> seatMapping;
	protected Map<UUID, BlockFace> stabilizedSubContraptions;

	private List<SuperGlueEntity> glueToRemove;
	private Map<BlockPos, Entity> initialPassengers;
	private List<BlockFace> pendingSubContraptions;

	// Client
	public Map<BlockPos, TileEntity> presentTileEntities;
	public List<TileEntity> maybeInstancedTileEntities;
	public List<TileEntity> specialRenderedTileEntities;

	protected ContraptionWorld world;

	public Contraption() {
		blocks = new HashMap<>();
		storage = new HashMap<>();
		seats = new ArrayList<>();
		actors = new ArrayList<>();
		superglue = new HashSet<>();
		seatMapping = new HashMap<>();
		fluidStorage = new HashMap<>();
		glueToRemove = new ArrayList<>();
		initialPassengers = new HashMap<>();
		presentTileEntities = new HashMap<>();
		maybeInstancedTileEntities = new ArrayList<>();
		specialRenderedTileEntities = new ArrayList<>();
		pendingSubContraptions = new ArrayList<>();
		stabilizedSubContraptions = new HashMap<>();
	}

	public ContraptionWorld getContraptionWorld() {
		if (world == null) {
			world = new ContraptionWorld(entity.world, this);
		}

		return world;
	}

	public abstract boolean assemble(World world, BlockPos pos) throws AssemblyException;

	public abstract boolean canBeStabilized(Direction facing, BlockPos localPos);

	protected abstract ContraptionType getType();

	protected boolean customBlockPlacement(IWorld world, BlockPos pos, BlockState state) {
		return false;
	}

	protected boolean customBlockRemoval(IWorld world, BlockPos pos, BlockState state) {
		return false;
	}

	protected boolean addToInitialFrontier(World world, BlockPos pos, Direction forcedDirection,
		Queue<BlockPos> frontier) throws AssemblyException {
		return true;
	}

	public static Contraption fromNBT(World world, CompoundNBT nbt, boolean spawnData) {
		String type = nbt.getString("Type");
		Contraption contraption = ContraptionType.fromType(type);
		contraption.readNBT(world, nbt, spawnData);
		return contraption;
	}

	public boolean searchMovedStructure(World world, BlockPos pos, @Nullable Direction forcedDirection)
		throws AssemblyException {
		initialPassengers.clear();
		Queue<BlockPos> frontier = new UniqueLinkedList<>();
		Set<BlockPos> visited = new HashSet<>();
		anchor = pos;

		if (bounds == null)
			bounds = new AxisAlignedBB(BlockPos.ZERO);

		if (!BlockMovementTraits.isBrittle(world.getBlockState(pos)))
			frontier.add(pos);
		if (!addToInitialFrontier(world, pos, forcedDirection, frontier))
			return false;
		for (int limit = 100000; limit > 0; limit--) {
			if (frontier.isEmpty())
				return true;
			if (!moveBlock(world, forcedDirection, frontier, visited))
				return false;
		}
		throw AssemblyException.structureTooLarge();
	}

	public void onEntityCreated(AbstractContraptionEntity entity) {
		this.entity = entity;

		// Create subcontraptions
		for (BlockFace blockFace : pendingSubContraptions) {
			Direction face = blockFace.getFace();
			StabilizedContraption subContraption = new StabilizedContraption(face);
			World world = entity.world;
			BlockPos pos = blockFace.getPos();
			try {
				if (!subContraption.assemble(world, pos))
					continue;
			} catch (AssemblyException e) {
				continue;
			}
			subContraption.removeBlocksFromWorld(world, BlockPos.ZERO);
			OrientedContraptionEntity movedContraption =
				OrientedContraptionEntity.create(world, subContraption, Optional.of(face));
			BlockPos anchor = blockFace.getConnectedPos();
			movedContraption.setPosition(anchor.getX() + .5f, anchor.getY(), anchor.getZ() + .5f);
			world.addEntity(movedContraption);
			stabilizedSubContraptions.put(movedContraption.getUniqueID(), new BlockFace(toLocalPos(pos), face));
		}

		// Gather itemhandlers of mounted storage
		List<IItemHandlerModifiable> list = storage.values()
			.stream()
			.map(MountedStorage::getItemHandler)
			.collect(Collectors.toList());
		inventory = new CombinedInvWrapper(Arrays.copyOf(list.toArray(), list.size(), IItemHandlerModifiable[].class));

		List<IFluidHandler> fluidHandlers = fluidStorage.values()
			.stream()
			.map(MountedFluidStorage::getFluidHandler)
			.collect(Collectors.toList());
		fluidInventory = new CombinedTankWrapper(
			Arrays.copyOf(fluidHandlers.toArray(), fluidHandlers.size(), IFluidHandler[].class));
	}

	public void onEntityInitialize(World world, AbstractContraptionEntity contraptionEntity) {
		if (world.isRemote)
			return;

		for (OrientedContraptionEntity orientedCE : world.getEntitiesWithinAABB(OrientedContraptionEntity.class,
			contraptionEntity.getBoundingBox()
				.grow(1)))
			if (stabilizedSubContraptions.containsKey(orientedCE.getUniqueID()))
				orientedCE.startRiding(contraptionEntity);

		for (BlockPos seatPos : getSeats()) {
			Entity passenger = initialPassengers.get(seatPos);
			if (passenger == null)
				continue;
			int seatIndex = getSeats().indexOf(seatPos);
			if (seatIndex == -1)
				continue;
			contraptionEntity.addSittingPassenger(passenger, seatIndex);
		}
	}

	public void onEntityTick(World world) {
		fluidStorage.forEach((pos, mfs) -> mfs.tick(entity, pos, world.isRemote));
	}

	/** move the first block in frontier queue */
	protected boolean moveBlock(World world, @Nullable Direction forcedDirection, Queue<BlockPos> frontier,
		Set<BlockPos> visited) throws AssemblyException {
		BlockPos pos = frontier.poll();
		if (pos == null)
			return false;
		visited.add(pos);

		if (World.isOutsideBuildHeight(pos))
			return true;
		if (!world.isBlockPresent(pos))
			throw AssemblyException.unloadedChunk(pos);
		if (isAnchoringBlockAt(pos))
			return true;
		BlockState state = world.getBlockState(pos);
		if (!BlockMovementTraits.movementNecessary(state, world, pos))
			return true;
		if (!movementAllowed(state, world, pos))
			throw AssemblyException.unmovableBlock(pos, state);
		if (state.getBlock() instanceof AbstractChassisBlock
			&& !moveChassis(world, pos, forcedDirection, frontier, visited))
			return false;

		if (AllBlocks.ADJUSTABLE_CRATE.has(state))
			AdjustableCrateBlock.splitCrate(world, pos);

		if (AllBlocks.BELT.has(state))
			moveBelt(pos, frontier, visited, state);

		if (AllBlocks.GANTRY_CARRIAGE.has(state))
			moveGantryPinion(world, pos, frontier, visited, state);

		if (AllBlocks.GANTRY_SHAFT.has(state))
			moveGantryShaft(world, pos, frontier, visited, state);

		if (AllBlocks.STICKER.has(state) && state.get(StickerBlock.EXTENDED)) {
			Direction offset = state.get(StickerBlock.FACING);
			BlockPos attached = pos.offset(offset);
			if (!visited.contains(attached)
				&& !BlockMovementTraits.notSupportive(world.getBlockState(attached), offset.getOpposite()))
				frontier.add(attached);
		}

		// Bearings potentially create stabilized sub-contraptions
		if (AllBlocks.MECHANICAL_BEARING.has(state))
			moveBearing(pos, frontier, visited, state);

		// Seats transfer their passenger to the contraption
		if (state.getBlock() instanceof SeatBlock)
			moveSeat(world, pos);

		// Pulleys drag their rope and their attached structure
		if (state.getBlock() instanceof PulleyBlock)
			movePulley(world, pos, frontier, visited);

		// Pistons drag their attaches poles and extension
		if (state.getBlock() instanceof MechanicalPistonBlock)
			if (!moveMechanicalPiston(world, pos, frontier, visited, state))
				return false;
		if (isExtensionPole(state))
			movePistonPole(world, pos, frontier, visited, state);
		if (isPistonHead(state))
			movePistonHead(world, pos, frontier, visited, state);

		// Doors try to stay whole
		if (state.getBlock() instanceof DoorBlock) {
			BlockPos otherPartPos = pos.up(state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? 1 : -1);
			if (!visited.contains(otherPartPos))
				frontier.add(otherPartPos);
		}

		// Cart assemblers attach themselves
		BlockPos posDown = pos.down();
		BlockState stateBelow = world.getBlockState(posDown);
		if (!visited.contains(posDown) && AllBlocks.CART_ASSEMBLER.has(stateBelow))
			frontier.add(posDown);

		Map<Direction, SuperGlueEntity> superglue = SuperGlueHandler.gatherGlue(world, pos);

		// Slime blocks and super glue drag adjacent blocks if possible
		for (Direction offset : Iterate.directions) {
			BlockPos offsetPos = pos.offset(offset);
			BlockState blockState = world.getBlockState(offsetPos);
			if (isAnchoringBlockAt(offsetPos))
				continue;
			if (!movementAllowed(blockState, world, offsetPos)) {
				if (offset == forcedDirection)
					throw AssemblyException.unmovableBlock(pos, state);
				continue;
			}

			boolean wasVisited = visited.contains(offsetPos);
			boolean faceHasGlue = superglue.containsKey(offset);
			boolean blockAttachedTowardsFace =
				BlockMovementTraits.isBlockAttachedTowards(world, offsetPos, blockState, offset.getOpposite());
			boolean brittle = BlockMovementTraits.isBrittle(blockState);
			boolean canStick = !brittle && state.canStickTo(blockState) && blockState.canStickTo(state);
			if (canStick) {
				if (state.getPushReaction() == PushReaction.PUSH_ONLY
					|| blockState.getPushReaction() == PushReaction.PUSH_ONLY) {
					canStick = false;
				}
				if (BlockMovementTraits.notSupportive(state, offset)) {
					canStick = false;
				}
				if (BlockMovementTraits.notSupportive(blockState, offset.getOpposite())) {
					canStick = false;
				}
			}

			if (!wasVisited && (canStick || blockAttachedTowardsFace || faceHasGlue
				|| (offset == forcedDirection && !BlockMovementTraits.notSupportive(state, forcedDirection))))
				frontier.add(offsetPos);
			if (faceHasGlue)
				addGlue(superglue.get(offset));
		}

		addBlock(pos, capture(world, pos));
		if (blocks.size() <= AllConfigs.SERVER.kinetics.maxBlocksMoved.get())
			return true;
		else
			throw AssemblyException.structureTooLarge();
	}

	protected void movePistonHead(World world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited,
		BlockState state) {
		Direction direction = state.get(MechanicalPistonHeadBlock.FACING);
		BlockPos offset = pos.offset(direction.getOpposite());
		if (!visited.contains(offset)) {
			BlockState blockState = world.getBlockState(offset);
			if (isExtensionPole(blockState) && blockState.get(PistonExtensionPoleBlock.FACING)
				.getAxis() == direction.getAxis())
				frontier.add(offset);
			if (blockState.getBlock() instanceof MechanicalPistonBlock) {
				Direction pistonFacing = blockState.get(MechanicalPistonBlock.FACING);
				if (pistonFacing == direction && blockState.get(MechanicalPistonBlock.STATE) == PistonState.EXTENDED)
					frontier.add(offset);
			}
		}
		if (state.get(MechanicalPistonHeadBlock.TYPE) == PistonType.STICKY) {
			BlockPos attached = pos.offset(direction);
			if (!visited.contains(attached))
				frontier.add(attached);
		}
	}

	protected void movePistonPole(World world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited,
		BlockState state) {
		for (Direction d : Iterate.directionsInAxis(state.get(PistonExtensionPoleBlock.FACING)
			.getAxis())) {
			BlockPos offset = pos.offset(d);
			if (!visited.contains(offset)) {
				BlockState blockState = world.getBlockState(offset);
				if (isExtensionPole(blockState) && blockState.get(PistonExtensionPoleBlock.FACING)
					.getAxis() == d.getAxis())
					frontier.add(offset);
				if (isPistonHead(blockState) && blockState.get(MechanicalPistonHeadBlock.FACING)
					.getAxis() == d.getAxis())
					frontier.add(offset);
				if (blockState.getBlock() instanceof MechanicalPistonBlock) {
					Direction pistonFacing = blockState.get(MechanicalPistonBlock.FACING);
					if (pistonFacing == d || pistonFacing == d.getOpposite()
						&& blockState.get(MechanicalPistonBlock.STATE) == PistonState.EXTENDED)
						frontier.add(offset);
				}
			}
		}
	}

	protected void moveGantryPinion(World world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited,
		BlockState state) {
		BlockPos offset = pos.offset(state.get(GantryCarriageBlock.FACING));
		if (!visited.contains(offset))
			frontier.add(offset);
		Axis rotationAxis = ((IRotate) state.getBlock()).getRotationAxis(state);
		for (Direction d : Iterate.directionsInAxis(rotationAxis)) {
			offset = pos.offset(d);
			BlockState offsetState = world.getBlockState(offset);
			if (AllBlocks.GANTRY_SHAFT.has(offsetState) && offsetState.get(GantryShaftBlock.FACING)
				.getAxis() == d.getAxis())
				if (!visited.contains(offset))
					frontier.add(offset);
		}
	}

	protected void moveGantryShaft(World world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited,
		BlockState state) {
		for (Direction d : Iterate.directions) {
			BlockPos offset = pos.offset(d);
			if (!visited.contains(offset)) {
				BlockState offsetState = world.getBlockState(offset);
				Direction facing = state.get(GantryShaftBlock.FACING);
				if (d.getAxis() == facing.getAxis() && AllBlocks.GANTRY_SHAFT.has(offsetState)
					&& offsetState.get(GantryShaftBlock.FACING) == facing)
					frontier.add(offset);
				else if (AllBlocks.GANTRY_CARRIAGE.has(offsetState) && offsetState.get(GantryCarriageBlock.FACING) == d)
					frontier.add(offset);
			}
		}
	}

	private void moveBearing(BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
		Direction facing = state.get(MechanicalBearingBlock.FACING);
		if (!canBeStabilized(facing, pos.subtract(anchor))) {
			BlockPos offset = pos.offset(facing);
			if (!visited.contains(offset))
				frontier.add(offset);
			return;
		}
		pendingSubContraptions.add(new BlockFace(pos, facing));
	}

	private void moveBelt(BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
		BlockPos nextPos = BeltBlock.nextSegmentPosition(state, pos, true);
		BlockPos prevPos = BeltBlock.nextSegmentPosition(state, pos, false);
		if (nextPos != null && !visited.contains(nextPos))
			frontier.add(nextPos);
		if (prevPos != null && !visited.contains(prevPos))
			frontier.add(prevPos);
	}

	private void moveSeat(World world, BlockPos pos) {
		BlockPos local = toLocalPos(pos);
		getSeats().add(local);
		List<SeatEntity> seatsEntities = world.getEntitiesWithinAABB(SeatEntity.class, new AxisAlignedBB(pos));
		if (!seatsEntities.isEmpty()) {
			SeatEntity seat = seatsEntities.get(0);
			List<Entity> passengers = seat.getPassengers();
			if (!passengers.isEmpty())
				initialPassengers.put(local, passengers.get(0));
		}
	}

	private void movePulley(World world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited) {
		int limit = AllConfigs.SERVER.kinetics.maxRopeLength.get();
		BlockPos ropePos = pos;
		while (limit-- >= 0) {
			ropePos = ropePos.down();
			if (!world.isBlockPresent(ropePos))
				break;
			BlockState ropeState = world.getBlockState(ropePos);
			Block block = ropeState.getBlock();
			if (!(block instanceof RopeBlock) && !(block instanceof MagnetBlock)) {
				if (!visited.contains(ropePos))
					frontier.add(ropePos);
				break;
			}
			addBlock(ropePos, capture(world, ropePos));
		}
	}

	private boolean moveMechanicalPiston(World world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited,
		BlockState state) throws AssemblyException {
		Direction direction = state.get(MechanicalPistonBlock.FACING);
		PistonState pistonState = state.get(MechanicalPistonBlock.STATE);
		if (pistonState == PistonState.MOVING)
			return false;

		BlockPos offset = pos.offset(direction.getOpposite());
		if (!visited.contains(offset)) {
			BlockState poleState = world.getBlockState(offset);
			if (AllBlocks.PISTON_EXTENSION_POLE.has(poleState) && poleState.get(PistonExtensionPoleBlock.FACING)
				.getAxis() == direction.getAxis())
				frontier.add(offset);
		}

		if (pistonState == PistonState.EXTENDED || MechanicalPistonBlock.isStickyPiston(state)) {
			offset = pos.offset(direction);
			if (!visited.contains(offset))
				frontier.add(offset);
		}

		return true;
	}

	private boolean moveChassis(World world, BlockPos pos, Direction movementDirection, Queue<BlockPos> frontier,
		Set<BlockPos> visited) {
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof ChassisTileEntity))
			return false;
		ChassisTileEntity chassis = (ChassisTileEntity) te;
		chassis.addAttachedChasses(frontier, visited);
		List<BlockPos> includedBlockPositions = chassis.getIncludedBlockPositions(movementDirection, false);
		if (includedBlockPositions == null)
			return false;
		for (BlockPos blockPos : includedBlockPositions)
			if (!visited.contains(blockPos))
				frontier.add(blockPos);
		return true;
	}

	protected Pair<BlockInfo, TileEntity> capture(World world, BlockPos pos) {
		BlockState blockstate = world.getBlockState(pos);
		if (blockstate.getBlock() instanceof ChestBlock)
			blockstate = blockstate.with(ChestBlock.TYPE, ChestType.SINGLE);
		if (AllBlocks.ADJUSTABLE_CRATE.has(blockstate))
			blockstate = blockstate.with(AdjustableCrateBlock.DOUBLE, false);
		if (AllBlocks.REDSTONE_CONTACT.has(blockstate))
			blockstate = blockstate.with(RedstoneContactBlock.POWERED, true);
		if (blockstate.getBlock() instanceof AbstractButtonBlock) {
			blockstate = blockstate.with(AbstractButtonBlock.POWERED, false);
			world.getPendingBlockTicks()
				.scheduleTick(pos, blockstate.getBlock(), -1);
		}
		if (blockstate.getBlock() instanceof PressurePlateBlock) {
			blockstate = blockstate.with(PressurePlateBlock.POWERED, false);
			world.getPendingBlockTicks()
				.scheduleTick(pos, blockstate.getBlock(), -1);
		}
		CompoundNBT compoundnbt = getTileEntityNBT(world, pos);
		TileEntity tileentity = world.getTileEntity(pos);
		return Pair.of(new BlockInfo(pos, blockstate, compoundnbt), tileentity);
	}

	protected void addBlock(BlockPos pos, Pair<BlockInfo, TileEntity> pair) {
		BlockInfo captured = pair.getKey();
		BlockPos localPos = pos.subtract(anchor);
		BlockInfo blockInfo = new BlockInfo(localPos, captured.state, captured.nbt);

		if (blocks.put(localPos, blockInfo) != null)
			return;
		bounds = bounds.union(new AxisAlignedBB(localPos));

		TileEntity te = pair.getValue();
		if (te != null && MountedStorage.canUseAsStorage(te))
			storage.put(localPos, new MountedStorage(te));
		if (te != null && MountedFluidStorage.canUseAsStorage(te))
			fluidStorage.put(localPos, new MountedFluidStorage(te));
		if (AllMovementBehaviours.contains(captured.state.getBlock()))
			actors.add(MutablePair.of(blockInfo, null));
	}

	@Nullable
	protected CompoundNBT getTileEntityNBT(World world, BlockPos pos) {
		TileEntity tileentity = world.getTileEntity(pos);
		if (tileentity == null)
			return null;
		CompoundNBT nbt = tileentity.write(new CompoundNBT());
		nbt.remove("x");
		nbt.remove("y");
		nbt.remove("z");

		if (tileentity instanceof FluidTankTileEntity && nbt.contains("Controller"))
			nbt.put("Controller",
				NBTUtil.writeBlockPos(toLocalPos(NBTUtil.readBlockPos(nbt.getCompound("Controller")))));

		return nbt;
	}

	protected void addGlue(SuperGlueEntity entity) {
		BlockPos pos = entity.getHangingPosition();
		Direction direction = entity.getFacingDirection();
		this.superglue.add(Pair.of(toLocalPos(pos), direction));
		glueToRemove.add(entity);
	}

	protected BlockPos toLocalPos(BlockPos globalPos) {
		return globalPos.subtract(anchor);
	}

	protected boolean movementAllowed(BlockState state, World world, BlockPos pos) {
		return BlockMovementTraits.movementAllowed(state, world, pos);
	}

	protected boolean isAnchoringBlockAt(BlockPos pos) {
		return pos.equals(anchor);
	}

	public void readNBT(World world, CompoundNBT nbt, boolean spawnData) {
		blocks.clear();
		presentTileEntities.clear();
		specialRenderedTileEntities.clear();

		INBT blocks = nbt.get("Blocks");
		// used to differentiate between the 'old' and the paletted serialization
		boolean usePalettedDeserialization =
			blocks != null && blocks.getId() == 10 && ((CompoundNBT) blocks).contains("Palette");
		readBlocksCompound(blocks, world, usePalettedDeserialization);

		actors.clear();
		nbt.getList("Actors", 10)
			.forEach(c -> {
				CompoundNBT comp = (CompoundNBT) c;
				BlockInfo info = this.blocks.get(NBTUtil.readBlockPos(comp.getCompound("Pos")));
				MovementContext context = MovementContext.readNBT(world, info, comp, this);
				getActors().add(MutablePair.of(info, context));
			});

		superglue.clear();
		NBTHelper.iterateCompoundList(nbt.getList("Superglue", NBT.TAG_COMPOUND), c -> superglue
			.add(Pair.of(NBTUtil.readBlockPos(c.getCompound("Pos")), Direction.byIndex(c.getByte("Direction")))));

		seats.clear();
		NBTHelper.iterateCompoundList(nbt.getList("Seats", NBT.TAG_COMPOUND), c -> seats.add(NBTUtil.readBlockPos(c)));

		seatMapping.clear();
		NBTHelper.iterateCompoundList(nbt.getList("Passengers", NBT.TAG_COMPOUND),
			c -> seatMapping.put(NBTUtil.readUniqueId(c.getCompound("Id")), c.getInt("Seat")));

		stabilizedSubContraptions.clear();
		NBTHelper.iterateCompoundList(nbt.getList("SubContraptions", NBT.TAG_COMPOUND), c -> stabilizedSubContraptions
			.put(NBTUtil.readUniqueId(c.getCompound("Id")), BlockFace.fromNBT(c.getCompound("Location"))));

		storage.clear();
		NBTHelper.iterateCompoundList(nbt.getList("Storage", NBT.TAG_COMPOUND), c -> storage
			.put(NBTUtil.readBlockPos(c.getCompound("Pos")), MountedStorage.deserialize(c.getCompound("Data"))));

		fluidStorage.clear();
		NBTHelper.iterateCompoundList(nbt.getList("FluidStorage", NBT.TAG_COMPOUND), c -> fluidStorage
			.put(NBTUtil.readBlockPos(c.getCompound("Pos")), MountedFluidStorage.deserialize(c.getCompound("Data"))));

		if (spawnData)
			fluidStorage.forEach((pos, mfs) -> {
				TileEntity tileEntity = presentTileEntities.get(pos);
				if (!(tileEntity instanceof FluidTankTileEntity))
					return;
				FluidTankTileEntity tank = (FluidTankTileEntity) tileEntity;
				IFluidTank tankInventory = tank.getTankInventory();
				if (tankInventory instanceof FluidTank)
					((FluidTank) tankInventory).setFluid(mfs.tank.getFluid());
				tank.getFluidLevel()
					.start(tank.getFillState());
				mfs.assignTileEntity(tank);
			});

		IItemHandlerModifiable[] handlers = new IItemHandlerModifiable[storage.size()];
		int index = 0;
		for (MountedStorage mountedStorage : storage.values())
			handlers[index++] = mountedStorage.getItemHandler();

		IFluidHandler[] fluidHandlers = new IFluidHandler[fluidStorage.size()];
		index = 0;
		for (MountedFluidStorage mountedStorage : fluidStorage.values())
			fluidHandlers[index++] = mountedStorage.getFluidHandler();

		inventory = new CombinedInvWrapper(handlers);
		fluidInventory = new CombinedTankWrapper(fluidHandlers);

		if (nbt.contains("BoundsFront"))
			bounds = NBTHelper.readAABB(nbt.getList("BoundsFront", 5));

		stalled = nbt.getBoolean("Stalled");
		anchor = NBTUtil.readBlockPos(nbt.getCompound("Anchor"));
	}

	public CompoundNBT writeNBT(boolean spawnPacket) {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putString("Type", getType().id);

		CompoundNBT blocksNBT = writeBlocksCompound();

		ListNBT actorsNBT = new ListNBT();
		for (MutablePair<BlockInfo, MovementContext> actor : getActors()) {
			CompoundNBT compound = new CompoundNBT();
			compound.put("Pos", NBTUtil.writeBlockPos(actor.left.pos));
			AllMovementBehaviours.of(actor.left.state)
				.writeExtraData(actor.right);
			actor.right.writeToNBT(compound);
			actorsNBT.add(compound);
		}

		ListNBT superglueNBT = new ListNBT();
		ListNBT storageNBT = new ListNBT();
		if (!spawnPacket) {
			for (Pair<BlockPos, Direction> glueEntry : superglue) {
				CompoundNBT c = new CompoundNBT();
				c.put("Pos", NBTUtil.writeBlockPos(glueEntry.getKey()));
				c.putByte("Direction", (byte) glueEntry.getValue()
					.getIndex());
				superglueNBT.add(c);
			}

			for (BlockPos pos : storage.keySet()) {
				CompoundNBT c = new CompoundNBT();
				MountedStorage mountedStorage = storage.get(pos);
				if (!mountedStorage.isValid())
					continue;
				c.put("Pos", NBTUtil.writeBlockPos(pos));
				c.put("Data", mountedStorage.serialize());
				storageNBT.add(c);
			}
		}

		ListNBT fluidStorageNBT = new ListNBT();
		for (BlockPos pos : fluidStorage.keySet()) {
			CompoundNBT c = new CompoundNBT();
			MountedFluidStorage mountedStorage = fluidStorage.get(pos);
			if (!mountedStorage.isValid())
				continue;
			c.put("Pos", NBTUtil.writeBlockPos(pos));
			c.put("Data", mountedStorage.serialize());
			fluidStorageNBT.add(c);
		}

		nbt.put("Seats", NBTHelper.writeCompoundList(getSeats(), NBTUtil::writeBlockPos));
		nbt.put("Passengers", NBTHelper.writeCompoundList(getSeatMapping().entrySet(), e -> {
			CompoundNBT tag = new CompoundNBT();
			tag.put("Id", NBTUtil.writeUniqueId(e.getKey()));
			tag.putInt("Seat", e.getValue());
			return tag;
		}));

		nbt.put("SubContraptions", NBTHelper.writeCompoundList(stabilizedSubContraptions.entrySet(), e -> {
			CompoundNBT tag = new CompoundNBT();
			tag.put("Id", NBTUtil.writeUniqueId(e.getKey()));
			tag.put("Location", e.getValue()
				.serializeNBT());
			return tag;
		}));

		nbt.put("Blocks", blocksNBT);
		nbt.put("Actors", actorsNBT);
		nbt.put("Superglue", superglueNBT);
		nbt.put("Storage", storageNBT);
		nbt.put("FluidStorage", fluidStorageNBT);
		nbt.put("Anchor", NBTUtil.writeBlockPos(anchor));
		nbt.putBoolean("Stalled", stalled);

		if (bounds != null) {
			ListNBT bb = NBTHelper.writeAABB(bounds);
			nbt.put("BoundsFront", bb);
		}

		return nbt;
	}

	private CompoundNBT writeBlocksCompound() {
		CompoundNBT compound = new CompoundNBT();
		PaletteHashMap<BlockState> palette = new PaletteHashMap<>(GameData.getBlockStateIDMap(), 16, (i, s) -> {
			throw new IllegalStateException("Palette Map index exceeded maximum");
		}, NBTUtil::readBlockState, NBTUtil::writeBlockState);
		ListNBT blockList = new ListNBT();

		for (BlockInfo block : this.blocks.values()) {
			int id = palette.idFor(block.state);
			CompoundNBT c = new CompoundNBT();
			c.putLong("Pos", block.pos.toLong());
			c.putInt("State", id);
			if (block.nbt != null)
				c.put("Data", block.nbt);
			blockList.add(c);
		}

		ListNBT paletteNBT = new ListNBT();
		palette.writePaletteToList(paletteNBT);
		compound.put("Palette", paletteNBT);
		compound.put("BlockList", blockList);

		return compound;
	}

	private void readBlocksCompound(INBT compound, World world, boolean usePalettedDeserialization) {
		PaletteHashMap<BlockState> palette = null;
		ListNBT blockList;
		if (usePalettedDeserialization) {
			CompoundNBT c = ((CompoundNBT) compound);
			palette = new PaletteHashMap<>(GameData.getBlockStateIDMap(), 16, (i, s) -> {
				throw new IllegalStateException("Palette Map index exceeded maximum");
			}, NBTUtil::readBlockState, NBTUtil::writeBlockState);
			palette.read(c.getList("Palette", 10));

			blockList = c.getList("BlockList", 10);
		} else {
			blockList = (ListNBT) compound;
		}

		PaletteHashMap<BlockState> finalPalette = palette;
		blockList.forEach(e -> {
			CompoundNBT c = (CompoundNBT) e;

			BlockInfo info = usePalettedDeserialization ? readBlockInfo(c, finalPalette) : legacyReadBlockInfo(c);

			this.blocks.put(info.pos, info);

			if (world.isRemote) {
				Block block = info.state.getBlock();
				CompoundNBT tag = info.nbt;
				MovementBehaviour movementBehaviour = AllMovementBehaviours.of(block);
				if (tag == null)
					return;

				tag.putInt("x", info.pos.getX());
				tag.putInt("y", info.pos.getY());
				tag.putInt("z", info.pos.getZ());

				TileEntity te = TileEntity.create(tag);
				if (te == null)
					return;
				te.setLocation(new ContraptionTileWorld(world, te, info), te.getPos());
				if (te instanceof KineticTileEntity)
					((KineticTileEntity) te).setSpeed(0);
				te.getBlockState();

				if (movementBehaviour == null || !movementBehaviour.hasSpecialInstancedRendering())
					maybeInstancedTileEntities.add(te);

				if (movementBehaviour != null && !movementBehaviour.renderAsNormalTileEntity())
					return;

				presentTileEntities.put(info.pos, te);
				specialRenderedTileEntities.add(te);
			}

		});
	}

	private static BlockInfo readBlockInfo(CompoundNBT blockListEntry, PaletteHashMap<BlockState> palette) {
		return new BlockInfo(BlockPos.fromLong(blockListEntry.getLong("Pos")),
			Objects.requireNonNull(palette.get(blockListEntry.getInt("State"))),
			blockListEntry.contains("Data") ? blockListEntry.getCompound("Data") : null);
	}

	private static BlockInfo legacyReadBlockInfo(CompoundNBT blockListEntry) {
		return new BlockInfo(NBTUtil.readBlockPos(blockListEntry.getCompound("Pos")),
			NBTUtil.readBlockState(blockListEntry.getCompound("Block")),
			blockListEntry.contains("Data") ? blockListEntry.getCompound("Data") : null);
	}

	public void removeBlocksFromWorld(World world, BlockPos offset) {
		storage.values()
			.forEach(MountedStorage::removeStorageFromWorld);
		fluidStorage.values()
			.forEach(MountedFluidStorage::removeStorageFromWorld);
		glueToRemove.forEach(SuperGlueEntity::remove);

		for (boolean brittles : Iterate.trueAndFalse) {
			for (Iterator<BlockInfo> iterator = blocks.values()
				.iterator(); iterator.hasNext();) {
				BlockInfo block = iterator.next();
				if (brittles != BlockMovementTraits.isBrittle(block.state))
					continue;

				BlockPos add = block.pos.add(anchor)
					.add(offset);
				if (customBlockRemoval(world, add, block.state))
					continue;
				BlockState oldState = world.getBlockState(add);
				Block blockIn = oldState.getBlock();
				if (block.state.getBlock() != blockIn)
					iterator.remove();
				world.getWorld()
					.removeTileEntity(add);
				int flags = BlockFlags.IS_MOVING | BlockFlags.NO_NEIGHBOR_DROPS | BlockFlags.UPDATE_NEIGHBORS
					| BlockFlags.BLOCK_UPDATE | BlockFlags.RERENDER_MAIN_THREAD;
				if (blockIn instanceof IWaterLoggable && oldState.has(BlockStateProperties.WATERLOGGED)
					&& oldState.get(BlockStateProperties.WATERLOGGED)
						.booleanValue()) {
					world.setBlockState(add, Blocks.WATER.getDefaultState(), flags);
					continue;
				}
				world.setBlockState(add, Blocks.AIR.getDefaultState(), flags);
			}
		}
		for (BlockInfo block : blocks.values()) {
			BlockPos add = block.pos.add(anchor)
				.add(offset);
//			if (!shouldUpdateAfterMovement(block))
//				continue;
			int flags = BlockFlags.IS_MOVING | BlockFlags.DEFAULT;
			world.notifyBlockUpdate(add, block.state, Blocks.AIR.getDefaultState(), flags);
			world.notifyNeighbors(add, block.state.getBlock());
			block.state.updateDiagonalNeighbors(world, add, flags & -2);
//			world.markAndNotifyBlock(add, null, block.state, Blocks.AIR.getDefaultState(),
//				BlockFlags.IS_MOVING | BlockFlags.DEFAULT); this method did strange logspamming with POI-related blocks
		}
	}

	public void addBlocksToWorld(World world, StructureTransform transform) {
		for (boolean nonBrittles : Iterate.trueAndFalse) {
			for (BlockInfo block : blocks.values()) {
				if (nonBrittles == BlockMovementTraits.isBrittle(block.state))
					continue;

				BlockPos targetPos = transform.apply(block.pos);
				BlockState state = transform.apply(block.state);

				if (customBlockPlacement(world, targetPos, state))
					continue;

				if (nonBrittles)
					for (Direction face : Iterate.directions)
						state = state.updatePostPlacement(face, world.getBlockState(targetPos.offset(face)), world,
							targetPos, targetPos.offset(face));

				BlockState blockState = world.getBlockState(targetPos);
				if (blockState.getBlockHardness(world, targetPos) == -1 || (state.getCollisionShape(world, targetPos)
					.isEmpty()
					&& !blockState.getCollisionShape(world, targetPos)
						.isEmpty())) {
					if (targetPos.getY() == 0)
						targetPos = targetPos.up();
					world.playEvent(2001, targetPos, Block.getStateId(state));
					Block.spawnDrops(state, world, targetPos, null);
					continue;
				}
				if (state.getBlock() instanceof IWaterLoggable && state.has(BlockStateProperties.WATERLOGGED)) {
					IFluidState ifluidstate = world.getFluidState(targetPos);
					state = state.with(BlockStateProperties.WATERLOGGED,
						Boolean.valueOf(ifluidstate.getFluid() == Fluids.WATER));
				}

				world.destroyBlock(targetPos, true);
				world.setBlockState(targetPos, state, 3 | BlockFlags.IS_MOVING);

				boolean verticalRotation = transform.rotationAxis == null || transform.rotationAxis.isHorizontal();
				verticalRotation = verticalRotation && transform.rotation != Rotation.NONE;
				if (verticalRotation) {
					if (state.getBlock() instanceof RopeBlock || state.getBlock() instanceof MagnetBlock)
						world.destroyBlock(targetPos, true);
				}

				TileEntity tileEntity = world.getTileEntity(targetPos);
				CompoundNBT tag = block.nbt;
				if (tileEntity != null)
					tag = NBTProcessors.process(tileEntity, tag, false);
				if (tileEntity != null && tag != null) {
					tag.putInt("x", targetPos.getX());
					tag.putInt("y", targetPos.getY());
					tag.putInt("z", targetPos.getZ());

					if (verticalRotation && tileEntity instanceof PulleyTileEntity) {
						tag.remove("Offset");
						tag.remove("InitialOffset");
					}

					if (tileEntity instanceof FluidTankTileEntity && tag.contains("LastKnownPos"))
						tag.put("LastKnownPos", NBTUtil.writeBlockPos(BlockPos.ZERO.down()));

					tileEntity.read(tag);

					if (storage.containsKey(block.pos)) {
						MountedStorage mountedStorage = storage.get(block.pos);
						if (mountedStorage.isValid())
							mountedStorage.addStorageToWorld(tileEntity);
					}

					if (fluidStorage.containsKey(block.pos)) {
						MountedFluidStorage mountedStorage = fluidStorage.get(block.pos);
						if (mountedStorage.isValid())
							mountedStorage.addStorageToWorld(tileEntity);
					}
				}
			}
		}
		for (BlockInfo block : blocks.values()) {
			if (!shouldUpdateAfterMovement(block))
				continue;
			BlockPos targetPos = transform.apply(block.pos);
			world.markAndNotifyBlock(targetPos, null, block.state, block.state,
				BlockFlags.IS_MOVING | BlockFlags.DEFAULT);
		}

		for (int i = 0; i < inventory.getSlots(); i++)
			inventory.setStackInSlot(i, ItemStack.EMPTY);
		for (int i = 0; i < fluidInventory.getTanks(); i++)
			fluidInventory.drain(fluidInventory.getFluidInTank(i), FluidAction.EXECUTE);

		for (Pair<BlockPos, Direction> pair : superglue) {
			BlockPos targetPos = transform.apply(pair.getKey());
			Direction targetFacing = transform.transformFacing(pair.getValue());

			SuperGlueEntity entity = new SuperGlueEntity(world, targetPos, targetFacing);
			if (entity.onValidSurface()) {
				if (!world.isRemote)
					world.addEntity(entity);
			}
		}
	}

	public void addPassengersToWorld(World world, StructureTransform transform, List<Entity> seatedEntities) {
		for (Entity seatedEntity : seatedEntities) {
			if (getSeatMapping().isEmpty())
				continue;
			Integer seatIndex = getSeatMapping().get(seatedEntity.getUniqueID());
			BlockPos seatPos = getSeats().get(seatIndex);
			seatPos = transform.apply(seatPos);
			if (!(world.getBlockState(seatPos)
				.getBlock() instanceof SeatBlock))
				continue;
			if (SeatBlock.isSeatOccupied(world, seatPos))
				continue;
			SeatBlock.sitDown(world, seatPos, seatedEntity);
		}
	}

	public void startMoving(World world) {
		for (MutablePair<BlockInfo, MovementContext> pair : actors) {
			MovementContext context = new MovementContext(world, pair.left, this);
			AllMovementBehaviours.of(pair.left.state)
				.startMoving(context);
			pair.setRight(context);
		}
	}

	public void stop(World world) {
		foreachActor(world, (behaviour, ctx) -> {
			behaviour.stopMoving(ctx);
			ctx.position = null;
			ctx.motion = Vec3d.ZERO;
			ctx.relativeMotion = Vec3d.ZERO;
			ctx.rotation = v -> v;
		});
	}

	public void foreachActor(World world, BiConsumer<MovementBehaviour, MovementContext> callBack) {
		for (MutablePair<BlockInfo, MovementContext> pair : actors)
			callBack.accept(AllMovementBehaviours.of(pair.getLeft().state), pair.getRight());
	}

	protected boolean shouldUpdateAfterMovement(BlockInfo info) {
		if (PointOfInterestType.forState(info.state)
			.isPresent())
			return false;
		return true;
	}

	public void expandBoundsAroundAxis(Axis axis) {
		Set<BlockPos> blocks = getBlocks().keySet();

		int radius = (int) (Math.ceil(Math.sqrt(getRadius(blocks, axis))));

		GridAlignedBB betterBounds = GridAlignedBB.ofRadius(radius);

		GridAlignedBB contraptionBounds = GridAlignedBB.from(bounds);
		if (axis == Direction.Axis.X) {
			betterBounds.maxX = contraptionBounds.maxX;
			betterBounds.minX = contraptionBounds.minX;
		} else if (axis == Direction.Axis.Y) {
			betterBounds.maxY = contraptionBounds.maxY;
			betterBounds.minY = contraptionBounds.minY;
		} else if (axis == Direction.Axis.Z) {
			betterBounds.maxZ = contraptionBounds.maxZ;
			betterBounds.minZ = contraptionBounds.minZ;
		}

		bounds = betterBounds.toAABB();
	}

	public void addExtraInventories(Entity entity) {}

	public Map<UUID, Integer> getSeatMapping() {
		return seatMapping;
	}

	public BlockPos getSeatOf(UUID entityId) {
		if (!getSeatMapping().containsKey(entityId))
			return null;
		int seatIndex = getSeatMapping().get(entityId);
		if (seatIndex >= getSeats().size())
			return null;
		return getSeats().get(seatIndex);
	}

	public BlockPos getBearingPosOf(UUID subContraptionEntityId) {
		if (stabilizedSubContraptions.containsKey(subContraptionEntityId))
			return stabilizedSubContraptions.get(subContraptionEntityId)
				.getConnectedPos();
		return null;
	}

	public void setSeatMapping(Map<UUID, Integer> seatMapping) {
		this.seatMapping = seatMapping;
	}

	public List<BlockPos> getSeats() {
		return seats;
	}

	public Map<BlockPos, BlockInfo> getBlocks() {
		return blocks;
	}

	public List<MutablePair<BlockInfo, MovementContext>> getActors() {
		return actors;
	}

	public void updateContainedFluid(BlockPos localPos, FluidStack containedFluid) {
		MountedFluidStorage mountedFluidStorage = fluidStorage.get(localPos);
		if (mountedFluidStorage != null)
			mountedFluidStorage.updateFluid(containedFluid);
	}

	@OnlyIn(Dist.CLIENT)
	public ContraptionLighter<?> makeLighter() {
		return new EmptyLighter(this);
	}

	public static float getRadius(Set<BlockPos> blocks, Direction.Axis axis) {
		switch (axis) {
		case X:
			return getMaxDistSqr(blocks, BlockPos::getY, BlockPos::getZ);
		case Y:
			return getMaxDistSqr(blocks, BlockPos::getX, BlockPos::getZ);
		case Z:
			return getMaxDistSqr(blocks, BlockPos::getX, BlockPos::getY);
		}

		throw new IllegalStateException("Impossible axis");
	}

	public static float getMaxDistSqr(Set<BlockPos> blocks, Coordinate one, Coordinate other) {
		float maxDistSq = -1;
		for (BlockPos pos : blocks) {
			float a = one.get(pos);
			float b = other.get(pos);

			float distSq = a * a + b * b;


			if (distSq > maxDistSq) maxDistSq = distSq;
		}

		return maxDistSq;
	}

	private static class ContraptionTileWorld extends WrappedWorld implements IFlywheelWorld {

		private final TileEntity te;
		private final BlockInfo info;

		public ContraptionTileWorld(World world, TileEntity te, BlockInfo info) {
			super(world);
			this.te = te;
			this.info = info;
		}

		@Override
		public BlockState getBlockState(BlockPos pos) {
			if (!pos.equals(te.getPos()))
				return Blocks.AIR.getDefaultState();
			return info.state;
		}

	}
}
