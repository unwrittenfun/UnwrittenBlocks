package unwrittenfun.minecraft.unwrittenblocks.common.blocks;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import unwrittenfun.minecraft.unwrittenblocks.common.ModInfo;
import unwrittenfun.minecraft.unwrittenblocks.common.UnwrittenBlocks;
import unwrittenfun.minecraft.unwrittenblocks.common.items.ItemRegister;
import unwrittenfun.minecraft.unwrittenblocks.common.multiblock.WallTeleporterNetwork;
import unwrittenfun.minecraft.unwrittenblocks.common.network.NetworkRegister;
import unwrittenfun.minecraft.unwrittenblocks.common.network.messages.TileEntityStackMessage;
import unwrittenfun.minecraft.unwrittenblocks.common.tileEntities.IWallTeleporterBlock;
import unwrittenfun.minecraft.unwrittenblocks.common.tileEntities.TEWallTeleporterBase;
import unwrittenfun.minecraft.unwrittenblocks.common.tileEntities.TEWallTeleporterWall;

/**
 * Author: James Birtles
 */
public class BlockWallTeleporterWall extends BlockContainer {
  public BlockWallTeleporterWall(String key) {
    super(Material.iron);
    setCreativeTab(UnwrittenBlocks.creativeTabUB);
    setBlockName(key);
    setBlockTextureName(ModInfo.RESOURCE_LOCATION + ":" + key);
    setHardness(2F);
  }

  @Override
  public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
    TEWallTeleporterWall teleporterWall = (TEWallTeleporterWall) world.getTileEntity(x, y, z);
    ItemStack mask = teleporterWall.mask;
    if (mask != null) {
      Block block = Block.getBlockFromItem(mask.getItem());
      return block.getIcon(side, mask.getItemDamage());
    }

    return super.getIcon(world, x, y, z, side);
  }

  @Override
  public boolean isOpaqueCube() {
    return false;
  }

  @Override
  public boolean renderAsNormalBlock() {
    return false;
  }

  @Override
  public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
    TEWallTeleporterWall teleporterWall = (TEWallTeleporterWall) world.getTileEntity(x, y, z);
    if (teleporterWall != null && teleporterWall.hasWTNetwork() && teleporterWall.getWTNetwork().hasDestination() && teleporterWall.getWTNetwork().fuel > 0) return AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
    return super.getCollisionBoundingBoxFromPool(world, x, y, z);
  }

  @Override
  public TileEntity createNewTileEntity(World world, int idk) {
    return new TEWallTeleporterWall();
  }

  @Override
  public boolean canPlaceBlockAt(World world, int x, int y, int z) {
    WallTeleporterNetwork networkTemp = null;
    for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
      TileEntity tileEntity = world.getTileEntity(x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ);
      if (tileEntity instanceof IWallTeleporterBlock) {
        IWallTeleporterBlock teleporter = (IWallTeleporterBlock) tileEntity;
        if (teleporter.hasWTNetwork()) {
          if (networkTemp == null) {
            networkTemp = teleporter.getWTNetwork();
          } else if (networkTemp != teleporter.getWTNetwork()) {
            return false;
          }
        }
      }
    }
    return super.canPlaceBlockAt(world, x, y, z);
  }

  @Override
  public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
    TEWallTeleporterWall teleporterWall = (TEWallTeleporterWall) world.getTileEntity(x, y, z);

    if (!player.isSneaking() && teleporterWall.hasWTNetwork() && (player.getHeldItem() == null || !(player.getHeldItem().getItem() instanceof ItemBlock))) {
      TileEntity base = teleporterWall.getWTNetwork().base;
      FMLNetworkHandler.openGui(player, UnwrittenBlocks.instance, 0, world, base.xCoord, base.yCoord, base.zCoord);
      return true;
    }

    if (player.getHeldItem().getItem() instanceof ItemBlock) {
      if (!(player.getHeldItem().isItemEqual(ItemRegister.wallStack) && teleporterWall.mask.isItemEqual(ItemRegister.wallStack))) {
        if (!world.isRemote) teleporterWall.setMask(player.getHeldItem().copy());
        return true;
      }
    }

    return false;
  }

  @Override
  public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
    TEWallTeleporterWall teleporterWall = (TEWallTeleporterWall) world.getTileEntity(x, y, z);
    teleporterWall.setIgnoreWT(true);
    if (teleporterWall.hasWTNetwork()) teleporterWall.getWTNetwork().refreshNetwork();
    super.breakBlock(world, x, y, z, block, meta);
  }

  @Override
  public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
    if (!world.isRemote) {
      if (entity instanceof EntityPlayerMP) {
        TEWallTeleporterWall teleporterWall = (TEWallTeleporterWall) world.getTileEntity(x, y, z);
        if (teleporterWall.hasWTNetwork()) teleporterWall.getWTNetwork().playerCollided(world, (EntityPlayerMP) entity);
      }
    }
    super.onEntityCollidedWithBlock(world, x, y, z, entity);
  }
}
