package com.simibubi.create.events;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllFluids;
import com.simibubi.create.Create;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.KineticDebugger;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionHandler;
import com.simibubi.create.content.contraptions.components.structureMovement.chassis.ChassisRangeDisplay;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionRenderDispatcher;
import com.simibubi.create.content.contraptions.components.structureMovement.train.CouplingHandlerClient;
import com.simibubi.create.content.contraptions.components.structureMovement.train.CouplingPhysics;
import com.simibubi.create.content.contraptions.components.structureMovement.train.CouplingRenderer;
import com.simibubi.create.content.contraptions.components.structureMovement.train.capability.CapabilityMinecartController;
import com.simibubi.create.content.contraptions.components.turntable.TurntableHandler;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.relays.belt.item.BeltConnectorHandler;
import com.simibubi.create.content.curiosities.tools.ExtendoGripRenderHandler;
import com.simibubi.create.content.curiosities.zapper.ZapperItem;
import com.simibubi.create.content.curiosities.zapper.ZapperRenderHandler;
import com.simibubi.create.content.curiosities.zapper.blockzapper.BlockzapperRenderHandler;
import com.simibubi.create.content.curiosities.zapper.terrainzapper.WorldshaperRenderHandler;
import com.simibubi.create.content.logistics.block.depot.EjectorTargetHandler;
import com.simibubi.create.content.logistics.block.mechanicalArm.ArmInteractionPointHandler;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.networking.AllPackets;
import com.simibubi.create.foundation.networking.LeftClickPacket;
import com.simibubi.create.foundation.ponder.PonderTooltipHandler;
import com.simibubi.create.foundation.render.KineticRenderer;
import com.simibubi.create.foundation.render.backend.FastRenderDispatcher;
import com.simibubi.create.foundation.render.backend.RenderWork;
import com.simibubi.create.foundation.renderState.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.tileEntity.behaviour.edgeInteraction.EdgeInteractionRenderer;
import com.simibubi.create.foundation.tileEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.tileEntity.behaviour.linked.LinkRenderer;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollValueHandler;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollValueRenderer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import com.simibubi.create.foundation.utility.placement.PlacementHelpers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvents {

	private static final String itemPrefix = "item." + Create.ID;
	private static final String blockPrefix = "block." + Create.ID;

	@SubscribeEvent
	public static void onTick(ClientTickEvent event) {
		World world = Minecraft.getInstance().world;
		if (event.phase == Phase.START)
			return;

		if (!isGameActive())
			return;

		AnimationTickHolder.tick();
		FastRenderDispatcher.tick();
		ScrollValueHandler.tick();

		CreateClient.schematicSender.tick();
		CreateClient.schematicAndQuillHandler.tick();
		CreateClient.schematicHandler.tick();

		ContraptionHandler.tick(world);
		CapabilityMinecartController.tick(world);
		CouplingPhysics.tick(world);

		PonderTooltipHandler.tick();
		//ScreenOpener.tick();
		ServerSpeedProvider.clientTick();
		BeltConnectorHandler.tick();
		FilteringRenderer.tick();
		LinkRenderer.tick();
		ScrollValueRenderer.tick();
		ChassisRangeDisplay.tick();
		EdgeInteractionRenderer.tick();
		WorldshaperRenderHandler.tick();
		BlockzapperRenderHandler.tick();
		CouplingHandlerClient.tick();
		CouplingRenderer.tickDebugModeRenders();
		KineticDebugger.tick();
		ZapperRenderHandler.tick();
		ExtendoGripRenderHandler.tick();
//		CollisionDebugger.tick();
		ArmInteractionPointHandler.tick();
		EjectorTargetHandler.tick();
		PlacementHelpers.tick();
		CreateClient.outliner.tickOutlines();
		CreateClient.ghostBlocks.tickGhosts();
		ContraptionRenderDispatcher.tick();
	}

	@SubscribeEvent
	public static void onLoadWorld(WorldEvent.Load event) {
		IWorld world = event.getWorld();
		if (world.isRemote() && world instanceof ClientWorld) {
			CreateClient.invalidateRenderers();
			AnimationTickHolder.reset();
			KineticRenderer renderer = CreateClient.kineticRenderer.get(world);
			renderer.invalidate();
			((ClientWorld) world).loadedTileEntityList.forEach(renderer::add);
		}

		/*
		i was getting nullPointers when trying to call this during client setup,
		so i assume minecraft's language manager isn't yet fully loaded at that time.
		not sure where else to call this tho :S
		*/
		IHaveGoggleInformation.numberFormat.update();
	}

	@SubscribeEvent
	public static void onUnloadWorld(WorldEvent.Unload event) {
		if (event.getWorld().isRemote()) {
			CreateClient.invalidateRenderers(event.getWorld());
			AnimationTickHolder.reset();
		}
	}

	@SubscribeEvent
	public static void onRenderWorld(RenderWorldLastEvent event) {
		Vec3d cameraPos = Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView();
		float pt = AnimationTickHolder.getPartialTicks();

		MatrixStack ms = event.getMatrixStack();
		ms.push();
		ms.translate(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ());
		SuperRenderTypeBuffer buffer = SuperRenderTypeBuffer.getInstance();

		CouplingRenderer.renderAll(ms, buffer);
		CreateClient.schematicHandler.render(ms, buffer);
		CreateClient.ghostBlocks.renderAll(ms, buffer);

		CreateClient.outliner.renderOutlines(ms, buffer, pt);
//		LightVolumeDebugger.render(ms, buffer);
		buffer.draw();
		RenderSystem.enableCull();

		ms.pop();

		RenderWork.runAll();
		FastRenderDispatcher.endFrame();
	}

	@SubscribeEvent
	public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
		if (event.getType() != ElementType.HOTBAR)
			return;

		onRenderHotbar(new MatrixStack(), Minecraft.getInstance()
				.getBufferBuilders()
				.getEntityVertexConsumers(), 0xF000F0, OverlayTexture.DEFAULT_UV);
	}

	public static void onRenderHotbar(MatrixStack ms, IRenderTypeBuffer buffer, int light, int overlay) {
		CreateClient.schematicHandler.renderOverlay(ms, buffer, light, overlay);
	}

	@SubscribeEvent
	public static void getItemTooltipColor(RenderTooltipEvent.Color event) {
		PonderTooltipHandler.handleTooltipColor(event);
	}
	
	@SubscribeEvent
	public static void addToItemTooltip(ItemTooltipEvent event) {
		if (!AllConfigs.CLIENT.tooltips.get())
			return;
		if (event.getPlayer() == null)
			return;

		ItemStack stack = event.getItemStack();
		String translationKey = stack.getItem()
				.getTranslationKey(stack);
		if (!translationKey.startsWith(itemPrefix) && !translationKey.startsWith(blockPrefix))
			return;

		if (TooltipHelper.hasTooltip(stack, event.getPlayer())) {
			List<ITextComponent> itemTooltip = event.getToolTip();
			List<ITextComponent> toolTip = new ArrayList<>();
			toolTip.add(itemTooltip.remove(0));
			TooltipHelper.getTooltip(stack)
					.addInformation(toolTip);
			itemTooltip.addAll(0, toolTip);
		}
		
		PonderTooltipHandler.addToTooltip(event.getToolTip(), stack);
	}

	@SubscribeEvent
	public static void onRenderTick(RenderTickEvent event) {
		if (!isGameActive())
			return;
		TurntableHandler.gameRenderTick();
	}

	protected static boolean isGameActive() {
		return !(Minecraft.getInstance().world == null || Minecraft.getInstance().player == null);
	}

	@SubscribeEvent
	public static void getFogDensity(EntityViewRenderEvent.FogDensity event) {
		ActiveRenderInfo info = event.getInfo();
		IFluidState fluidState = info.getFluidState();
		if (fluidState.isEmpty())
			return;
		Fluid fluid = fluidState.getFluid();

		if (fluid.isEquivalentTo(AllFluids.CHOCOLATE.get())) {
			event.setDensity(5f);
			event.setCanceled(true);
		}

		if (fluid.isEquivalentTo(AllFluids.HONEY.get())) {
			event.setDensity(1.5f);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void getFogColor(EntityViewRenderEvent.FogColors event) {
		ActiveRenderInfo info = event.getInfo();
		IFluidState fluidState = info.getFluidState();
		if (fluidState.isEmpty())
			return;
		Fluid fluid = fluidState.getFluid();

		if (fluid.isEquivalentTo(AllFluids.CHOCOLATE.get())) {
			event.setRed(98 / 256f);
			event.setGreen(32 / 256f);
			event.setBlue(32 / 256f);
		}

		if (fluid.isEquivalentTo(AllFluids.HONEY.get())) {
			event.setRed(234 / 256f);
			event.setGreen(174 / 256f);
			event.setBlue(47 / 256f);
		}
	}

	@SubscribeEvent
	public static void leftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
		ItemStack stack = event.getItemStack();
		if (stack.getItem() instanceof ZapperItem) {
			AllPackets.channel.sendToServer(new LeftClickPacket());
		}
	}

}
