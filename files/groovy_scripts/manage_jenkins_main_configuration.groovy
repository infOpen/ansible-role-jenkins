#!/usr/bin/env groovy

import jenkins.model.*
import groovy.json.*


/* FUNCTIONS */
def parse_data(String arg) {

    try {
        def jsonSlurper = new JsonSlurper()
        return jsonSlurper.parseText(arg)
    }
    catch(e) {
        throw new Exception("Parse data error, incoming data : ${arg}, "
                            + "error message : ${e.getMessage()}")
    }
}


def set_number_of_executors(jenkins_instance, executors_number) {

    // Get current value, used to check if changed
    def cur_value = jenkins_instance.getNumExecutors()
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


/* SCRIPT */
def changed = false
def data = [:]

try {
    def jenkins_instance = Jenkins.getInstance()
    data = parse_data(args[0])

    // Manage configuration with user data
    set_number_of_executors(jenkins_instance, data['number_of_executors'])
}
catch(Exception e) {
    throw new RuntimeException(e.getMessage())
}

def result = [ 'changed': changed, 'output': data ]

println JsonOutput.toJson(result.toString())

