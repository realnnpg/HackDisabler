package com.nnpg.hackdisabler.modules;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HackDisabler extends Module {
    private final Set<Module> disabledModules = new HashSet<>();
    private final Set<Module> queuedModules = new HashSet<>();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Set<Module> trackedModules = new HashSet<>();
    private final Set<Module> disabledModulesOnDisconnect = new HashSet<>();


    public enum ModuleActivationBehavior {
        Allow,
        Queue,
        Block
    }

    private final Setting<List<Module>> ignoredModulesSetting = sgGeneral.add(new ModuleListSetting.Builder()
        .name("ignored-modules")
        .description("Modules that won't be disabled when HackDisabler is activated.")
        .defaultValue(new ArrayList<>())
        .build()
    );

    private final Setting<ModuleActivationBehavior> activationBehaviorSetting = sgGeneral.add(new EnumSetting.Builder<ModuleActivationBehavior>()
        .name("activation-behavior")
        .description("What happens when a module is enabled while HackDisabler is active.")
        .defaultValue(ModuleActivationBehavior.Block)
        .build()
    );

    public HackDisabler() {
        super(Categories.Misc, "Hack Disabler", "Disables all active Meteor hacks and re-enables them when turned off.");
        MeteorClient.EVENT_BUS.subscribe(this);
    }


    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        for (Module module : disabledModulesOnDisconnect) {
            if (module != this) {
                trackedModules.add(module);
                disabledModules.add(module);
            }
        }
        disabledModulesOnDisconnect.clear();
    }

    @Override
    public void onActivate() {
        disabledModules.clear();
        queuedModules.clear();
        trackedModules.clear();
        List<Module> ignoredModules = ignoredModulesSetting.get();
        for (Module module : Modules.get().getAll()) {
            if (module != this) {
                trackedModules.add(module);
                if (module.isActive() && !ignoredModules.contains(module)) {
                    module.toggle();
                    disabledModules.add(module);
                }
            }
        }
        info("Disabled " + disabledModules.size() + " modules.");
    }


    @Override
    public void onDeactivate() {
        for (Module module : disabledModules) {
            if (!module.isActive()) {
                module.toggle();
            }
        }
        info("Re-enabled " + disabledModules.size() + " modules.");
        for (Module module : queuedModules) {
            if (!module.isActive()) {
                module.toggle();
            }
        }
        if (!queuedModules.isEmpty()) {
            info("Enabled " + queuedModules.size() + " queued modules.");
        }
        queuedModules.clear();
        trackedModules.clear();
    }


    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        List<Module> ignoredModules = ignoredModulesSetting.get();
        for (Module module : Modules.get().getAll()) {
            if (module != this && module.isActive() && !ignoredModules.contains(module)) {
                module.toggle();
                disabledModulesOnDisconnect.add(module);

            }
        }
        info("Disabled all modules (server disconnect).");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(PlayerMoveEvent event) {
        if (!isActive()) return;
        ModuleActivationBehavior behavior = activationBehaviorSetting.get();
        List<Module> ignoredModules = ignoredModulesSetting.get();
        for (Module module : Modules.get().getAll()) {
            if (module == this || ignoredModules.contains(module)) continue;
            if (module.isActive() && !disabledModules.contains(module) && trackedModules.contains(module)) {
                switch (behavior) {
                    case Allow:
                        break;
                    case Queue:
                        module.toggle();
                        queuedModules.add(module);
                        info("Queued " + module.name + " for activation");
                        break;
                    case Block:
                        module.toggle();
                        info("Blocked activation of " + module.name);
                        break;
                }
            }
        }
    }
}

