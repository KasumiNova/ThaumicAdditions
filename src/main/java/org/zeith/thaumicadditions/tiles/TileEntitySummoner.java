package org.zeith.thaumicadditions.tiles;

import com.zeitheron.hammercore.tile.ITileDroppable;
import com.zeitheron.hammercore.tile.TileSyncableTickable;
import com.zeitheron.hammercore.utils.FrictionRotator;
import com.zeitheron.hammercore.utils.WorldUtil;
import com.zeitheron.hammercore.utils.base.Cast;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.zeith.thaumicadditions.api.AspectUtil;
import org.zeith.thaumicadditions.init.ItemsTAR;
import thaumcraft.api.ThaumcraftApi.EntityTags;
import thaumcraft.api.ThaumcraftApi.EntityTagsNBT;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.common.lib.events.EssentiaHandler;

import java.util.UUID;

public class TileEntitySummoner
		extends TileSyncableTickable
		implements IAspectContainer, ITileDroppable
{
	public final IntList entities = new IntArrayList();
	public final FrictionRotator rotator = new FrictionRotator();
	public ItemStack sample = ItemStack.EMPTY;
	public AspectList accumulated = new AspectList();
	public AspectList missing = new AspectList();
	public int cooldown = 0;
	private Entity cachedEntity;

	public static AspectList getAspectsForDNA(ItemStack sample)
	{
		if(!sample.isEmpty() && sample.getItem() == ItemsTAR.ENTITY_CELL && sample.hasTagCompound() && sample.getTagCompound().hasKey("Entity", NBT.TAG_COMPOUND))
		{
			NBTTagCompound nbt = sample.getTagCompound().getCompoundTag("Entity");
			EntityEntry entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(nbt.getString("Id")));
			NBTTagCompound tc = nbt.getCompoundTag("Data");

			AspectList tags = new AspectList();
			if(entry != null)
				for(EntityTags et : CommonInternals.scanEntities)
				{
					if(!et.entityName.equals(entry.getName()))
						continue;

					if(et.nbts == null || et.nbts.length == 0)
					{
						tags = et.aspects;
						continue;
					}

					for(EntityTagsNBT enbt : et.nbts)
					{
						if(!tc.hasKey(enbt.name) || !ThaumcraftApiHelper.getNBTDataFromId(tc, tc.getTagId(enbt.name), enbt.name).equals(enbt.value))
							continue;
					}

					tags.add(et.aspects);
				}

			return tags;
		} else
			return new AspectList();
	}

	public static FrictionRotator readFrictionRotatorFromNBT(NBTTagCompound nbt, FrictionRotator rotator)
	{
		rotator.currentSpeed = nbt.getFloat("CurrentSpeed");
		rotator.degree = nbt.getFloat("Degree");
		rotator.friction = nbt.getFloat("Friction");
		rotator.prevDegree = nbt.getFloat("PrevDegree");
		rotator.speed = nbt.getFloat("Speed");
		return rotator;
	}

	public static NBTTagCompound writeFrictionRotatorToNBT(FrictionRotator rotator, NBTTagCompound nbt)
	{
		nbt.setFloat("CurrentSpeed", rotator.currentSpeed);
		nbt.setFloat("Degree", rotator.degree);
		nbt.setFloat("Friction", rotator.friction);
		nbt.setFloat("PrevDegree", rotator.prevDegree);
		nbt.setFloat("Speed", rotator.speed);

		return nbt;
	}

	@SideOnly(Side.CLIENT)
	public Entity getCachedEntity()
	{
		boolean cs = canSpawn();
		if(cachedEntity == null && cs)
		{
			String id = sample.getTagCompound().getCompoundTag("Entity").getString("Id");
			cachedEntity = EntityList.createEntityByIDFromName(new ResourceLocation(id), world);
			if(this.cachedEntity instanceof EntityLiving)
				((EntityLiving) cachedEntity).onInitialSpawn(world.getDifficultyForLocation(new BlockPos(cachedEntity)), null);
		}
		if(!cs)
			cachedEntity = null;
		return cachedEntity;
	}

	@Override
	public void tick()
	{
		rotator.friction = .25F;
		rotator.update();

		entities.removeIf(e -> world.getEntityByID(e) == null || world.getEntityByID(e).isDead);

		if(cooldown > 0)
			--cooldown;

		if(accumulated == null)
			accumulated = new AspectList();
		AspectList requirements = null;

		rotator.speedup(.2F);

		if(canSpawn() && (requirements = getAspectsForDNA(sample)).visSize() > 0)
		{
			missing = AspectUtil.getMissing(accumulated, requirements);

			if(missing.visSize() == 0)
			{
				if(cooldown == 0 && !world.isRemote && atTickRate(20) && performSpawn())
				{
					cooldown += 80 + getRNG().nextInt(50);
					accumulated.remove(requirements);

					rotator.speedup(10F);
					sendChangesToNearby();
				}
			} else if(!world.isRemote && missing != null && missing.size() > 0 && atTickRate(3))
			{
				Aspect[] as = missing.getAspects();
				Aspect a = as[as.length - 1];

				if(EssentiaHandler.drainEssentia(this, a, null, 8, 1))
				{
					accumulated.add(a, 1);
					rotator.speedup(1F);
					sendChangesToNearby();
				}
			}
		} else
			missing = new AspectList();
	}

	@Override
	public void createDrop(EntityPlayer player, World world, BlockPos pos)
	{
		if(!sample.isEmpty())
			WorldUtil.spawnItemStack(world, pos, sample);
		AuraHelper.polluteAura(world, pos, accumulated.visSize(), true);
	}

	public boolean performSpawn()
	{
		int spawnCount = 1 + getRNG().nextInt(4);
		double spawnRange = 4;
		int maxNearbyEntities = 4;

		int spawned = 0;

		for(int i = 0; i < spawnCount && entities.size() < 10; ++i)
		{
			String id = sample.getTagCompound().getCompoundTag("Entity").getString("Id");

			double d0 = (double) pos.getX() + (world.rand.nextDouble() - world.rand.nextDouble()) * spawnRange + 0.5D;
			double d1 = pos.getY() + world.rand.nextInt(3);
			double d2 = (double) pos.getZ() + (world.rand.nextDouble() - world.rand.nextDouble()) * spawnRange + 0.5D;
			Entity entity = EntityList.createEntityByIDFromName(new ResourceLocation(id), world);

			if(entity == null) return false;

			entity.setPositionAndUpdate(d0, d1, d2);

			WorldServer ws = Cast.cast(world, WorldServer.class);
			for(int k = 0; k < 16 && ws != null && ws.getEntityFromUuid(entity.getUniqueID()) != null; ++k)
				entity.setUniqueId(UUID.randomUUID());

			EntityLiving entityliving = entity instanceof EntityLiving ? (EntityLiving) entity : null;
			entity.setLocationAndAngles(entity.posX, entity.posY, entity.posZ, world.rand.nextFloat() * 360.0F, 0.0F);

			if(entityliving != null)
				entityliving.spawnExplosionParticle();

			if(!world.isRemote)
			{
				AnvilChunkLoader.spawnEntity(entity, world);
				entities.add(entity.getEntityId());
				++spawned;
			}
		}

		if(spawned > 0)
			world.playEvent(2004, pos, 0);

		return spawned > 0;
	}

	public boolean canSpawn()
	{
		if(world.getRedstonePowerFromNeighbors(pos) > 0)
			return false;
		return !sample.isEmpty() && sample.getItem() == ItemsTAR.ENTITY_CELL && sample.hasTagCompound() && sample.getTagCompound().hasKey("Entity", NBT.TAG_COMPOUND);
	}

	@Override
	public void writeNBT(NBTTagCompound nbt)
	{
		nbt.setTag("Accum", AspectUtil.writeALToNBT(accumulated, new NBTTagCompound()));
		nbt.setTag("Sample", sample.serializeNBT());
		nbt.setInteger("Cooldown", cooldown);
		nbt.setTag("Rotator", writeFrictionRotatorToNBT(rotator, new NBTTagCompound()));
	}

	@Override
	public void readNBT(NBTTagCompound nbt)
	{
		readFrictionRotatorFromNBT(nbt.getCompoundTag("Rotator"), rotator);
		cooldown = nbt.getInteger("Cooldown");
		accumulated = new AspectList();
		if(nbt.hasKey("Accum"))
			accumulated.readFromNBT(nbt.getCompoundTag("Accum"));
		sample = new ItemStack(nbt.getCompoundTag("Sample"));
	}

	@Override
	public AspectList getAspects()
	{
		return missing != null && missing.size() > 0 ? missing : null;
	}

	@Override
	public void setAspects(AspectList var1)
	{
	}

	@Override
	public boolean doesContainerAccept(Aspect var1)
	{
		return false;
	}

	@Override
	public int addToContainer(Aspect a, int q)
	{
		if(missing != null)
		{
			int max = Math.min(missing.getAmount(a), q);
			if(accumulated != null)
			{
				accumulated.add(a, max);
				return max;
			}
		}
		return 0;
	}

	@Override
	public boolean takeFromContainer(Aspect var1, int var2)
	{
		return false;
	}

	@Override
	public boolean takeFromContainer(AspectList var1)
	{
		return false;
	}

	@Override
	public boolean doesContainerContainAmount(Aspect var1, int var2)
	{
		return false;
	}

	@Override
	public boolean doesContainerContain(AspectList var1)
	{
		return false;
	}

	@Override
	public int containerContains(Aspect var1)
	{
		return 0;
	}
}