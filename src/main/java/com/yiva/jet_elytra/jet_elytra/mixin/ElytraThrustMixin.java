package com.yiva.jet_elytra.jet_elytra.mixin;

import com.yiva.jet_elytra.jet_elytra.capability.PlayerFuel;
import com.yiva.jet_elytra.jet_elytra.gameplay.JetThrustState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Jet thrust for glide flight.
 *
 * <p><b>Why this targets {@code LivingEntity.travel} and not {@code Player.travel}.</b> In 1.20.1 the
 * elytra flight maths live in {@code LivingEntity.travel}, not in the {@code Player} override:</p>
 *
 * <pre>
 * // LivingEntity.travel, the fall-flying branch
 * ...
 * vec3 = vec3.add((look.x / d1 * d3 - vec3.x) * 0.1, 0.0, (look.z / d1 * d3 - vec3.z) * 0.1);
 * this.setDeltaMovement(vec3.scale(0.99, 0.98, 0.99));   // &lt;- glide velocity recomputed here
 * this.move(MoverType.SELF, this.getDeltaMovement());    // &lt;- and consumed here, same tick
 * </pre>
 *
 * <p>{@code Player.travel} only wraps that call, so any velocity written there is thrown away: at
 * {@code TAIL} the value is overwritten again by {@code Player}'s
 * {@code setDeltaMovement(x, d5 * 0.6, z)} restore, and at {@code HEAD} it is recomputed by the glide
 * maths above before it is ever used. That is why earlier attempts burned fuel and spawned particles
 * while the player did not move at all.</p>
 *
 * <p>Injecting immediately before the elytra {@code move()} call is the one point where the value is
 * both final and actually used. It is the third of the three {@code move()} calls in the method
 * (water, lava, elytra), hence {@code ordinal = 2}; the normal-movement path reaches {@code move()}
 * through {@code moveRelative} in a helper method instead of calling it here.</p>
 *
 * <p><b>Both sides have to do this, which is the subtlety that made the jet feel dead.</b> The
 * server's copy of the velocity only decides where the server thinks the player is; the client
 * recomputes its own elytra velocity every tick in its own {@code travel}, and the client is
 * authoritative over its position, so the next position packet undoes anything the server applied.
 * The client therefore applies the same override, using the ramp the server pushes in
 * {@code S2C_ThrustSync} - see {@link JetThrustState} for why the client cannot read it off its own
 * {@code PlayerFuel}.</p>
 */
@Mixin(LivingEntity.class)
public abstract class ElytraThrustMixin {

    /**
     * Replaces the velocity the elytra glide is about to move with, while the jet is lifting.
     *
     * <p>Read-only with respect to the ramp: {@code JetThrust.tick} is the single writer of the
     * server's ramp. A ramp of zero means thrust has not been entered (or has ended), in which case
     * vanilla behaviour is left completely untouched.</p>
     */
    @Inject(method = "travel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", ordinal = 2))
    private void jetelytra$thrust(final Vec3 travelVector, final CallbackInfo ci) {
        // The mixin class does not extend LivingEntity, so go through Object like every mixin does.
        if (!((Object) this instanceof Player self)) {
            return;
        }

        // Hard guard: stale lift state must never move a player who is not fall-flying.
        if (!self.isFallFlying()) {
            return;
        }

        final double jetSpeed = jetelytra$jetSpeed(self);
        if (jetSpeed <= 0.0D) {
            return;
        }

        // Fly along the look vector at the jet's speed, floored at the speed the glide already had: the
        // ramp starts at one increment, and dropping a fast dive to that tiny value would read as the
        // jet braking the player instead of pushing them.
        final Vec3 look = self.getViewVector(1.0F);
        final double speed = Math.max(jetSpeed, self.getDeltaMovement().length());
        self.setDeltaMovement(look.x * speed, look.y * speed, look.z * speed);
    }

    /**
     * The ramp this side should fly at.
     *
     * <p>On the logical server that is the authoritative ramp on the player's own tank. On the client
     * it is the value the server last pushed: the client's own capability is a different instance
     * that nothing writes, so reading it there would always report zero and silently disable the
     * jet.</p>
     */
    private static double jetelytra$jetSpeed(final Player self) {
        if (self.level().isClientSide()) {
            return JetThrustState.serverSpeed();
        }
        return PlayerFuel.of(self).getBoostSpeed();
    }
}
