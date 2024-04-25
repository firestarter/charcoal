package com.firestartermc.charcoal.mixin;

import com.firestartermc.charcoal.GitHubRelease;
import com.firestartermc.charcoal.HttpKt;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.swing.*;

@Mixin(Minecraft.class)
public class StartupInjection {

    @Inject(method = "init()V", at = @At("HEAD"))
    void init(CallbackInfo callback) {
        GitHubRelease latestRelease = HttpKt.fetchLatestRelease();
        if (latestRelease == null) return;

        int response = JOptionPane.showConfirmDialog(null, String.format("Bonfire version %s has been released! Would you like to automatically update your game now to this version?\n\nThis is required to keep playing on our official server!", latestRelease.getVersion()));
        if (response == 0) {
            // todo; do the update lol
        }
    }
}