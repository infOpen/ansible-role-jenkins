#!/usr/bin/env groovy

import jenkins.model.*
import groovy.json.*


def jenkins_instance = Jenkins.getInstance()
def jenkins_pm = jenkins_instance.getPluginManager()
def jenkins_plugins = jenkins_pm.getPlugins()
def plugins_status = [
    active: [],
    deleted: [],
    disabled: [],
    enabled: [],
    has_update: [],
    installed: []
]

jenkins_plugins.each {

    plugins_status['installed'].push(it.getShortName())

    if (it.isActive()) {
        plugins_status['active'].push(it.getShortName())
    }

    if (it.hasUpdate()) {
        plugins_status['has_update'].push(it.getShortName())
    }

    if (it.isEnabled()) {
        plugins_status['enabled'].push(it.getShortName())
    }
    else if (it.isDeleted()) {
        plugins_status['deleted'].push(it.getShortName())
    }
    else {
        plugins_status['disabled'].push(it.getShortName())
    }
}

println JsonOutput.toJson(plugins_status)

