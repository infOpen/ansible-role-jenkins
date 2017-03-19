#!/usr/bin/env groovy

import groovy.json.*
import hudson.model.*
import jenkins.model.*
import ru.yandex.jenkins.plugins.debuilder.DebianPackageRepo


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
    Manage debian package builder repo method configuration

    @param DebianPackageRepo Repository to update
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_method_update(DebianPackageRepo repository, Map data) {

    try {

        // Get current repo method
        def String cur_repo_method = repository.getMethod()

        // Change setting if different value
        if (cur_repo_method != data['method']) {
            repository.setMethod(data['method'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage debian package builder repo method error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage debian package builder repo fqdn configuration

    @param DebianPackageRepo Repository to update
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_fqdn_update(DebianPackageRepo repository, Map data) {

    try {

        // Get current repo fqdn
        def String cur_repo_fqdn = repository.getFqdn()

        // Change setting if different value
        if (cur_repo_fqdn != data['fqdn']) {
            repository.setFqdn(data['fqdn'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage debian package builder repo fqdn error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage debian package builder repo incoming configuration

    @param DebianPackageRepo Repository to update
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_incoming_update(DebianPackageRepo repository, Map data) {

    try {

        // Get current repo incoming
        def String cur_repo_incoming = repository.getIncoming()

        // Change setting if different value
        if (cur_repo_incoming != data['incoming']) {
            repository.setIncoming(data['incoming'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage debian package builder repo incoming error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage debian package builder repo login configuration

    @param DebianPackageRepo Repository to update
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_login_update(DebianPackageRepo repository, Map data) {

    try {

        // Get current repo login
        def String cur_repo_login = repository.getLogin()

        // Change setting if different value
        if (cur_repo_login != data['login']) {
            repository.setLogin(data['login'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage debian package builder repo login error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage debian package builder repo key_path configuration

    @param DebianPackageRepo Repository to update
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_key_path_update(DebianPackageRepo repository, Map data) {

    try {

        // Get current repo key path
        def String cur_repo_key_path = repository.getKeypath()

        // Change setting if different value
        if (cur_repo_key_path != data['key_path']) {
            repository.setKeypath(data['key_path'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage debian package builder repo key_path error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage debian package builder repo options configuration

    @param DebianPackageRepo Repository to update
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_options_update(DebianPackageRepo repository, Map data) {

    try {

        // Get current repo options
        def String cur_repo_options = repository.getOptions()

        // Change setting if different value
        if (cur_repo_options != data['options']) {
            repository.setOptions(data['options'])
            return true
        }

        return false
    }
    catch(Exception e) {
        throw new Exception(
            'Manage debian package builder repo options error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage debian package publisher repository update

    @param Descriptor Debian package builder plugin descriptor
    @param List<DebianPackageRepo> Repositories
    @param integer Repository to update
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean update_repository(Descriptor desc,
                              List<DebianPackageRepo> repositories,
                              Integer index,
                              Map data) {

    try {

        def List<Boolean> has_changed = []
        def repository = repositories[index]

        // Check if repository settings need update
        has_changed.push(manage_method_update(repository, data))
        has_changed.push(manage_fqdn_update(repository, data))
        has_changed.push(manage_incoming_update(repository, data))
        has_changed.push(manage_login_update(repository, data))
        has_changed.push(manage_key_path_update(repository, data))
        has_changed.push(manage_options_update(repository, data))

        return has_changed.any()
    }
    catch(Exception e) {
        throw new Exception(
            'Manage debian package builder repository update error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage debian package publisher repository create

    @param Descriptor Debian package builder plugin descriptor
    @param List<DebianPackageRepo> Repositories
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean add_repository(Descriptor desc,
                           List<DebianPackageRepo> repositories,
                           Map data) {

    try {

        // Get current gpg private key
        def DebianPackageRepo repository = new DebianPackageRepo(
                                                        data['name'],
                                                        data['method'],
                                                        data['fqdn'],
                                                        data['incoming'],
                                                        data['login'],
                                                        data['options'],
                                                        data['key_path'])

        repositories.push(repository)
        desc.repos = repositories

        return true
    }
    catch(Exception e) {
        throw new Exception(
            'Add debian package publisher repository error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage debian package publisher repositories

    @param Descriptor Debian package builder plugin descriptor
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_repository(Descriptor desc, Map data) {

    try {

        // Get repositories
        def List<DebianPackageRepo> repositories = desc.getRepositories()

        // Get repository with this name if exists
        def Integer index = repositories.findIndexOf {
            it.getName() == data['name']
        }

        if (index == -1) {
            if (data['state'] == 'present') {
                return add_repository(desc, repositories, data)
            }
            return false
        }

        if (data['state'] == 'absent') {
            repositories.remove(index)
            desc.repos = repositories
            return true
        }

        return update_repository(desc, repositories, index, data)
    }
    catch(Exception e) {
        throw new Exception(
            'Manage debian package publisher repository management error, '
            + 'error message : ' + e.getMessage())
    }
}


/* SCRIPT */

def Boolean has_changed = false

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()
    def desc = jenkins_instance.getDescriptor(
        'ru.yandex.jenkins.plugins.debuilder.DebianPackagePublisher')

    // Get arguments data
    def Map data = parse_data(args[0])

    // Manage new configuration
    has_changed = manage_repository(desc, data)

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

