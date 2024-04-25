package com.firestartermc.charcoal.mixin;

import com.firestartermc.charcoal.UpdaterKt;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(Minecraft.class)
public class StartupInjection {

    @Shadow
    @Final
    public File mcDataDir;

    @Inject(method = "init()V", at = @At("HEAD"))
    void init(CallbackInfo callback) {
        // Run the auto-update in a blocking manner to prevent the game from starting before the update is complete
        UpdaterKt.autoUpdateBlocking(mcDataDir.toPath());
    }
}
