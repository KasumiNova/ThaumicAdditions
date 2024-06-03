package org.zeith.thaumicadditions.fluids;

import net.minecraftforge.fluids.Fluid;
import org.zeith.thaumicadditions.InfoTAR;

public class FluidCrystalWater
		extends Fluid
{
	public FluidCrystalWater()
	{
		super("crystal_water", InfoTAR.id("blocks/crystal_water_still"), InfoTAR.id("blocks/crystal_water_flow"));
		setUnlocalizedName(InfoTAR.MOD_ID + ":crystal_water");
	}
	
	@Override
	public int getColor()
	{
		return 0xFF3D9999;
	}
	
	@Override
	public String getUnlocalizedName()
	{
		return "fluid." + InfoTAR.MOD_ID + ":crystal_water";
	}
}