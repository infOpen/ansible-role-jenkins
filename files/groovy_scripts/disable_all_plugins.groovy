#!/usr/bin/env groovy

import jenkins.model.*
import groovy.json.*


def jenkins_instance = Jenkins.getInstance()
def jenkins_pm = jenkins_instance.getPluginManager()
def jenkins_plugins = jenkins_pm.getPlugins()

jenkins_plugins.each {
    it.disable()
}

println JsonOutput.toJson(jenkins_instance.save())

