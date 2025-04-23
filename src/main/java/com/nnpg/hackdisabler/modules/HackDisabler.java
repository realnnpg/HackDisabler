package com.nnpg.hackdisabler.modules;
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
import net.fabricmc.loader.impl.util.log.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class HackDisabler extends Module {
    private final Set<Module> disabledModules = new HashSet<>();
    private final Set<Module> queuedModules = new HashSet<>();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Set<Module> trackedModules = new HashSet<>();
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
    @Override
    public void onActivate() {
        MeteorClient.LOG.warn("nnpg-Entered onActivate");
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
        MeteorClient.LOG.warn("nnpg-Entered onDeactivate");
        for (Module module : disabledModules) {
            if (!module.isActive()) {
                module.toggle();
            }
        }
        info("Re-enabled " + disabledModules.size() + " modules.");
        disabledModules.clear();
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
        MeteorClient.LOG.warn("nnpg-Entered onGameLefet");

        List<Module> ignoredModules = ignoredModulesSetting.get();
        for (Module module : Modules.get().getAll()) {
            if (module != this && module.isActive() && !ignoredModules.contains(module)) {
                module.toggle();
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
