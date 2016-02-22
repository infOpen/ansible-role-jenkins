#!/usr/bin/env groovy

import jenkins.model.*
import groovy.json.*


/* FUNCTIONS */

def get_plugin_dependencies(jenkins_uc, plugin_name) {

    def plugin = jenkins_uc.getPlugin(plugin_name)
    def dependencies = plugin.dependencies.keySet() as String[]
    def add_dep = []
    dependencies.each() {
        add_dep = [ *get_plugin_dependencies(jenkins_uc, it), *add_dep ]
    }
    return [ *add_dep, *dependencies ]
}


/* SCRIPT */

def dependencies = []

try {
    def jenkins_instance = Jenkins.getInstance()
    def jenkins_uc = jenkins_instance.getUpdateCenter()
    jenkins_uc.updateAllSites()

    def plugin_name = args[0]
    dependencies = [ *get_plugin_dependencies(jenkins_uc, plugin_name),
                     plugin_name ]

}
catch (e) {
    throw new RuntimeException(e.getMessage())
}

println JsonOutput.toJson(dependencies.unique())

