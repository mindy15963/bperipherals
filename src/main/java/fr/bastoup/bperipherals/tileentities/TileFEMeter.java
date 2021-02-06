package fr.bastoup.bperipherals.tileentities;

import dan200.computercraft.shared.Capabilities;
import fr.bastoup.bperipherals.capabilites.FEMeterEnergyIn;
import fr.bastoup.bperipherals.capabilites.FEMeterEnergyOut;
import fr.bastoup.bperipherals.init.ModTileTypes;
import fr.bastoup.bperipherals.peripherals.PeripheralFEMeter;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;

public class TileFEMeter extends TileOrientable implements ICapabilityProvider, ITickableTileEntity {

    private final FEMeterEnergyOut outEnergyStorage = new FEMeterEnergyOut(this);
    private final FEMeterEnergyIn inEnergyStrorage = new FEMeterEnergyIn(outEnergyStorage);
    private final PeripheralFEMeter peripheral = new PeripheralFEMeter(this);

    private final LazyOptional<FEMeterEnergyOut> holderOut = LazyOptional.of(() -> outEnergyStorage);
    private final LazyOptional<FEMeterEnergyIn> holderIn = LazyOptional.of(() -> inEnergyStrorage);
    private final LazyOptional<PeripheralFEMeter> holderPeripheral = LazyOptional.of(() -> peripheral);

    private int energyTransferedLastTick = 0;
    private int energyStoredLastTick = 0;

    public TileFEMeter() {
        super(ModTileTypes.FE_METER);
    }

    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("Energy", holderOut.orElse(null).serializeNBT());
        nbt.putInt("energyTransfered", energyTransferedLastTick);
        return nbt;
    }

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        CompoundNBT nbt = pkt.getNbtCompound();
        holderOut.orElse(null).deserializeNBT(nbt.getCompound("Energy"));
        energyTransferedLastTick = nbt.getInt("energyTransfered");
    }
	@Override
	public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        holderOut.orElse(null).deserializeNBT(nbt.getCompound("Energy"));
        energyTransferedLastTick = nbt.getInt("energyTransfered");
    }
	
	@Override
	public CompoundNBT write(CompoundNBT compound) {
        CompoundNBT nbt = super.write(compound);
        nbt.put("Energy", holderOut.orElse(null).serializeNBT());
        nbt.putInt("energyTransfered", energyTransferedLastTick);
        return nbt;
    }

	@SuppressWarnings("unchecked")
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction facing) {

        if (capability.equals(CapabilityEnergy.ENERGY)) {
            switch (getFaceOfFacing(facing)) {
                case LEFT:
                    return holderIn.cast();
                case RIGHT:
                    return holderOut.cast();
                default:
                    return null;
            }
        }

        if (capability.equals(Capabilities.CAPABILITY_PERIPHERAL)) {
            return holderPeripheral.cast();
        }

        return super.getCapability(capability, facing);
    }

	@Override
	public void tick() {
		if (!world.isRemote) {
            energyStoredLastTick = holderOut.orElse(null).getEnergyStored();
            holderOut.orElse(null).sendEnergy();
            energyTransferedLastTick = energyStoredLastTick - holderOut.orElse(null).getEnergyStored();

            if (holderOut.orElse(null).resetUpdated()) {
                world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
            }
        }
    }

    public int getEnergyTransferedLastTick() {
        return energyTransferedLastTick;
    }

    public int getEnergyStored() {
        return holderOut.orElse(null).getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return holderOut.orElse(null).getMaxEnergyStored();
    }

    public int getTransferRate() {
        return holderOut.orElse(null).getTransferRate();
    }

    public void setTransferRate(int rate) {
        holderOut.orElse(null).setTransferRate(rate);
    }

    public int getMaxTransferRate() {
        return holderOut.orElse(null).getMaxTransferRate();
    }
}
