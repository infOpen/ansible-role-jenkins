#!/usr/bin/env groovy

import jenkins.model.*
import groovy.json.*


/* FUNCTIONS */

// Check arguments
def check_args(plugin_name) {
    if (plugin_name.getClass() != String) {
        throw new Exception(
            "Bad argument type : ${plugin_name.getClass().toString()} ")
    }
    else if (plugin_name == '') {
        throw new Exception("Plugin name cannot be an empty string !")
    }
}

// Enable plugin and its dependencies
def enable_plugin(jenkins_pm, plugin) {

    def plugins_enabled = []

    if (plugin == null) {
        throw new Exception(
            "Plugin to enable cannot be null")
    }

    // Before enable durrent plugin, all dependencies should be enabled
    plugin.getDependencies().each() {

        // First script version, optional dependencies not managed
        if (! it.optional) {

            // Enable all required dependencies
            plugins_enabled.addAll(
                enable_plugin(jenkins_pm, jenkins_pm.getPlugin(it.shortName)))
        }
    }

    // Dependencies are enabled, we can enable current plugin
    plugin_enabled = plugin.isActive() || plugin.isEnabled()
    if (! plugin_enabled || plugin.isDeleted()) {
        plugins_enabled.push(plugin.getShortName())
        plugin.enable()
    }

    return plugins_enabled
}


/* SCRIPT */

def changes = [
    enabled: []
]

try {
    def jenkins_instance = Jenkins.getInstance()
    def jenkins_pm = jenkins_instance.getPluginManager()
    def plugin_name = args[0]

    check_args(plugin_name)
    changes['enabled'] = enable_plugin(jenkins_pm,
                                       jenkins_pm.getPlugin(plugin_name))
    jenkins_instance.save()
}
catch (e) {
    throw new RuntimeException(e.getMessage())
}

println JsonOutput.toJson(changes)

