#!/usr/bin/env groovy

import groovy.json.*
import hudson.model.*
import jenkins.model.*
import com.dabsquared.gitlabjenkins.connection.GitLabConnection
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig


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
    Check if two Gitlab configuration objects have the same properties

    @param GitLabConnection Old configuration object
    @param GitLabConnection New configuration object
    @return Boolean True configuration are the same, else false
*/
def Boolean is_same_connection(GitLabConnection old_connection, GitLabConnection new_connection) {

    try {

        def List<Boolean> checks = []

        // Check properties
        checks.push(old_connection.getName() != new_connection.getName())
        checks.push(old_connection.getApiTokenId() != new_connection.getApiTokenId())
        checks.push(old_connection.getUrl() != new_connection.getUrl())
        checks.push(old_connection.isIgnoreCertificateErrors() != new_connection.isIgnoreCertificateErrors())
        checks.push(old_connection.getConnectionTimeout() != new_connection.getConnectionTimeout())
        checks.push(old_connection.getReadTimeout() != new_connection.getReadTimeout())

        return !checks.any()
    }
    catch(Exception e) {
        throw new Exception(
            'Check connections properties error, error message : ' + e.getMessage())
    }
}


/**
    Manage GitLab

    @param Jenkins Jenkins instance
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_gitlab(Jenkins jenkins_instance, Map data) {
    try {
        def desc = jenkins_instance.getDescriptor(
            'com.dabsquared.gitlabjenkins.GitLabPushTrigger')
        // Manage new empty connections list
        def List<GitLabConnection> new_connections = new ArrayList<>()

        // Manage existing connection with this name
        def GitLabConnection old_connection

        // Manage a connection for needed connection
        def GitLabConnection new_connection = new GitLabConnection(
            data['name'],
            data['host_url'],
            data['api_token'],
            data['ignore_cert_error'],
            data['connection_timeout'],
            data['read_timeout'])

        // Get current plugin configuration
        GitLabConnectionConfig gitLabConfig = (GitLabConnectionConfig) Jenkins.getInstance().getDescriptor(GitLabConnectionConfig.class)

        // Copy existing connections if name is different
        for (GitLabConnection connection : gitLabConfig.getConnections()) {
            if (connection.getName() != data['name']) {
                new_connections.add(connection)
            } else {
                old_connection = connection
            }
        }
        new_connections.add(new_connection)

        // Return false if same config
        if ((old_connection != null) && is_same_connection(old_connection, new_connection)) {
            return false
        }

        // Clear current configuration
        gitLabConfig.getConnections().clear()

        // Manage new configuration list
        for (GitLabConnection connection : new_connections) {
            gitLabConfig.getConnections().add(connection)
        }

        // Save new configuration to disk
        gitLabConfig.save()
        desc.save()

        return true
    }
    catch(Exception e) {
        throw new Exception(
            'Gitlab management error, error message : ' + e.getMessage())
    }
}


/* SCRIPT */

def Boolean has_changed = false

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()

    // Get arguments data
    def Map data = parse_data(args[0])

    // Manage new configuration
    has_changed = manage_gitlab(jenkins_instance, data)

    // Save new configuration to disk
    if (has_changed) {
        jenkins_instance.save()
    }
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

