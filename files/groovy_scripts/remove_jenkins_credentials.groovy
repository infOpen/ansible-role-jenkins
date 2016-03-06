#!/usr/bin/env groovy

import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.CredentialsStore
import groovy.json.*


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
    Remove domain credentials

    @param CredentialsStore Credentials store
    @param String Domain_name
    @return Boolean Return True if credentials state or content changed
*/
def Boolean remove_domain_credentials(CredentialsStore store,
                                      String domain_name) {

    try {
        // Get domain
        def Domain domain = get_domain(domain_name, store)

        // Get domain credentials
        def List<Credentials> domain_credentials = store.getCredentials(domain)

        // If domain contains credentials, empty it
        for (def Credentials credentials : domain_credentials) {
            store.removeCredentials(domain, credentials)
        }

        return (domain_credentials.size() > 0)
    }
    catch(Exception e) {
        throw new Exception(
            "Remove credentials error, error message : ${e.getMessage()}")
    }
}


/* SCRIPT */

def Boolean has_changed = false

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()
    def credentials_store = get_credential_store(jenkins_instance)

    // Get user data
    def String domain_name = args[0]

    //manage credentials
    has_changed = remove_domain_credentials(credentials_store, domain_name)

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

