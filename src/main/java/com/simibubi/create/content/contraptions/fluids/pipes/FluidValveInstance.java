package com.simibubi.create.content.contraptions.fluids.pipes;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.base.KineticTileEntityRenderer;
import com.simibubi.create.content.contraptions.relays.encased.ShaftInstance;
import com.simibubi.create.foundation.render.backend.instancing.IDynamicInstance;
import com.simibubi.create.foundation.render.backend.instancing.InstanceKey;
import com.simibubi.create.foundation.render.backend.instancing.InstancedTileRenderer;
import com.simibubi.create.foundation.render.backend.instancing.impl.ModelData;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.MatrixStacker;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;

public class FluidValveInstance extends ShaftInstance implements IDynamicInstance {

    protected InstanceKey<ModelData> pointer;

    protected final double xRot;
    protected final double yRot;
    protected final int pointerRotationOffset;

    public FluidValveInstance(InstancedTileRenderer<?> dispatcher, KineticTileEntity tile) {
        super(dispatcher, tile);

        Direction facing = blockState.get(FluidValveBlock.FACING);

        yRot = AngleHelper.horizontalAngle(facing);
        xRot = facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90;

        Direction.Axis pipeAxis = FluidValveBlock.getPipeAxis(blockState);
        Direction.Axis shaftAxis = KineticTileEntityRenderer.getRotationAxisOf(tile);

        boolean twist = pipeAxis.isHorizontal() && shaftAxis == Direction.Axis.Z || pipeAxis.isVertical();
        pointerRotationOffset = twist ? 90 : 0;

        pointer = renderer.getTransformMaterial().getModel(AllBlockPartials.FLUID_VALVE_POINTER, blockState).createInstance();

        transformPointer((FluidValveTileEntity) tile);
    }

    @Override
    public void beginFrame() {

        FluidValveTileEntity valve = (FluidValveTileEntity) tile;

        if (valve.pointer.settled()) return;

        transformPointer(valve);
    }

    private void transformPointer(FluidValveTileEntity valve) {
        float pointerRotation = MathHelper.lerp(valve.pointer.getValue(AnimationTickHolder.getPartialTicks()), 0, -90);

        MatrixStack ms = new MatrixStack();
        MatrixStacker.of(ms)
                     .translate(getInstancePosition())
                     .centre()
                     .rotateY(yRot)
                     .rotateX(xRot)
                     .rotateY(pointerRotationOffset + pointerRotation)
                     .unCentre();

        pointer.getInstance().setTransform(ms);
    }

    @Override
    public void updateLight() {
        super.updateLight();
        relight(pos, pointer.getInstance());
    }

    @Override
    public void remove() {
        super.remove();
        pointer.delete();
    }
}
