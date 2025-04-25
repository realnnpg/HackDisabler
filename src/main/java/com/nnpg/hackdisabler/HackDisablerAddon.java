package com.nnpg.hackdisabler;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;

import java.io.File;

public class HackDisablerAddon extends MeteorAddon {

    private static final String CONFIG_DIR = "hack-disabler";
    private File folder;

    @Override
    public void onInitialize() {
        folder = new File(MeteorClient.FOLDER, CONFIG_DIR);
        if (!folder.exists() && !folder.mkdirs()) {
            warning("Failed to create configuration directory: " + folder.getAbsolutePath());
        }
        Modules.get().add(new HackDisabler(folder));
    }

    @Override
    public String getPackage() {
        return "com.nnpg.hackdisabler";
    }

    @Override
    public File getFolder() {
        return folder;
    }

    @Override
    public void onShutdown() {
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }
}
