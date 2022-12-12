package com.github.alexmodguy.alexscaves.server.entity.living;

import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.block.PewenBranchBlock;
import com.github.alexmodguy.alexscaves.server.entity.ai.RelicheirusMeleeGoal;
import com.github.alexmodguy.alexscaves.server.entity.ai.RelicheirusNibblePewensGoal;
import com.github.alexmodguy.alexscaves.server.entity.ai.RelicheirusPushTreesGoal;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.animation.LegSolverQuadruped;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;

public class RelicheirusEntity extends Animal implements IAnimatedEntity {
    public LegSolverQuadruped legSolver = new LegSolverQuadruped(-0.15F, 0.6F, 0.5F, 0.75F, 1);
    private static final EntityDataAccessor<Integer> PECK_Y = SynchedEntityData.defineId(RelicheirusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HELD_MOB_ID = SynchedEntityData.defineId(RelicheirusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PUSHING_TREES_FOR = SynchedEntityData.defineId(RelicheirusEntity.class, EntityDataSerializers.INT);
    private Animation currentAnimation;
    private int animationTick;
    public static final Animation ANIMATION_SPEAK_1 = Animation.create(13);
    public static final Animation ANIMATION_SPEAK_2 = Animation.create(20);
    public static final Animation ANIMATION_EAT_TREE = Animation.create(40);
    public static final Animation ANIMATION_EAT_TRILOCARIS = Animation.create(50);
    public static final Animation ANIMATION_PUSH_TREE = Animation.create(60);
    public static final Animation ANIMATION_SCRATCH_1 = Animation.create(60);
    public static final Animation ANIMATION_SCRATCH_2 = Animation.create(40);
    public static final Animation ANIMATION_SHAKE = Animation.create(30);
    public static final Animation ANIMATION_MELEE_SLASH_1 = Animation.create(20);
    public static final Animation ANIMATION_MELEE_SLASH_2 = Animation.create(20);
    private float prevRaiseArmsAmount = 0;
    private float raiseArmsAmount = 0;

    public RelicheirusEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
        maxUpStep = 1.1F;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PECK_Y, 0);
        this.entityData.define(HELD_MOB_ID, -1);
        this.entityData.define(PUSHING_TREES_FOR, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.ATTACK_DAMAGE, 12.0D).add(Attributes.MOVEMENT_SPEED, 0.2D).add(Attributes.MAX_HEALTH, 120.0D);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RelicheirusMeleeGoal(this));
        this.goalSelector.addGoal(2, new RelicheirusPushTreesGoal(this, 25));
        this.goalSelector.addGoal(3, new RelicheirusNibblePewensGoal(this, 20));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0D, 45));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, RelicheirusEntity.class)));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, TrilocarisEntity.class, 100, true, false, null));
    }

    public static boolean checkPrehistoricSpawnRules(EntityType<? extends Animal> type, LevelAccessor levelAccessor, MobSpawnType mobType, BlockPos pos, RandomSource randomSource) {
        return levelAccessor.getBlockState(pos.below()).is(ACTagRegistry.DINOSAURS_SPAWNABLE_ON) && levelAccessor.getFluidState(pos).isEmpty() && levelAccessor.getFluidState(pos.below()).isEmpty();
    }

    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.COW_STEP, 0.7F, 0.85F);
    }

    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 0.99F * dimensions.height;
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult prev = super.mobInteract(player, hand);
        ItemStack itemstack = player.getItemInHand(hand);
        if(!prev.consumesAction() && itemstack.is(Items.SUSPICIOUS_STEW)){
            if(!itemstack.getCraftingRemainingItem().isEmpty()){
                this.spawnAtLocation(itemstack.getCraftingRemainingItem().copy());
            }
            this.usePlayerItem(player, hand, itemstack);
            this.setPushingTreesFor(1200);
            return InteractionResult.SUCCESS;
        }
        return prev;
    }

    public void push(double x, double y, double z) {
        if(this.getAnimation() != ANIMATION_EAT_TRILOCARIS){
            super.push(x, y, z);
        }
    }

    public void tick() {
        super.tick();
        if (this.getAnimation() != ANIMATION_EAT_TREE) {
            this.yBodyRot = Mth.approachDegrees(this.yBodyRotO, yBodyRot, getHeadRotSpeed());
        }
        this.prevRaiseArmsAmount = raiseArmsAmount;
        this.legSolver.update(this, this.yBodyRot, this.getScale());
        AnimationHandler.INSTANCE.updateAnimations(this);
        if (shouldRaiseArms() && raiseArmsAmount < 5F) {
            raiseArmsAmount++;
        }
        if (!shouldRaiseArms() && raiseArmsAmount > 0F) {
            raiseArmsAmount--;
        }
        if (!level.isClientSide) {
            if (isStillEnough() && random.nextInt(200) == 0 && this.getAnimation() == NO_ANIMATION) {
                Animation idle;
                float rand = random.nextFloat();
                if (rand < 0.15F) {
                    idle = ANIMATION_SCRATCH_1;
                } else if (rand < 0.3F) {
                    idle = ANIMATION_SCRATCH_2;
                } else {
                    idle = ANIMATION_SHAKE;
                }
                this.setAnimation(idle);
            }
            boolean held = false;
            LivingEntity target = this.getTarget();
            if (target != null && target.distanceTo(this) < 10 && target instanceof TrilocarisEntity) {
                if (this.getAnimation() == ANIMATION_EAT_TRILOCARIS) {
                    if(this.getAnimationTick() < 20){
                        held = true;
                        this.setHeldMobId(target.getId());
                    }else if(this.getAnimationTick() <= 50){
                        Vec3 trilocarisPos = getTrilocarisPos();
                        target.setPos(trilocarisPos);
                        if (this.getAnimationTick() >= 45 && target.isAlive()) {
                            target.hurt(DamageSource.mobAttack(this), 20);
                        }
                        held = true;
                        target.fallDistance = 0;
                    }
                }
            }
            if (!held && getHeldMobId() != -1) {
                this.setHeldMobId(-1);
            }
            if(this.getPushingTreesFor() > 0){
                this.setPushingTreesFor(this.getPushingTreesFor() - 1);
            }
        }
        if (this.getAnimation() == ANIMATION_SPEAK_1 && this.getAnimationTick() == 1 || this.getAnimation() == ANIMATION_SPEAK_2 && this.getAnimationTick() == 1) {
            actuallyPlayAmbientSound();
        }
    }

    private Vec3 getTrilocarisPos() {
        Vec3 triloUp = new Vec3(0, 0F, 1.5F);
        if (this.getAnimation() == ANIMATION_EAT_TRILOCARIS && getAnimationTick() >= 15F) {
            float anim1 = Math.min(getAnimationTick() - 15F, 15F) / 15F;
            float anim2 = Math.min(getAnimationTick(), 15F) / 15F;
            triloUp = triloUp.add(0, (this.getEyeHeight() + 1F) * anim1, anim2 * -1F + 1F);
        }
        Vec3 head = triloUp.xRot(-this.getXRot() * ((float) Math.PI / 180F)).yRot(-this.getYHeadRot() * ((float) Math.PI / 180F));
        return this.position().add(head);
    }

    private boolean isStillEnough() {
        return this.getDeltaMovement().horizontalDistance() < 0.05;
    }

    public boolean shouldRaiseArms() {
        return this.getAnimation() == ANIMATION_EAT_TREE || this.getAnimation() == ANIMATION_PUSH_TREE || this.getAnimation() == ANIMATION_SCRATCH_1 || this.getAnimation() == ANIMATION_SCRATCH_2 || this.getAnimation() == ANIMATION_MELEE_SLASH_1 || this.getAnimation() == ANIMATION_MELEE_SLASH_2;
    }

    public void setPeckY(int y) {
        this.entityData.set(PECK_Y, y);
    }


    public int getPeckY() {
        return this.entityData.get(PECK_Y);
    }

    public void setHeldMobId(int i) {
        this.entityData.set(HELD_MOB_ID, i);
    }

    public void travel(Vec3 vec3d) {
        if (this.getAnimation() == ANIMATION_EAT_TRILOCARIS) {
            vec3d = Vec3.ZERO;
        }
        super.travel(vec3d);
    }

    public int getHeldMobId() {
        return this.entityData.get(HELD_MOB_ID);
    }

    public Entity getHeldMob() {
        int id = getHeldMobId();
        return id == -1 ? null : level.getEntity(id);
    }

    public void setPushingTreesFor(int time) {
        this.entityData.set(PUSHING_TREES_FOR,time);
    }

    public int getPushingTreesFor() {
        return this.entityData.get(PUSHING_TREES_FOR);
    }

    public float getRaiseArmsAmount(float partialTick) {
        return (prevRaiseArmsAmount + (raiseArmsAmount - prevRaiseArmsAmount) * partialTick) * 0.2F;
    }

    public int getHeadRotSpeed() {
        return 5;
    }

    public void playAmbientSound() {
        if (this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(random.nextBoolean() ? ANIMATION_SPEAK_2 : ANIMATION_SPEAK_1);
        }
    }

    public void actuallyPlayAmbientSound() {
        SoundEvent soundevent = this.getAmbientSound();
        if (soundevent != null) {
            this.playSound(soundevent, this.getSoundVolume(), this.getVoicePitch());
        }
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_SPEAK_1, ANIMATION_SPEAK_2, ANIMATION_EAT_TREE, ANIMATION_EAT_TRILOCARIS, ANIMATION_PUSH_TREE, ANIMATION_SCRATCH_1, ANIMATION_SCRATCH_2, ANIMATION_SHAKE, ANIMATION_MELEE_SLASH_1, ANIMATION_MELEE_SLASH_2};
    }

    public void calculateEntityAnimation(LivingEntity living, boolean flying) {
        living.animationSpeedOld = living.animationSpeed;
        double d0 = living.getX() - living.xo;
        double d2 = living.getZ() - living.zo;
        float f = (float) Math.sqrt(d0 * d0 + d2 * d2) * 4.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        living.animationSpeed += (f - living.animationSpeed) * 0.4F;
        living.animationPosition += living.animationSpeed;
    }


    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_146743_, AgeableMob p_146744_) {
        return null;
    }

    public BlockPos getStandAtTreePos(BlockPos target) {
        Vec3 vec3 = Vec3.atCenterOf(target).subtract(this.position());
        float f = -((float) Mth.atan2(vec3.x, vec3.z)) * 180.0F / (float) Math.PI;
        BlockState state = level.getBlockState(target);
        Direction dir = Direction.fromYRot(f);
        if (state.is(ACBlockRegistry.PEWEN_BRANCH.get())) {
            dir = Direction.fromYRot(state.getValue(PewenBranchBlock.ROTATION) * 45);
        }
        if (level.getBlockState(target.below()).isAir()) {
            target = target.relative(dir);
        }
        return target.relative(dir.getOpposite(), 4).atY((int) this.getY());
    }

    public boolean lockTreePosition(BlockPos target) {
        Vec3 vec3 = Vec3.atCenterOf(target).subtract(this.position());
        float f = -((float) Mth.atan2(vec3.x, vec3.z)) * 180.0F / (float) Math.PI;
        BlockState state = level.getBlockState(target);
        Direction dir = Direction.fromYRot(f);
        if (state.is(ACBlockRegistry.PEWEN_BRANCH.get())) {
            dir = Direction.fromYRot(state.getValue(PewenBranchBlock.ROTATION) * 45);
        }
        float targetRot = Mth.approachDegrees(this.getYRot(), dir.toYRot(), 20);
        this.setYRot(targetRot);
        this.setYHeadRot(targetRot);
        this.yBodyRot = targetRot;
        if (level.getBlockState(target.below()).isAir()) {
            target = target.relative(dir);
        }
        Vec3 vec31 = Vec3.atCenterOf(target.relative(dir.getOpposite(), 2));
        Vec3 vec32 = vec31.subtract(this.position());
        if (vec32.length() > 1) {
            vec32 = vec32.normalize();
        }
        Vec3 delta = new Vec3(vec32.x * 0.1F, 0F, vec32.z * 0.1F);
        this.setDeltaMovement(this.getDeltaMovement().add(delta));
        return this.distanceToSqr(vec31.x, this.getY(), vec31.z) < 4.0D && Mth.degreesDifferenceAbs(this.getYRot(), dir.toYRot()) < 7;
    }
}
