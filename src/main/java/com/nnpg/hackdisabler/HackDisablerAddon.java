package com.nnpg.hackdisabler;

import com.nnpg.hackdisabler.modules.HackDisabler;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;

import java.io.File;

public class HackDisablerAddon extends MeteorAddon {
    @Override
    public void onInitialize() {
        Modules.get().add(new HackDisabler());
    }

    @Override
    public String getPackage() {
        return "com.nnpg.hackdisabler";
    }

    //@Override
    public File getFolder() {
        return new File(MeteorClient.FOLDER, "hack-disabler");
    }
}
