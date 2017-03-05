#!/usr/bin/env groovy

import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.CredentialsStore
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import com.cloudbees.jenkins.plugins.sshcredentials.impl.*
import com.dabsquared.gitlabjenkins.connection.GitLabApiTokenImpl
import hudson.plugins.sshslaves.*
import hudson.util.Secret
import groovy.json.*
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl


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
    Get current credentials store

    @param Jenkins Jenkins instance
    @return CredentialsStore Credential store
*/
def CredentialsStore get_credential_store(Jenkins jenkins_instance) {

    try {
        def CredentialsStore store = Jenkins.instance.getExtensionList(
            'com.cloudbees.plugins.credentials.SystemCredentialsProvider'
        )[0].getStore()

        return store
    }
    catch(Exception e) {
        throw new Exception(
            "Get store error, error message : ${e.getMessage()}")
    }
}


/**
    Get domain

    @param String Needed domain name
    @param CredentialsStore Store instance
    @return Domain Domain or null
*/
def Domain get_domain(String domain_name, CredentialsStore store) {

    try {
        // Special case : the Global domain
        if (domain_name.toLowerCase() == 'global') {
            return Domain.global()
        }

        // Else, it's a store domain
        def domains = store.getDomains()
        for (def Domain domain : domains) {
            if (domain.getName() == domain_name) {
                return domain
            }
        }

        // If domain not found, return null
        return null
    }
    catch(Exception e) {
        throw new Exception(
            "Get domain error, error message : ${e.getMessage()}")
    }
}


/**
    Get credentials scope

    @param String Needed credentials scope
    @return CredentialsScope Credential scope or null
*/
def CredentialsScope get_credentials_scope(String credentials_scope_name) {

    try {
        switch (credentials_scope_name.toLowerCase()) {

            case 'global':
                return CredentialsScope.GLOBAL
                break

            case 'system':
                return CredentialsScope.SYSTEM
                break

            case 'user':
                return CredentialsScope.USER
                break

            default:
                throw new Exception(
                    "Unknown credentials scope : " + credentials_scope_name)
        }
    }
    catch(Exception e) {
        throw new Exception(
            "Get scope error, error message : ${e.getMessage()}")
    }
}


/**
    Get private key source

    @param String Private key source type
    @param String Private key date for this source type
    @return BasicSSHUserPrivateKey.PrivateKeySource Private key source
*/
def BasicSSHUserPrivateKey.PrivateKeySource get_private_key_source(
                                            String private_key_source_type,
                                            String private_key_source_data) {

    try {
        switch (private_key_source_type.toLowerCase()) {

            case "direct_entry":
                return new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(
                                   private_key_source_data)
                break

            case "file_on_master":
                return new BasicSSHUserPrivateKey.FileOnMasterPrivateKeySource(
                                    private_key_source_data)
                break

            case "user":
                return new BasicSSHUserPrivateKey.UsersPrivateKeySource()
                break

            default:
                throw new Exception(
                    "Unknown private key source : " + private_key_source_type)
        }
    }
    catch(Exception e) {
        throw new Exception(
            "Get private key source error, error message : ${e.getMessage()}")
    }
}


/**
    Create SSH credentials with passphrase management

    @param Map Credentials description
    @return BasicSSHUserPrivateKey Credentials object
*/
def BasicSSHUserPrivateKey create_ssh_credentials_with_passphrase(
                                Map credentials_desc) {

    try {
        def BasicSSHUserPrivateKey crendentials

        credentials = new BasicSSHUserPrivateKey(
                            get_credentials_scope(credentials_desc['scope']),
                            credentials_desc['id'],
                            credentials_desc['username'],
                            get_private_key_source(
                                credentials_desc['private_key_source_type'],
                                credentials_desc['private_key_source_data']),
                            credentials_desc['private_key_passphrase'],
                            credentials_desc['description'])

        return credentials
    }
    catch(Exception e) {
        throw new Exception(
            "SSH credentials create error, error message : ${e.getMessage()}")
    }
}


