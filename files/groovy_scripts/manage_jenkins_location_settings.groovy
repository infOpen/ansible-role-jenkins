#!/usr/bin/env groovy

import jenkins.model.*
import jenkins.model.ProjectNamingStrategy
import jenkins.model.ProjectNamingStrategy.PatternProjectNamingStrategy \
    as PatternProjectNaming
import hudson.model.*
import groovy.json.*


/**
    Set the Jenkins administrator email address

    @param JenkinsLocationConfiguration Jenkins location configuration
    @param String New value for administrator email address
    @return Boolean True if changed, else false
*/
def Boolean set_administrator_email(JenkinsLocationConfiguration location,
                                    String new_value) {

    // Get current value, used to check if changed
    def String cur_value = location.getAdminAddress()
    if (cur_value == new_value) {
        return false
    }

    try {
        location.setAdminAddress(new_value)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during administrator email address change')
    }

    return true
}


/**
    Set the Jenkins location url

    @param JenkinsLocationConfiguration Jenkins location configuration
    @param String New value for jenkins url
    @return Boolean True if changed, else false
*/
def Boolean set_url(JenkinsLocationConfiguration location, String new_value) {

    // Get current value, used to check if changed
    def String cur_value = location.getUrl()
    if (cur_value == new_value) {
        return false
    }

    try {
        location.setUrl(new_value)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during Jenkins url change')
    }

    return true
}


/* SCRIPT */

def List <Boolean> has_changed = []
def String new_address = ""
def String new_url = ""

try {
    def JenkinsLocationConfiguration location
    location = JenkinsLocationConfiguration.get()

    // Get arguments data
    def String full_name = args[0]
    def String email = args[1]
    def String url = args[2]
    new_address = "${full_name} <${email}>"
    new_url = "${url}"

    // Manage configuration with user data
    has_changed.push(set_administrator_email(location, new_address))
    has_changed.push(set_url(location, url))

    // Save new configuration to disk
    location.save()
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
        address new_address
        url new_url
    }
}

println result

