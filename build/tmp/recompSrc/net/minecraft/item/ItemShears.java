package net.minecraft.item;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;

public class ItemShears extends Item
{
    private static final String __OBFID = "CL_00000062";

    public ItemShears()
    {
        this.setMaxStackSize(1);
        this.setMaxDurability(238);
        this.setCreativeTab(CreativeTabs.tabTools);
    }

    public boolean onBlockDestroyed(ItemStack stack, World worldIn, Block blockIn, int p_150894_4_, int p_150894_5_, int p_150894_6_, EntityLivingBase p_150894_7_)
    {
        if (blockIn.getMaterial() != Material.leaves && blockIn != Blocks.web && blockIn != Blocks.tallgrass && blockIn != Blocks.vine && blockIn != Blocks.tripwire && !(blockIn instanceof IShearable))
        {
            return super.onBlockDestroyed(stack, worldIn, blockIn, p_150894_4_, p_150894_5_, p_150894_6_, p_150894_7_);
        }
        else
        {
            return true;
        }
    }

    /**
     * Can this Item harvest passed block
     */
    public boolean canItemHarvestBlock(Block p_150897_1_)
    {
        return p_150897_1_ == Blocks.web || p_150897_1_ == Blocks.redstone_wire || p_150897_1_ == Blocks.tripwire;
    }

    public float getStrVsBlock(ItemStack p_150893_1_, Block p_150893_2_)
    {
        return p_150893_2_ != Blocks.web && p_150893_2_.getMaterial() != Material.leaves ? (p_150893_2_ == Blocks.wool ? 5.0F : super.getStrVsBlock(p_150893_1_, p_150893_2_)) : 15.0F;
    }

    /**
     * Returns true if the item can be used on the given entity, e.g. shears on sheep.
     *  
     * @param stack the item stack of the item being used
     * @param player the player who used the item
     * @param target the target we hit with the item in hand
     */
    @Override
    public boolean itemInteractionForEntity(ItemStack itemstack, EntityPlayer player, EntityLivingBase entity)
    {
        if (entity.worldObj.isRemote)
        {
            return false;
        }
        if (entity instanceof IShearable)
        {
            IShearable target = (IShearable)entity;
            if (target.isShearable(itemstack, entity.worldObj, (int)entity.posX, (int)entity.posY, (int)entity.posZ))
            {
                ArrayList<ItemStack> drops = target.onSheared(itemstack, entity.worldObj, (int)entity.posX, (int)entity.posY, (int)entity.posZ,
                        EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, itemstack));

                Random rand = new Random();
                for(ItemStack stack : drops)
                {
                    EntityItem ent = entity.entityDropItem(stack, 1.0F);
                    ent.motionY += rand.nextFloat() * 0.05F;
                    ent.motionX += (rand.nextFloat() - rand.nextFloat()) * 0.1F;
                    ent.motionZ += (rand.nextFloat() - rand.nextFloat()) * 0.1F;
                }
                itemstack.damageItem(1, entity);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack itemstack, int x, int y, int z, EntityPlayer player)
    {
        if (player.worldObj.isRemote)
        {
            return false;
        }
        Block block = player.worldObj.getBlock(x, y, z);
        if (block instanceof IShearable)
        {
            IShearable target = (IShearable)block;
            if (target.isShearable(itemstack, player.worldObj, x, y, z))
            {
                ArrayList<ItemStack> drops = target.onSheared(itemstack, player.worldObj, x, y, z,
                        EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, itemstack));
                Random rand = new Random();

                for(ItemStack stack : drops)
                {
                    float f = 0.7F;
                    double d  = (double)(rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
                    double d1 = (double)(rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
                    double d2 = (double)(rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
                    EntityItem entityitem = new EntityItem(player.worldObj, (double)x + d, (double)y + d1, (double)z + d2, stack);
                    entityitem.delayBeforeCanPickup = 10;
                    player.worldObj.spawnEntityInWorld(entityitem);
                }

                itemstack.damageItem(1, player);
                player.addStat(StatList.mineBlockStatArray[Block.getIdFromBlock(block)], 1);
            }
        }
        return false;
    }
}