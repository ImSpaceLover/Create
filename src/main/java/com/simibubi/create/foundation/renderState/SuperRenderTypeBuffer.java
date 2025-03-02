package com.simibubi.create.foundation.renderState;

import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.Atlases;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

public class SuperRenderTypeBuffer implements IRenderTypeBuffer {

	public static BlockPos vertexSortingOrigin = BlockPos.ZERO;
	static SuperRenderTypeBuffer instance;

	public static SuperRenderTypeBuffer getInstance() {
		if (instance == null)
			instance = new SuperRenderTypeBuffer();
		return instance;
	}

	SuperRenderTypeBufferPhase earlyBuffer;
	SuperRenderTypeBufferPhase defaultBuffer;
	SuperRenderTypeBufferPhase lateBuffer;

	public SuperRenderTypeBuffer() {
		earlyBuffer = new SuperRenderTypeBufferPhase();
		defaultBuffer = new SuperRenderTypeBufferPhase();
		lateBuffer = new SuperRenderTypeBufferPhase();
	}

	public IVertexBuilder getEarlyBuffer(RenderType type) {
		return earlyBuffer.getBuffer(type);
	}

	@Override
	public IVertexBuilder getBuffer(RenderType type) {
		return defaultBuffer.getBuffer(type);
	}

	public IVertexBuilder getLateBuffer(RenderType type) {
		return lateBuffer.getBuffer(type);
	}

	public void draw() {
		RenderSystem.disableCull();
		earlyBuffer.draw();
		defaultBuffer.draw();
		lateBuffer.draw();
	}

	public void draw(RenderType type) {
		RenderSystem.disableCull();
		earlyBuffer.draw(type);
		defaultBuffer.draw(type);
		lateBuffer.draw(type);
	}

	private static class SuperRenderTypeBufferPhase extends IRenderTypeBuffer.Impl {

		// Visible clones from net.minecraft.client.renderer.RenderTypeBuffers
		static final RegionRenderCacheBuilder blockBuilders = new RegionRenderCacheBuilder();

		static final SortedMap<RenderType, BufferBuilder> createEntityBuilders() {
			return Util.make(new Object2ObjectLinkedOpenHashMap<>(), (map) -> {
				map.put(Atlases.getEntitySolid(), blockBuilders.get(RenderType.getSolid()));
				assign(map, RenderTypes.getOutlineSolid());
				map.put(Atlases.getEntityCutout(), blockBuilders.get(RenderType.getCutout()));
				map.put(Atlases.getBannerPatterns(), blockBuilders.get(RenderType.getCutoutMipped()));
				map.put(Atlases.getEntityTranslucent(), blockBuilders.get(RenderType.getTranslucent()));
				assign(map, Atlases.getShieldPatterns());
				assign(map, Atlases.getBeds());
				assign(map, Atlases.getShulkerBoxes());
				assign(map, Atlases.getSign());
				assign(map, Atlases.getChest());
				assign(map, RenderType.getTranslucentNoCrumbling());
				assign(map, RenderType.getGlint());
				assign(map, RenderType.getEntityGlint());
				assign(map, RenderType.getWaterMask());
				ModelBakery.BLOCK_DESTRUCTION_RENDER_LAYERS.forEach((p_228488_1_) -> {
					assign(map, p_228488_1_);
				});
			});
		}

		private static void assign(Object2ObjectLinkedOpenHashMap<RenderType, BufferBuilder> map, RenderType type) {
			map.put(type, new BufferBuilder(type.getExpectedBufferSize()));
		}

		protected SuperRenderTypeBufferPhase() {
			super(new BufferBuilder(256), createEntityBuilders());
		}

		public void draw(RenderType p_228462_1_) {
			BlockPos v = vertexSortingOrigin;
			BufferBuilder bufferbuilder = layerBuffers.getOrDefault(p_228462_1_, this.fallbackBuffer);
			boolean flag = Objects.equals(this.currentLayer, p_228462_1_.asOptional());
			if (flag || bufferbuilder != this.fallbackBuffer) {
				if (this.activeConsumers.remove(bufferbuilder)) {
					p_228462_1_.draw(bufferbuilder, v.getX(), v.getY(), v.getZ());
					if (flag) {
						this.currentLayer = Optional.empty();
					}
				}
			}
		}

	}

}
