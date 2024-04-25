package com.firestartermc.charcoal

import com.firestartermc.charcoal.common.Proxy
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.Instance
import net.minecraftforge.fml.common.SidedProxy

@Mod(
    modid = "charcoal",
    name = "Charcoal",
    version = "1.0-SNAPSHOT",
)
class Charcoal {
    companion object {
        @Instance
        lateinit var instance: Charcoal

        @SidedProxy(
            clientSide = "com.firestartermc.charcoal.client.ClientProxy",
            serverSide = "com.firestartermc.charcoal.common.CommonProxy",
        )
        lateinit var proxy: Proxy
    }
}
