package com.simibubi.create.foundation.ponder;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.IScreenRenderable;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.ponder.content.PonderTagScreen;
import com.simibubi.create.foundation.ponder.ui.PonderButton;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class NavigatableSimiScreen extends AbstractSimiScreen {

	public static final String THINK_BACK = PonderLocalization.LANG_PREFIX + "think_back";

	protected int depthPointX, depthPointY;
	public final LerpedFloat transition = LerpedFloat.linear()
		.startWithValue(0)
		.chase(0, .1f, LerpedFloat.Chaser.LINEAR);
	protected final LerpedFloat arrowAnimation = LerpedFloat.linear()
			.startWithValue(0)
			.chase(0, 0.075f, LerpedFloat.Chaser.LINEAR);
	protected PonderButton backTrack;

	public NavigatableSimiScreen() {
		MainWindow window = Minecraft.getInstance()
			.getWindow();
		depthPointX = window.getScaledWidth() / 2;
		depthPointY = window.getScaledHeight() / 2;
	}

	@Override
	public void onClose() {
		ScreenOpener.clearStack();
		super.onClose();
	}

	@Override
	public void tick() {
		super.tick();
		transition.tickChaser();
		arrowAnimation.tickChaser();
	}

	@Override
	protected void init() {
		super.init();
		List<Screen> screenHistory = ScreenOpener.getScreenHistory();
		if (screenHistory.isEmpty())
			return;
		if (!(screenHistory.get(0) instanceof NavigatableSimiScreen))
			return;

		Screen screen = screenHistory.get(0);
		IScreenRenderable icon = null;
		ItemStack altIcon = null;

		if (screen instanceof PonderUI)
			altIcon = ((PonderUI) screen).stack;
		if (screen instanceof PonderTagScreen)
			icon = ((PonderTagScreen) screen).getTag();

		widgets.add(backTrack = new PonderButton(31, height - 31 - PonderButton.SIZE, () -> {
			ScreenOpener.openPreviousScreen(this, Optional.empty());
		}).fade(0, -1));
		backTrack.fade(1);

		if (icon != null)
			backTrack.showing(icon);
		if (altIcon != null)
			backTrack.showing(altIcon);
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		super.render(mouseX, mouseY, partialTicks);
//		renderZeloBreadcrumbs(mouseX, mouseY, partialTicks);
		if (backTrack == null)
			return;

		RenderSystem.pushMatrix();
		RenderSystem.translated(0, 0, 500);
		if (backTrack.isHovered()) {
			drawString(font, Lang.translate(THINK_BACK), 15, height - 16, 0xffa3a3a3);
			if (MathHelper.epsilonEquals(arrowAnimation.getValue(), arrowAnimation.getChaseTarget())) {
				arrowAnimation.setValue(1);
				arrowAnimation.setValue(1);//called twice to also set the previous value to 1
			}
		}
		RenderSystem.popMatrix();
	}

	@Override
	protected void renderWindowBackground(int mouseX, int mouseY, float partialTicks) {
		if (transition.getChaseTarget() == 0) {
			renderBackground();
			return;
		}

		renderBackground();

		Screen lastScreen = ScreenOpener.getPreviouslyRenderedScreen();
		float transitionValue = transition.getValue(partialTicks);
		double scale = 1 + 0.5 * transitionValue;

		// draw last screen into buffer
		if (lastScreen != null && lastScreen != this) {
			RenderSystem.pushMatrix();// 1
			UIRenderHelper.framebuffer.framebufferClear(Minecraft.IS_RUNNING_ON_MAC);
			UIRenderHelper.prepFramebufferSize();
			RenderSystem.pushMatrix();// 2
			RenderSystem.translated(0, 0, -1000);
			UIRenderHelper.framebuffer.bindFramebuffer(true);
			lastScreen.render(mouseX, mouseY, 10);
			RenderSystem.popMatrix();// 2

			// use the buffer texture
			Minecraft.getInstance()
				.getFramebuffer()
				.bindFramebuffer(true);

			MainWindow window = Minecraft.getInstance()
				.getWindow();
			int dpx = window.getScaledWidth() / 2;
			int dpy = window.getScaledHeight() / 2;
			if (lastScreen instanceof AbstractSimiScreen) {
				dpx = ((NavigatableSimiScreen) lastScreen).depthPointX;
				dpy = ((NavigatableSimiScreen) lastScreen).depthPointY;
			}

			// transitionV is 1/-1 when the older screen is hidden
			// transitionV is 0 when the older screen is still fully visible
			RenderSystem.translated(dpx, dpy, 0);
			RenderSystem.scaled(scale, scale, 1);
			RenderSystem.translated(-dpx, -dpy, 0);
			UIRenderHelper.drawFramebuffer(1f - Math.abs(transitionValue));
			RenderSystem.popMatrix();// 1
		}

		// modify current screen as well
		scale = transitionValue > 0 ? 1 - 0.5 * (1 - transitionValue) : 1 + .5 * (1 + transitionValue);
		RenderSystem.translated(depthPointX, depthPointY, 0);
		RenderSystem.scaled(scale, scale, 1);
		RenderSystem.translated(-depthPointX, -depthPointY, 0);

		if (backTrack != null) {
			int x = (int) MathHelper.lerp(arrowAnimation.getValue(partialTicks), -9, 21);
			int maxX = backTrack.x + backTrack.getWidth();

			if (x + 30 < backTrack.x)
				UIRenderHelper.breadcrumbArrow(x + 30, height - 51, maxX - (x + 30), 20, 5, 0x40aa9999, 0x10aa9999);

			UIRenderHelper.breadcrumbArrow(x, height - 51, 30, 20, 5, 0x40aa9999, 0x10aa9999);
			UIRenderHelper.breadcrumbArrow(x - 30, height - 51, 30, 20, 5, 0x40aa9999, 0x10aa9999);
		}
	}

	@Override
	public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
		if (code == GLFW.GLFW_KEY_BACKSPACE) {
			ScreenOpener.openPreviousScreen(this, Optional.empty());
			return true;
		}
		return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
	}

	public void centerScalingOn(int x, int y) {
		depthPointX = x;
		depthPointY = y;
	}

	public void centerScalingOnMouse() {
		MainWindow w = minecraft.getWindow();
		double mouseX = minecraft.mouseHelper.getMouseX() * w.getScaledWidth() / w.getWidth();
		double mouseY = minecraft.mouseHelper.getMouseY() * w.getScaledHeight() / w.getHeight();
		centerScalingOn((int) mouseX, (int) mouseY);
	}

	public boolean isEquivalentTo(NavigatableSimiScreen other) {
		return false;
	}

	public void shareContextWith(NavigatableSimiScreen other) {}

	protected void renderZeloBreadcrumbs(int mouseX, int mouseY, float partialTicks) {
		List<Screen> history = ScreenOpener.getScreenHistory();
		if (history.isEmpty())
			return;

		history.add(0, Minecraft.getInstance().currentScreen);
		int spacing = 20;

		List<String> names = history.stream()
			.map(NavigatableSimiScreen::screenTitle)
			.collect(Collectors.toList());

		int bWidth = names.stream()
			.mapToInt(s -> font.getStringWidth(s) + spacing)
			.sum();

		MutableInt x = new MutableInt(width - bWidth);
		MutableInt y = new MutableInt(height - 18);
		MutableBoolean first = new MutableBoolean(true);

		if (x.getValue() < 25)
			x.setValue(25);

		RenderSystem.pushMatrix();
		RenderSystem.translated(0, 0, 600);
		names.forEach(s -> {
			int sWidth = font.getStringWidth(s);
			UIRenderHelper.breadcrumbArrow(x.getValue(), y.getValue(), sWidth + spacing, 14, spacing / 2, 0xdd101010,
				0x44101010);
			drawString(font, s, x.getValue() + 5, y.getValue() + 3, first.getValue() ? 0xffeeffee : 0xffddeeff);
			first.setFalse();

			x.add(sWidth + spacing);
		});
		RenderSystem.popMatrix();
	}

	private static String screenTitle(Screen screen) {
		if (screen instanceof NavigatableSimiScreen)
			return ((NavigatableSimiScreen) screen).getBreadcrumbTitle();
		return "<";
	}

	protected String getBreadcrumbTitle() {
		return this.getClass()
			.getSimpleName();
	}
}
