#!/usr/bin/env groovy

import groovy.json.*
import hudson.model.*
import jenkins.model.*
import ru.yandex.jenkins.plugins.debuilder.DebianPackageRepo


/* SCRIPT */

def Boolean has_changed = false

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()
    def Descriptor desc
    desc = jenkins_instance.getDescriptor(
        'ru.yandex.jenkins.plugins.debuilder.DebianPackagePublisher')

    // Get current configuration
    def List<DebianPackageRepo> repositories = desc.getRepositories()

    // Empty debian package builder plugin repositories configuration
    desc.repos = []
    has_changed = (repositories.size() > 0)

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
    changed has_changed
    output {
        changed has_changed
    }
}

println result

