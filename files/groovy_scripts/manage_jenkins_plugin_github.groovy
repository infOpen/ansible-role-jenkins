#!/usr/bin/env groovy

import groovy.json.*
import hudson.model.*
import jenkins.model.*
import org.jenkinsci.plugins.github.config.GitHubServerConfig
import org.jenkinsci.plugins.github.GitHubPlugin


/**
    Convert Json string to Groovy Object

    @param String arg Json string to parse
    @return Object Groovy object used to get data
*/
def Object parse_data(String arg) {

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
    Get github config by url and credetials

    @param List<GitHubServerConfig> All github plugin configuration
    @param String Github url
    @param String Credentials id
    @return GitHubServerConfig Configuration if found, else null
*/
def GitHubServerConfig get_github_config(List<GitHubServerConfig> configs,
                                         String url,
                                         String credentials_id) {

    try {
        // Default url is the github API URL
        def String GITHUB_API_URL = 'https://api.github.com'

        for (def config : configs) {

            def Boolean is_same_url
            def Boolean is_same_credentials

            // Check if same url than this configuration
            if (url == '') {
                is_same_url = (config.getApiUrl() == GITHUB_API_URL)
            }
            else {
                is_same_url = (config.getApiUrl() == url)
            }

            // Check if same credentials id than this configuration
            is_same_credentials = (config.getCredentialsId() == credentials_id)

            // If these two settings are the same, return this configuration
            if (is_same_url && is_same_credentials) {
                return config
            }
        }

        return null
    }
    catch(Exception e) {
        throw new Exception(
            'Get github configuration error, error message : '
            + e.getMessage())
    }
}


/**
    Add configuration

    @param Descriptor Github plugin descriptor
    @param List<GitHubServerConfig> All github plugin configuration
    @param Map Needed configuration
    @return Boolean True if configuration added
*/
def Boolean add_github_config(Descriptor desc,
                              List<GitHubServerConfig> configs,
                              Map data) {

    try {
        def GitHubServerConfig config = new GitHubServerConfig(
                                                data['credentials_id'])

        config.setManageHooks(data['manage_hooks'])
        config.setClientCacheSize(data['client_cache_size'])

        // If this configuration use custom url
        if (data['custom_url'] != '') {
            config.setCustomUrl(true)
            config.setApiUrl(data['custom_url'])
        }

        // Add configuration
        configs.add(config)
        desc.setConfigs(configs)

        return true
    }
    catch(Exception e) {
        throw new Exception(
            'Add github configuration error, error message : '
            + e.getMessage())
    }
}


/**
    Update configuration

    @param Descriptor Github plugin descriptor
    @param List<GitHubServerConfig> All github plugin configuration
    @param GitHubServerConfig All github plugin configuration
    @param Map Needed configuration
    @return Boolean True if configuration added
*/
def Boolean update_github_config(Descriptor desc,
                                 List<GitHubServerConfig> configs,
                                 GitHubServerConfig config,
                                 Map data) {

    try {
        def Boolean has_changed = false

        // Check manage hooks setting
        if (config.isManageHooks() != data['manage_hooks']) {
            config.setManageHooks(data['manage_hooks'])
            has_changed = true
        }

        // Check client cache size setting
        if (config.getClientCacheSize() != data['client_cache_size']) {
            config.setClientCacheSize(data['client_cache_size'])
            has_changed = true
        }

        // If changes occurs, set new config
        if (has_changed) {
            desc.setConfigs(configs)
        }

        return has_changed
    }
    catch(Exception e) {
        throw new Exception(
            'Add github configuration error, error message : '
            + e.getMessage())
    }
}


/**
    Manage configuration

    @param Descriptor Github plugin descriptor
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_github_config(Descriptor desc, Map data) {

    try {
        // Get current plugin configuration
        def List<GitHubServerConfig> configs = desc.getConfigs()

        def GitHubServerConfig config = get_github_config(
                                            configs,
                                            data['custom_url'],
                                            data['credentials_id'])

        // Configuration not exists, create it
        if (config == null) {
            return add_github_config(desc, configs, data)
        }

        // Else, update existing
        return update_github_config(desc, configs, config, data)
    }
    catch(Exception e) {
        throw new Exception(
            'Manage github configuration error, error message : '
            + e.getMessage())
    }
}


/* SCRIPT */

def Boolean has_changed = false

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()
    def Descriptor desc
    desc = jenkins_instance.getDescriptor('github-plugin-configuration')

    // Get arguments data
    def Object data = parse_data(args[0])

    // Manage new configuration
    has_changed = manage_github_config(desc, data)

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

