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

// Get plugin informations form Update Center
def get_plugin(jenkins_uc, plugin_name) {
    def plugin = jenkins_uc.getPlugin(plugin_name)

    if (plugin == null) {
        throw new Exception(
            "Plugin not found in Update Center : ${plugin_name}")
    }
    return plugin
}

// Install plugins if needed
def install_plugin(plugin_state, plugin) {

    // Check plugin state
    plugin_installed = plugin.getInstalled()
    plugin_need_update = plugin_installed \
                            && (plugin_state == 'latest') \
                            && plugin_installed.hasUpdate()

    // Install plugin if needed
    if ((plugin.getInstalled() == null) || plugin_need_update) {
        plugin.deploy()
        return true
    }
    return false
}


/* SCRIPT */

def installed = false

try {
    def jenkins_instance = Jenkins.getInstance()
    def jenkins_uc = jenkins_instance.getUpdateCenter()
    def plugin_name = args[0]
    def plugin_state = args[1]

    check_args(plugin_name)
    def jenkins_plugin = get_plugin(jenkins_uc, plugin_name)

    installed = install_plugin(plugin_state, jenkins_plugin)
    jenkins_instance.save()
}
catch (e) {
    throw new RuntimeException(e.getMessage())
}

println JsonOutput.toJson(installed)

