#!/usr/bin/env groovy

import jenkins.model.*
import jenkins.plugins.git.GitSCMSource
import jenkins.scm.api.SCMSource
import hudson.model.*
import groovy.json.*
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import org.jenkinsci.plugins.workflow.libs.LibraryRetriever
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever


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
    catch(e) {
        throw new Exception("Parse data error, incoming data : ${arg}, "
                            + "error message : ${e.getMessage()}")
    }
}


/**
    Check if two scm have save properties

    @param SCMSource First scm
    @param SCMSource Second scm
    @return Boolean True if scm are same, else false
*/
def Boolean are_same_scm(SCMSource scm_a, SCMSource scm_b) {

    try {
        def List<Boolean> checks = []

        if (scm_a.getClass().getName() != scm_b.getClass().getName()) {
            return false
        }

        if (scm_a.getClass().getName() == 'jenkins.plugins.git.GitSCMSource') {
            checks.add(scm_a.getCredentialsId() == scm_b.getCredentialsId())
            checks.add(scm_a.getRemote() == scm_b.getRemote())
        }

        return checks.every()
    }
    catch(e) {
        throw new Exception(
            "SCM compare error, error message : ${e.getMessage()}")
    }
}


/**
    Manage SCM

    @param Map Needed configuration
    @return SCMSource SCM object to use with retriever
*/
def SCMSource manage_scm(Map data) {

    try {

        def SCMSource scm = null

        if (data['scm']['type'] == 'git') {
            scm = new GitSCMSource(data['scm']['remote'])
            scm.setCredentialsId(data['scm']['credentials_id'])
        }

        return scm
    }
    catch(Exception e) {
        throw new Exception(
            "SCM management error, error message : ${e.getMessage()}")
    }
}


/**
    Check if two retrievers have save properties

    @param LibraryRetriever First retriever
    @param LibraryRetriever Second retriever
    @return Boolean True if retrievers are same, else false
*/
def Boolean are_same_retrievers(LibraryRetriever retriever_a, LibraryRetriever retriever_b) {

    try {
        def List<Boolean> checks = []

        if (retriever_a.getClass().getName() != retriever_b.getClass().getName()) {
            return false
        }

        if (retriever_a.getClass().getName() == 'org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever') {
            checks.add(are_same_scm(retriever_a.getScm(), retriever_b.getScm()))
        }

        return checks.every()
    }
    catch(e) {
        throw new Exception(
            "Retrievers compare error, error message : ${e.getMessage()}")
    }
}


/**
    Manage Retriever

    @param Map Needed configuration
    @return LibraryRetriever Retriever to use with Library
*/
def LibraryRetriever manage_retriever(Map data) {

    try {

        def LibraryRetriever retriever = null

        if (data.containsKey('scm') && data['scm']['type'] == 'git') {
            retriever = new SCMSourceRetriever(manage_scm(data))
        }

        return retriever
    }
    catch(Exception e) {
        throw new Exception(
            "Retriever management error, error message : ${e.getMessage()}")
    }
}


/**
    Check if two shared libraries have save properties

    @param LibraryConfiguration First library
    @param LibraryConfiguration Second library
    @return Boolean True if libraries are same, else false
*/
def Boolean are_same_shared_libraries(LibraryConfiguration library_a, LibraryConfiguration library_b) {

    try {
        def List<Boolean> checks = []

        checks.add(library_a.getDefaultVersion() == library_b.getDefaultVersion())
        checks.add(library_a.isImplicit() == library_b.isImplicit())
        checks.add(library_a.isAllowVersionOverride() == library_b.isAllowVersionOverride())
        checks.add(library_a.getIncludeInChangesets() == library_b.getIncludeInChangesets())
        checks.add(are_same_retrievers(library_a.getRetriever(), library_b.getRetriever()))

        return checks.every()
    }
    catch(e) {
        throw new Exception(
            "Libraries compare error, error message : ${e.getMessage()}")
    }
}


/**
    Create and return a shared library

    @param Map Library data
    @return LibraryConfiguration True if configuration change everything, else false
*/
def LibraryConfiguration create_shared_library(Map data) {

    try {
        def LibraryConfiguration library = null
        def LibraryRetriever retriever = manage_retriever(data)

        library = new LibraryConfiguration(data['name'], retriever)
        library.setDefaultVersion(data.get('default_version', ''))
        library.setImplicit(data.get('implicit', false))
        library.setAllowVersionOverride(data.get('allow_version_override', true))
        library.setIncludeInChangesets(data.get('include_in_changesets', true))

        return library
    }
    catch(e) {
        throw new Exception(
            "Library create error, error message : ${e.getMessage()}")
    }
}


/**
    Add shared library

    @param Descriptor Workflow libs plugin descriptor
    @param Map Library data
    @return Boolean True if configuration change everything, else false
*/
def Boolean add_shared_library(Descriptor desc, Map data) {

        def LibraryConfiguration library = create_shared_library(data)
        def List<LibraryConfiguration> libraries = desc.getLibraries()

    try {
        libraries.add(library)
        desc.setLibraries(libraries)

        return true
    }
    catch(e) {
        throw new Exception(
            "Library add error, error message : ${e.getMessage()}")
    }
}


/**
    Update shared library

    @param Descriptor Workflow libs plugin descriptor
    @param List<LibraryConfiguration> libraries
    @param Integer library_id
    @param Map Library data
    @return Boolean True if configuration change everything, else false
*/
def Boolean update_shared_library(Descriptor desc, List<LibraryConfiguration> libraries, Integer library_id, Map data) {

    try {
        def LibraryConfiguration current_library = libraries[library_id]
        def LibraryConfiguration new_library = create_shared_library(data)

        if (are_same_shared_libraries(current_library, new_library)) {
            return false
        }

        libraries[library_id] = new_library
        desc.setLibraries(libraries)

        return true
    }
    catch(e) {
        throw new Exception(
            "Library update error, error message : ${e.getMessage()}")
    }
}


/**
    Get shared library by name

    @param List<LibraryConfiguration> libraries
    @param String Library name
    @return Integer Needed library if exists, else null
*/
def Integer get_library_id_by_name(List<LibraryConfiguration> libraries, name) {

    try {
        def library_id = null

        libraries.eachWithIndex{ library , index ->
            if (library.getName() == name) {
                library_id = index
            }
        }

        return library_id
    }
    catch(e) {
        throw new Exception(
            "Libraries get by name error, error message : ${e.getMessage()}")
    }
}


/**
    Manage shared library

    @param Jenkins Jenkins instance
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_shared_library(Descriptor desc, Map data) {

    try {
        // Get current shared library by name
        def List<LibraryConfiguration> libraries = desc.getLibraries()
        def Integer current_library_id = get_library_id_by_name(libraries, data['name'])

        if (current_library_id == null) {
            if (data['state'] == 'present') {
                return add_shared_library(desc, data)
            }
            return false
        }

        if (data['state'] == 'absent') {
            libraries.remove(current_library_id)
            desc.setLibraries(libraries)
            return true
        }

        return update_shared_library(desc, libraries, current_library_id, data)
    }
    catch(Exception e) {
        throw new Exception(
            'Shared libraries management error, error message : ' + e.getMessage())
    }
}


/* SCRIPT */
def Boolean has_changed = false
try {
    def Jenkins jenkins_instance = Jenkins.getInstance()
    def Descriptor desc
    desc = jenkins_instance.getDescriptor(GlobalLibraries.class)
    data = parse_data(args[0])

    // Manage configuration with user data
    has_changed = manage_shared_library(desc, data)

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
    output has_changed
}

println result
