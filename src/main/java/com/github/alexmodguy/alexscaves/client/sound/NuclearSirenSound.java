package com.github.alexmodguy.alexscaves.client.sound;

import com.github.alexmodguy.alexscaves.client.ClientProxy;
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.block.blockentity.NuclearSirenBlockEntity;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class NuclearSirenSound extends AbstractTickableSoundInstance implements UnlimitedPitch {
    private final NuclearSirenBlockEntity siren;

    public NuclearSirenSound(NuclearSirenBlockEntity siren) {
        super(ACSoundRegistry.NUCLEAR_SIREN.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.looping = true;
        this.delay = 0;
        this.siren = siren;
    }

    public boolean canPlaySound() {
        if (ClientProxy.closestSirenSound == this) {
            if (Minecraft.getInstance().level == null) {
                ClientProxy.closestSirenSound = null;
                return false;
            }
            BlockState state = Minecraft.getInstance().level.getBlockState(siren.getBlockPos());
            BlockEntity blockEntity = Minecraft.getInstance().level.getBlockEntity(siren.getBlockPos());
            if (!siren.isRemoved() && blockEntity instanceof NuclearSirenBlockEntity && state.is(ACBlockRegistry.NUCLEAR_SIREN.get()) && (this.siren.isActivated(state) || this.volume > 0)) {
                return true;
            } else {
                ClientProxy.closestSirenSound = null;
                return false;
            }
        } else {
            return ClientProxy.closestSirenSound == null || ClientProxy.closestSirenSound.isStopped();
        }
    }

    public boolean canStartSilent() {
        return true;
    }

    public void tick() {
        this.pitch = 1.0F;
        Vec3 sirenPos = this.siren.getBlockPos().getCenter();
        this.x = sirenPos.x;
        this.y = sirenPos.y;
        this.z = sirenPos.z;
        this.volume = this.siren.getVolume(1.0F) * (1F - ClientProxy.masterVolumeNukeModifier);
        BlockState state = Minecraft.getInstance().level.getBlockState(siren.getBlockPos());
        BlockEntity blockEntity = Minecraft.getInstance().level.getBlockEntity(siren.getBlockPos());
        if(this.siren.isRemoved() || !(blockEntity instanceof NuclearSirenBlockEntity) || !this.siren.isActivated(state)){
            this.stop();
            ClientProxy.closestSirenSound = null;
        }
    }
}
