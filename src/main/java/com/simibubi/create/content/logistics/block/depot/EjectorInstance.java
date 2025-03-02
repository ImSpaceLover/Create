package com.simibubi.create.content.logistics.block.depot;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.relays.encased.ShaftInstance;
import com.simibubi.create.foundation.render.backend.instancing.IDynamicInstance;
import com.simibubi.create.foundation.render.backend.instancing.InstanceKey;
import com.simibubi.create.foundation.render.backend.instancing.InstancedTileRenderer;
import com.simibubi.create.foundation.render.backend.instancing.impl.ModelData;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.MatrixStacker;

public class EjectorInstance extends ShaftInstance implements IDynamicInstance {

	protected final EjectorTileEntity tile;

	protected final InstanceKey<ModelData> plate;

	public EjectorInstance(InstancedTileRenderer<?> dispatcher, EjectorTileEntity tile) {
		super(dispatcher, tile);
		this.tile = tile;

		plate = getTransformMaterial().getModel(AllBlockPartials.EJECTOR_TOP, blockState).createInstance();

		pivotPlate();
		updateLight();
	}

	@Override
	public void beginFrame() {

		if (tile.lidProgress.settled()) return;

		pivotPlate();
	}

	private void pivotPlate() {
		float lidProgress = tile.getLidProgress(AnimationTickHolder.getPartialTicks());
		float angle = lidProgress * 70;

		MatrixStack ms = new MatrixStack();

		EjectorRenderer.applyLidAngle(tile, angle, MatrixStacker.of(ms).translate(getInstancePosition()));

		plate.getInstance().setTransform(ms);
	}

	@Override
	public void updateLight() {
		super.updateLight();
		relight(pos, plate.getInstance());
	}

	@Override
	public void remove() {
		super.remove();
		plate.delete();
	}
}
