#!/usr/bin/env groovy

import groovy.json.*
import hudson.model.*
import hudson.util.CopyOnWriteList
import java.net.URL
import jenkins.model.*
import hudson.plugins.jira.JiraSite


/**
    Convert Json string to Groovy Object

    @param String arg Json string to parse
    @return Map Groovy object used to get data
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
    Manage Jira site alt url configuration

    @param JiraSite Site to update
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_alt_url_update(JiraSite site, Map data) {

    try {

        // Get current site alt_url
        def URL cur_alt_url = site.alternativeUrl.toExternalForm()
        def URL new_alt_url
        if (data['jira_alt_url'] != '') {
            new_alt_url = new URL(data['jira_alt_url']).toExternalForm()
        }
        else {
            new_alt_url = new URL(data['jira_url']).toExternalForm()
        }

        // Change setting if different value
        if (cur_alt_url != new_alt_url) {
            site.alternativeUrl = new URL(data['jira_alt_url'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage Jira alt url error, error message : ' + e.getMessage())
    }
}


/**
    Manage Jira site http auth configuration

    @param JiraSite Site to update
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_http_auth_update(JiraSite site, Map data) {

    try {

        // Get current site http auth
        def Boolean cur_http_auth = site.useHTTPAuth

        // Change setting if different value
        if (cur_http_auth != data['use_http_auth']) {
            site.useHTTPAuth = data['use_http_auth']
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage Jira http_auth error, error message : ' + e.getMessage())
    }
}


/**
    Manage Jira site user configuration

    @param JiraSite Site to update
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_incoming_update(JiraSite site, Map data) {

    try {

        // Get current repo incoming
        def String cur_repo_incoming = site.getIncoming()

        // Change setting if different value
        if (cur_repo_incoming != data['incoming']) {
            site.setIncoming(data['incoming'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage Jira repo incoming error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage Jira repo login configuration

    @param JiraSite Site to update
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_login_update(JiraSite site, Map data) {

    try {

        // Get current repo login
        def String cur_repo_login = site.getLogin()

        // Change setting if different value
        if (cur_repo_login != data['login']) {
            site.setLogin(data['login'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage Jira repo login error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage Jira repo key_path configuration

    @param JiraSite Site to update
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_key_path_update(JiraSite site, Map data) {

    try {

        // Get current repo key path
        def String cur_repo_key_path = site.getKeypath()

        // Change setting if different value
        if (cur_repo_key_path != data['key_path']) {
            site.setKeypath(data['key_path'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage Jira repo key_path error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage Jira repo options configuration

    @param JiraSite Site to update
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_options_update(JiraSite site, Map data) {

    try {

        // Get current repo options
        def String cur_repo_options = site.getOptions()

        // Change setting if different value
        if (cur_repo_options != data['options']) {
            site.setOptions(data['options'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage Jira repo options error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage Jira site update

    @param Descriptor Jira plugin descriptor
    @param JiraSite[] Sites
    @param integer Site to update
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean update_site(Descriptor desc,
                        JiraSite[] sites,
                        Integer index,
                        Map data) {

    try {

        def List<Boolean> has_changed = []
        def site = sites[index]

        // Check if site settings need update
        has_changed.push(manage_alt_url_update(site, data))
        has_changed.push(manage_http_auth_update(site, data))
        has_changed.push(manage_user_update(site, data))
        has_changed.push(manage_password_update(site, data))
        has_changed.push(manage_group_visibility_update(site, data))
        has_changed.push(manage_role_visibility_update(site, data))
        has_changed.push(manage_support_wiki_style_update(site, data))
        has_changed.push(manage_record_scm_changes_update(site, data))
        has_changed.push(manage_timeout_update(site, data))
        has_changed.push(manage_datetime_pattern_update(site, data))
        has_changed.push(manage_user_pattern_update(site, data))
        has_changed.push(manage_append_change_timestamp_update(site, data))
        has_changed.push(manage_update_for_all_status_update(site, data))

        return has_changed.any()
    }
    catch(Exception e) {
        throw new Exception(
            'Manage Jira site update error, error message : ' + e.getMessage())
    }
}


/**
    Manage Jira site create

    @param Descriptor Jira plugin descriptor
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean add_site(Descriptor desc, Map data) {

    try {

        // Create URL objects
        def URL main_url = new URL(data['jira_url'])
        def URL alt_url = null

        if (data['jira_alt_url'] != '') {
            alt_url = new URL(data['jira_alt_url'])
        }
        else {
            alt_url = new URL(data['jira_url'])
        }

        // Create new site
        def JiraSite site = new JiraSite(
                                    main_url,
                                    alt_url,
                                    data['user'],
                                    data['password'],
                                    data['supports_wiki_style'],
                                    data['record_scm_changes'],
                                    data['user_pattern'],
                                    data['update_for_all_status'],
                                    data['group_visibility'],
                                    data['role_visibility'],
                                    data['use_http_auth'])

        // Additional setters
        site.setTimeout(data['timeout'])
        site.setDateTimePattern(data['datetime_pattern'])
        site.setAppendChangeTimestamp(data['append_change_timestamp'])

        desc.sites.add(site)

        return true
    }
    catch(Exception e) {
        throw new Exception(
            'Add Jira site error, error message : ' + e.getMessage())
    }
}


/**
    Manage Jira sites

    @param Descriptor Jira plugin descriptor
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_site(Descriptor desc, Map data) {

    try {

        // Get sites
        def JiraSite[] sites = desc.getSites()

        // Get site with this url if exists
        def Integer index = sites.findIndexOf {
            it.getName() == new URL(data['jira_url']).toExternalForm()
        }

        if (index == -1) {
            if (data['state'] == 'present') {
                return add_site(desc, data)
            }
            return false
        }

        if (data['state'] == 'absent') {
            desc.sites = null
            return true
        }

        return update_site(desc, sites, index, data)
    }
    catch(Exception e) {
        throw new Exception(
                    'Jira site management error, error message : '
                    + e.getMessage())
    }
}


/* SCRIPT */

def Boolean has_changed = false

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()
    def desc = jenkins_instance.getDescriptor(
                                    'hudson.plugins.jira.JiraProjectProperty')

    // Get arguments data
    def Map data = parse_data(args[0])

    // Manage new configuration
    has_changed = manage_site(desc, data)

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

