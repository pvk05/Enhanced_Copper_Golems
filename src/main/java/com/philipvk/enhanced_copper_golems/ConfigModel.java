package com.philipvk.enhanced_copper_golems;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;

@Modmenu(modId = Enhanced_copper_golems.MOD_ID)
@Config(name = "my-config", wrapperName = "MyConfig")
public class ConfigModel {

    public boolean chest = true;
    public boolean trappedChest = true;
    public boolean barrel = true;
    public boolean shulkerbox = false;
}
