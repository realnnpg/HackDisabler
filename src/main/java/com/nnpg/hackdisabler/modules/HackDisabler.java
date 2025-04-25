package com.nnpg.hackdisabler;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.meteor.ModuleToggleEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.category.Category;
import meteordevelopment.meteorclient.utils.event.EventHandler;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class HackDisabler extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBehavior = settings.createGroup("Behavior");
    private final SettingGroup sgIgnored = settings.createGroup("Ignored Modules");

    private final Setting<List<Module>> ignoredModules = sgIgnored.add(
        new ModuleListSetting.Builder()
            .name("ignored-modules")
            .description("Modules to be ignored while disabling.")
            .defaultValue(List.of())
            .build()
    );

    private final Setting<ActivationBehavior> activationBehavior = sgBehavior.add(
        new EnumSetting.Builder<ActivationBehavior>()
            .name("activation-behavior")
            .description("Behavior when trying to activate tracked modules.")
            .defaultValue(ActivationBehavior.BLOCK)
            .build()
    );

    private final Set<Module> disabledModules = new LinkedHashSet<>();
    private final Set<Module> queuedModules = new LinkedHashSet<>();
    private final Set<Module> trackedModules = new LinkedHashSet<>();

    private final File configDir;

    public enum ActivationBehavior {
        ALLOW, QUEUE, BLOCK
    }

    public HackDisabler(File configDir) {
        super(new Category("Misc"), "hack-disabler", "Temporarily disables certain modules and blocks reactivation attempts.");
        this.configDir = configDir;
    }

    @Override
    public void onActivate() {
        disabledModules.clear();
        queuedModules.clear();
        trackedModules.clear();

        for (Module module : Modules.get().getAll()) {
            if (module == this || ignoredModules.get().contains(module)) continue;
            if (module.isActive()) {
                safelyToggle(module);
                disabledModules.add(module);
            }
            trackedModules.add(module);
        }

        info("Disabled " + disabledModules.size() + " module(s).");
    }

    @Override
    public void onDeactivate() {
        for (Module module : disabledModules) enableIfInactive(module);
        if (!disabledModules.isEmpty()) info("Re-enabled " + disabledModules.size() + " module(s).");

        for (Module module : queuedModules) enableIfInactive(module);
        if (!queuedModules.isEmpty()) info("Activated " + queuedModules.size() + " queued module(s).");

        disabledModules.clear();
        queuedModules.clear();
        trackedModules.clear();
    }

    @EventHandler
    private void onModuleToggle(ModuleToggleEvent event) {
        Module module = event.module;

        if (!trackedModules.contains(module)) return;

        if (event.enabled) {
            switch (activationBehavior.get()) {
                case ALLOW -> {}
                case QUEUE -> {
                    queuedModules.add(module);
                    event.setCanceled(true);
                }
                case BLOCK -> event.setCanceled(true);
            }
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        disabledModules.clear();
        queuedModules.clear();
        trackedModules.clear();
    }

    private void safelyToggle(Module module) {
        try {
            module.toggle();
        } catch (Exception e) {
            error("Error while toggling module: " + module.name, e);
        }
    }

    private void enableIfInactive(Module module) {
        if (!module.isActive()) safelyToggle(module);
    }

    @Override
    public void onLoad() {
        meteordevelopment.meteorclient.MeteorClient.EVENT_BUS.subscribe(this);
    }

    @Override
    public void onUnload() {
        meteordevelopment.meteorclient.MeteorClient.EVENT_BUS.unsubscribe(this);
    }
              }