/**
    Create username with password credentials

    @param Map Credentials description
    @return UsernamePasswordCredentialsImpl Credentials object
*/
def UsernamePasswordCredentialsImpl create_password_credentials(
                                            Map credentials_desc) {

    try {
        def UsernamePasswordCredentialsImpl crendentials

        credentials = new UsernamePasswordCredentialsImpl(
                            get_credentials_scope(credentials_desc['scope']),
                            credentials_desc['id'],
                            credentials_desc['description'],
                            credentials_desc['username'],
                            credentials_desc['password'])

        return credentials
    }
    catch(Exception e) {
        throw new Exception(
            "Password credentials create error, "
            + "error message : ${e.getMessage()}")
    }
}


/**
    Create text credentials

    @param Map Credentials description
    @return StringCredentialsImpl Credentials object
*/
def StringCredentialsImpl create_text_credentials(Map credentials_desc) {

    try {
        def StringCredentialsImpl crendentials

        credentials = new StringCredentialsImpl(
                            get_credentials_scope(credentials_desc['scope']),
                            credentials_desc['id'],
                            credentials_desc['description'],
                            Secret.fromString(credentials_desc['text']))

        return credentials
    }
    catch(Exception e) {
        throw new Exception(
            "Text credentials create error, error message : ${e.getMessage()}")
    }
}


/**
    Create Gitlab API credentials

    @param Map Credentials description
    @return GitLabApiTokenImpl Credentials object
*/
def GitLabApiTokenImpl create_gitlab_api_credentials(Map credentials_desc) {

    try {
        def GitLabApiTokenImpl crendentials

        credentials = new GitLabApiTokenImpl(
                            get_credentials_scope(credentials_desc['scope']),
                            credentials_desc['id'],
                            credentials_desc['description'],
                            Secret.fromString(credentials_desc['text']))

        return credentials
    }
    catch(Exception e) {
        throw new Exception(
            "Text credentials create error, error message : ${e.getMessage()}")
    }
}


/**
    Get credentials by its id

    @param CredentialsStore Credentials store
    @param Domain Credentials domain
    @param String Credentials id
    @return BaseStandardCredentials Return credentials if found, else null
*/
def BaseStandardCredentials get_credentials_by_id(CredentialsStore store,
                                                  Domain domain,
                                                  String credentials_id) {

    try {
        def List<Credentials> domain_credentials = store.getCredentials(domain)

        for (def Credentials credentials : domain_credentials) {
            if (credentials.id == credentials_id) {
                return credentials
            }
        }

        // If credentials not exists, return null
        return null
    }
    catch(Exception e) {
        throw new Exception(
            "Credentials get error, error message : ${e.getMessage()}")
    }
}


/**
    Create credentials

    @param Map Credentials description
    @return BaseStandardCredentials Return created credentials
*/
def BaseStandardCredentials create_credentials(Map credentials_desc) {

    try {
        def String credentials_type = credentials_desc['credentials_type']

        switch (credentials_type) {

            case "ssh_with_passphrase":
                return create_ssh_credentials_with_passphrase(credentials_desc)
                break

            case "password":
                return create_password_credentials(credentials_desc)
                break

            case "text":
                return create_text_credentials(credentials_desc)
                break

            case "gitlab_api_token":
                return create_gitlab_api_credentials(credentials_desc)
                break

            default:
                throw new Exception(
                    "Unknown credentials type : " + credentials_type)
        }
    }
    catch(Exception e) {
        throw new Exception(
            "Create credentials error, error message : ${e.getMessage()}")
    }
}


/**
    Check if two credentials have same values

    @param String credentials_type
    @param BaseStandardCredentials credentials_a
    @param BaseStandardCredentials credentials_b
    @return Boolean True if attributes are equals
*/
def Boolean are_same_credentials(
                                String credentials_type,
                                BaseStandardCredentials cred_a,
                                BaseStandardCredentials cred_b) {

    def List<Boolean> has_changed = []

    // Check ID and description
    has_changed.push(cred_a.getId() != cred_b.getId())
    has_changed.push(cred_a.getDescription() != cred_b.getDescription())

    try {
        switch (credentials_type) {

            case "ssh_with_passphrase":
                has_changed.push(cred_a.getUsername() != cred_b.getUsername())
                has_changed.push(cred_a.getPrivateKey() != cred_b.getPrivateKey())
                has_changed.push(cred_a.getPassphrase() != cred_b.getPassphrase())
                break

            case "password":
                has_changed.push(cred_a.getUsername() != cred_b.getUsername())
                has_changed.push(cred_a.getPassword() != cred_b.getPassword())
                break

            case "text":
                has_changed.push(cred_a.getSecret() != cred_b.getSecret())
                break

            case "gitlab_api_token":
                has_changed.push(cred_a.getApiToken() != cred_b.getApiToken())
                break

            default:
                throw new Exception(
                    "Unknown credentials type : " + credentials_type)
        }

        return !has_changed.any()
    }
    catch(Exception e) {
        throw new Exception(
            "Compare credentials error, error message : ${e.getMessage()}")
    }
}


