#!/usr/bin/env groovy

import jenkins.model.*
import hudson.model.*
import groovy.json.*


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
    Set the Jenkins number of executors

    @param Jenkins Jenkins singleton
    @param Integer New number of executors
    @return Boolean True if changed, else false
*/
def Boolean set_number_of_executors(Jenkins jenkins_instance,
                                    Integer executors_number) {

    // Get current value, used to check if changed
    def Integer cur_value = jenkins_instance.getNumExecutors()
    if (cur_value == executors_number) {
        return false
    }

    try {
        jenkins_instance.setNumExecutors(executors_number)
    }
    catch(Exception e) {
        throw new Exception('An error occurs during number of executor change')
    }

    return true
}


/**
    Set the new Jenkins mode

    @param Jenkins Jenkins singleton
    @param String New mode
    @return Boolean True if changed, else false
*/
def Boolean set_mode(Jenkins jenkins_instance, String mode) {

    // Get current value, used to check if changed
    def Node.Mode cur_value = jenkins_instance.getMode()
    if (cur_value.getName() == mode) {
        return false
    }

    try {
        def Node.Mode new_mode = Node.Mode.valueOf(mode)
        jenkins_instance.setMode(new_mode)
    }
    catch(Exception e) {
        throw new Exception('An error occurs during mode change')
    }

    return true
}


/* SCRIPT */
def Boolean changed = false
def Map data = [:]

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()
    data = parse_data(args[0])

    // Manage configuration with user data
    set_number_of_executors(jenkins_instance, data['number_of_executors'])
    set_mode(jenkins_instance, data['mode'])
}
catch(Exception e) {
    throw new RuntimeException(e.getMessage())
}

def Map result = [ 'changed': changed, 'output': data ]

println JsonOutput.toJson(result.toString())

