package com.igteam.immersivegeology.client.menu;

import blusunrize.immersiveengineering.client.gui.info.InfoArea;
import blusunrize.immersiveengineering.client.utils.GuiHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;

import java.util.List;

/**
 * Fluid info area retaining IG's region-based GUI overlays. IE 1.21's
 * FluidInfoArea accepts GUI sprites only, while the existing IG screens store
 * their tank frames as regions of the full screen texture.
 */
public class IGFluidInfoArea extends InfoArea
{
	private final IFluidTank tank;
	private final Rect2i area;
	private final int overlayUMin;
	private final int overlayVMin;
	private final int overlayWidth;
	private final int overlayHeight;
	private final ResourceLocation overlayTexture;

	public IGFluidInfoArea(
			IFluidTank tank, Rect2i area,
			int overlayUMin, int overlayVMin, int overlayWidth, int overlayHeight,
			ResourceLocation overlayTexture
	)
	{
		super(area);
		this.tank = tank;
		this.area = area;
		this.overlayUMin = overlayUMin;
		this.overlayVMin = overlayVMin;
		this.overlayWidth = overlayWidth;
		this.overlayHeight = overlayHeight;
		this.overlayTexture = overlayTexture;
	}

	@Override
	public void fillTooltipOverArea(int mouseX, int mouseY, List<Component> tooltip)
	{
		blusunrize.immersiveengineering.client.gui.info.FluidInfoArea.fillTooltip(
				tank.getFluid(), tank.getCapacity(), tooltip::add
		);
	}

	@Override
	public void draw(GuiGraphics graphics)
	{
		FluidStack fluid = tank.getFluid();
		graphics.pose().pushPose();
		if(!fluid.isEmpty()&&tank.getCapacity() > 0)
		{
			int fluidHeight = (int)(area.getHeight()*(fluid.getAmount()/(float)tank.getCapacity()));
			GuiHelper.drawRepeatedFluidSpriteGui(
					graphics.bufferSource(), graphics.pose(), fluid,
					area.getX(), area.getY()+area.getHeight()-fluidHeight,
					area.getWidth(), fluidHeight
			);
		}
		if(overlayWidth > 0&&overlayHeight > 0)
		{
			int xOff = (area.getWidth()-overlayWidth)/2;
			int yOff = (area.getHeight()-overlayHeight)/2;
			graphics.blit(
					overlayTexture, area.getX()+xOff, area.getY()+yOff,
					overlayUMin, overlayVMin, overlayWidth, overlayHeight
			);
		}
		graphics.pose().popPose();
	}
}