/**
    Add credentials

    @param CredentialsStore Credentials store
    @param Domain Credentials domain
    @param Map Credentials description
    @return Boolean Return True if credentials added
*/
def Boolean add_credentials(CredentialsStore store,
                            Domain domain,
                            Map credentials_desc) {

    try {
        // Create new credentials
        def BaseStandardCredentials credentials
        credentials = create_credentials(credentials_desc)

        return store.addCredentials(domain, credentials)
    }
    catch(Exception e) {
        throw new Exception(
            "Add credentials error, error message : ${e.getMessage()}")
    }
}


/**
    Update credentials

    @param CredentialsStore Credentials store
    @param Domain Credentials domain
    @param BaseStandardCredentials Existing credentials
    @param Map Credentials description
    @return Boolean Return True if credentials updated
*/
def Boolean update_credentials(CredentialsStore store,
                               Domain domain,
                               BaseStandardCredentials cur_credentials,
                               Map credentials_desc) {

    try {
        // Create new credentials
        def BaseStandardCredentials new_credentials
        new_credentials = create_credentials(credentials_desc)
        credentials_type = credentials_desc['credentials_type']

        // Check differences
        if (are_same_credentials(credentials_type, cur_credentials, new_credentials)) {
            return false
        }

        return store.updateCredentials(
                    domain, cur_credentials, new_credentials)
    }
    catch(Exception e) {
        throw new Exception(
            "Update credentials error, error message : ${e.getMessage()}")
    }
}


/**
    Remove credentials

    @param CredentialsStore Credentials store
    @param Domain Credentials domain
    @param BaseStandardCredentials Credentials to remove
    @return Boolean Return True if credentials removed
*/
def Boolean remove_credentials(CredentialsStore store,
                               Domain domain,
                               BaseStandardCredentials credentials) {

    try {
        return store.removeCredentials(domain, credentials)
    }
    catch(Exception e) {
        throw new Exception(
            "Remove credentials error, error message : ${e.getMessage()}")
    }
}


/**
    Manage credentials

    @param CredentialsStore Credentials store
    @param Map Credentials description
    @return Boolean Return True if credentials state or content changed
*/
def Boolean manage_credentials(CredentialsStore store,
                               Map credentials_desc) {

    try {
        // Get credential domain
        def Domain domain = get_domain(
                                credentials_desc['credentials_domain'], store)

        // Get credential if exists
        def BaseStandardCredentials current_credentials
        current_credentials = get_credentials_by_id(
                                    store, domain, credentials_desc['id'])

        def Boolean is_credentials_exists = (current_credentials != null)

        // If credential should be removed
        if (credentials_desc['type'] == 'absent') {
            if (! is_credentials_exists) {
                return false
            }
            return remove_credentials(store, domain, current_credentials)
        }

        // If credentials not exists, add new credential
        if (! is_credentials_exists) {
            return add_credentials(store, domain, credentials_desc)
        }

        // Else, update it
        return update_credentials(
                    store, domain, current_credentials, credentials_desc)
    }
    catch(Exception e) {
        throw new Exception(
            "Credentials management error, error message : ${e.getMessage()}")
    }
}


/* SCRIPT */

def Boolean has_changed = false

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()
    def credentials_store = get_credential_store(jenkins_instance)

    // Get user data
    data = parse_data(args[0])

    //manage credentials
    has_changed = manage_credentials(credentials_store, data)

    // Save new configuration to disk
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

