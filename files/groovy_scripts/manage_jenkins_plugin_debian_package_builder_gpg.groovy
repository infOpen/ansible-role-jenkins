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
    Manage debian package builder gpg account name configuration

    @param Descriptor Debian package builder plugin descriptor
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_gpg_account_name(Descriptor desc, Map data) {

    try {

        // Get current gpg account name
        def String cur_gpg_account_name = desc.getAccountName()

        // Change setting if different value
        if (cur_gpg_account_name != data['name']) {
            desc.setAccountName(data['name'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage debian package builder gpg account name error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage debian package builder gpg account email configuration

    @param Descriptor Debian package builder plugin descriptor
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_gpg_account_email(Descriptor desc, Map data) {

    try {

        // Get current gpg account email
        def String cur_gpg_account_email = desc.getAccountEmail()

        // Change setting if different value
        if (cur_gpg_account_email != data['email']) {
            desc.setAccountEmail(data['email'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage debian package builder gpg account email error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage debian package builder gpg public key configuration

    @param Descriptor Debian package builder plugin descriptor
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_gpg_public_key(Descriptor desc, Map data) {

    try {

        // Get current gpg public key
        def String cur_gpg_public_key = desc.getPublicKey()

        // Change setting if different value
        if (cur_gpg_public_key != data['public_key']) {
            desc.setPublicKey(data['public_key'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage debian package builder gpg public key error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage debian package builder gpg private key configuration

    @param Descriptor Debian package builder plugin descriptor
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_gpg_private_key(Descriptor desc, Map data) {

    try {

        // Get current gpg private key
        def String cur_gpg_private_key = desc.getPrivateKey()

        // Change setting if different value
        if (cur_gpg_private_key != data['private_key']) {
            desc.setPrivateKey(data['private_key'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage debian package builder gpg private key error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage debian package builder gpg passphrase configuration

    @param Descriptor Debian package builder plugin descriptor
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_gpg_passphrase(Descriptor desc, Map data) {

    try {

        // Get current gpg passphrase
        def String cur_gpg_passphrase = desc.getPassphrase()

        // Change setting if different value
        if (cur_gpg_passphrase != data['passphrase']) {
            desc.setPassphrase(data['passphrase'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage debian package builder gpg passphrase error, '
            + 'error message : ' + e.getMessage())
    }
}


/* SCRIPT */

def List<Boolean> has_changed = []

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()
    def desc = jenkins_instance.getDescriptor(
        'ru.yandex.jenkins.plugins.debuilder.DebianPackageBuilder')

    // Get arguments data
    def Map data = parse_data(args[0])

    // Manage new configuration
    has_changed.push(manage_gpg_account_name(desc, data))
    has_changed.push(manage_gpg_account_email(desc, data))
    has_changed.push(manage_gpg_public_key(desc, data))
    has_changed.push(manage_gpg_private_key(desc, data))
    has_changed.push(manage_gpg_passphrase(desc, data))

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

