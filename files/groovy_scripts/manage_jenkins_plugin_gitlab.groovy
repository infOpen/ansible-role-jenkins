#!/usr/bin/env groovy

import groovy.json.*
import hudson.model.*
import jenkins.model.*


/**
    Convert Json string to Groovy Object

    @param String arg Json string to parse
    @return Map Groovy object used to get data
*/
def Map parse_data(String arg) {

    try {
        def JsonSlurper jsonSlurper = new JsonSlurper()
        return jsonSlurper.parseText(arg)
    }
    catch(Exception e) {
        throw new Exception("Parse data error, incoming data : ${arg}, "
                            + "error message : ${e.getMessage()}")
    }
}


/**
    Manage gitlab api tokenconfiguration

    @param Descriptor Gitlab plugin descriptor
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_api_token(Descriptor desc, Map data) {

    try {

        // Get current api_token
        def String cur_api_token = desc.getGitlabApiToken()

        // Change setting if different value
        if (cur_api_token != data['api_token']) {
            desc.gitlabApiToken = data['api_token']
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage gitlab api token error, error message : ' + e.getMessage())
    }
}


/**
    Manage gitlab host url configuration

    @param Descriptor Gitlab plugin descriptor
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_host_url(Descriptor desc, Map data) {

    try {

        // Get current host url
        def String cur_host_url = desc.getGitlabHostUrl()

        // Change setting if different value
        if (cur_host_url != data['host_url']) {
            desc.gitlabHostUrl = data['host_url']
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage gitlab host url error, error message : ' + e.getMessage())
    }
}


/**
    Manage gitlab cert error configuration

    @param Descriptor Gitlab plugin descriptor
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_ignore_certificate_error(Descriptor desc, Map data) {

    try {

        // Get current setting
        def Boolean cur_ignore_cert_error = desc.getIgnoreCertificateErrors()

        // Change setting if different value
        if (cur_ignore_cert_error != data['ignore_cert_error']) {
            desc.ignoreCertificateErrors = data['ignore_cert_error']
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage gitlab cert management error, error message : '
            + e.getMessage())
    }
}


/* SCRIPT */

def List<Boolean> has_changed = []

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()
    def desc = jenkins_instance.getDescriptor(
        'com.dabsquared.gitlabjenkins.GitLabPushTrigger')

    // Get arguments data
    def Map data = parse_data(args[0])

    // Manage new configuration
    has_changed.push(manage_api_token(desc, data))
    has_changed.push(manage_host_url(desc, data))
    has_changed.push(manage_ignore_certificate_error(desc, data))

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

