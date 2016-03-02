#!/usr/bin/env groovy

import jenkins.model.*
import jenkins.model.ProjectNamingStrategy
import jenkins.model.ProjectNamingStrategy.PatternProjectNamingStrategy \
    as PatternProjectNaming
import hudson.model.*
import groovy.json.*


/**
    Set the Git plugin global email address

    @param Descriptor Git plugin descriptor
    @param String New value for git plugin global email address
    @return Boolean True if changed, else false
*/
def Boolean set_git_plugin_global_email(Descriptor desc, String new_value) {

    // Get current value, used to check if changed
    def String cur_value = desc.getGlobalConfigEmail()
    if (cur_value == new_value) {
        return false
    }

    try {
        desc.setGlobalConfigEmail(new_value)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during git plugin email address change')
    }

    return true
}


/**
    Set the Git plugin global full name

    @param Descriptor Git plugin descriptor
    @param String New value for git plugin global full name
    @return Boolean True if changed, else false
*/
def Boolean set_git_plugin_global_name(Descriptor desc, String new_value) {

    // Get current value, used to check if changed
    def String cur_value = desc.getGlobalConfigName()
    if (cur_value == new_value) {
        return false
    }

    try {
        desc.setGlobalConfigName(new_value)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during git plugin full name change')
    }

    return true
}


/**
    Set the Git plugin create account setting

    @param Descriptor Git plugin descriptor
    @param Boolean New value for git plugin create account
    @return Boolean True if changed, else false
*/
def Boolean set_git_plugin_create_account(Descriptor desc, Boolean new_value) {

    // Get current value, used to check if changed
    def Boolean cur_value = desc.isCreateAccountBasedOnEmail()
    if (cur_value == new_value) {
        return false
    }

    try {
        desc.setCreateAccountBasedOnEmail(new_value)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during git plugin account create change')
    }

    return true
}


/* SCRIPT */

def List <Boolean> has_changed = []
def String new_email = ""
def String new_full_name = ""
def Boolean new_account_create = false

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()
    def Descriptor desc
    desc = jenkins_instance.getDescriptor('hudson.plugins.git.GitSCM')

    // Get arguments data
    new_full_name = args[0]
    new_email = args[1]
    new_account_create = args[2].toBoolean()

    // Manage configuration with user data
    has_changed.push(set_git_plugin_global_email(desc, new_email))
    has_changed.push(set_git_plugin_global_name(desc, new_full_name))
    has_changed.push(set_git_plugin_create_account(desc, new_account_create))

    // Save new configuration to disk
    desc.save()
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
        email new_email
        full_name new_full_name
        account_create new_account_create
    }
}

println result

