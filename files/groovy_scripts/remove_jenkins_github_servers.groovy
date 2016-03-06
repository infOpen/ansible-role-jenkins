#!/usr/bin/env groovy

import groovy.json.*
import hudson.model.*
import jenkins.model.*
import org.jenkinsci.plugins.github.config.GitHubServerConfig
import org.jenkinsci.plugins.github.GitHubPlugin


/* SCRIPT */

def Boolean has_changed = false

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()
    def Descriptor desc
    desc = jenkins_instance.getDescriptor('github-plugin-configuration')

    // Manage new configuration
    def List<GitHubServerConfig> configs = desc.getConfigs()

    // Empty github plugin configuration
    desc.setConfigs([])
    has_changed = (configs.size() > 0)

    // Save new configuration to disk
    desc.save()
    jenkins_instance.save()
}
catch(Exception e) {
    throw new RuntimeException(e.getMessage())
}

// Build json result
result = new JsonBuilder()
result {
    changed has_changed.any()
    output {
        changed has_changed
    }
}

println result

