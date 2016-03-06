#!/usr/bin/env groovy

import jenkins.model.*
import jenkins.model.ProjectNamingStrategy
import jenkins.model.ProjectNamingStrategy.PatternProjectNamingStrategy
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
    Set the Jenkins disable remember me option

    @param Jenkins Jenkins singleton
    @param Boolean New value for disable remember me option
    @return Boolean True if changed, else false
*/
def Boolean set_disable_remember_me(Jenkins jenkins_instance,
                                    Boolean new_value) {

    // Get current value, used to check if changed
    def Boolean cur_value = jenkins_instance.isDisableRememberMe()
    if (cur_value == new_value) {
        return false
    }

    try {
        jenkins_instance.setDisableRememberMe(new_value)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during disable remember me change: '
            + e.getMessage())
    }

    return true
}


/**
    Set the Jenkins label

    @param Jenkins Jenkins singleton
    @param Boolean New Jenkins label
    @return Boolean True if changed, else false
*/
def Boolean set_label(Jenkins jenkins_instance, String label) {

    // Get current value, used to check if changed
    def String cur_value = jenkins_instance.getLabelString()
    if (cur_value == label) {
        return false
    }

    try {
        jenkins_instance.setLabelString(label)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during label change: ' + e.getMessage())
    }

    return true
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
        throw new Exception(
            'An error occurs during number of executor change: '
            + e.getMessage())
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
        throw new Exception(
            'An error occurs during mode change: ' + e.getMessage())
    }

    return true
}


/**
    Set the new Jenkins project naming strategy

    @param Jenkins Jenkins singleton
    @param String New project naming strategy regex
    @return Boolean True if changed, else false
*/
def Boolean set_project_naming_strategy(Jenkins jenkins_instance,
                                        Map strategy) {

    try {
        def PatternProjectNamingStrategy new_value
        new_value = new PatternProjectNamingStrategy(
                                                strategy.pattern,
                                                strategy.description,
                                                strategy.force)

        // Get current value, used to check if changed
        def ProjectNamingStrategy cur_value = jenkins_instance
                                               .getProjectNamingStrategy()

        if (cur_value instanceof PatternProjectNamingStrategy
          && cur_value.getNamePattern() == new_value.getNamePattern()
          && cur_value.getDescription() == new_value.getDescription()
          && cur_value.isForceExistingJobs() == new_value.isForceExistingJobs()
        ) {
            return false
        }

        jenkins_instance.setProjectNamingStrategy(new_value)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during project naming strategy change: '
            + e.getMessage())
    }

    return true
}


/**
    Set the Jenkins quiet period

    @param Jenkins Jenkins singleton
    @param Integer New quiet period
    @return Boolean True if changed, else false
*/
def Boolean set_quiet_period(Jenkins jenkins_instance, Integer quiet_period) {

    // Get current value, used to check if changed
    def Integer cur_value = jenkins_instance.getQuietPeriod()
    if (cur_value == quiet_period) {
        return false
    }

    try {
        jenkins_instance.setQuietPeriod(quiet_period)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during quiet period change: ' + e.getMessage())
    }

    return true
}


/**
    Set the Jenkins SCM checkout retry count

    @param Jenkins Jenkins singleton
    @param Integer SCM checkout retry count
    @return Boolean True if changed, else false
*/
def Boolean set_scm_checkout_retry_count(Jenkins jenkins_instance,
                                         Integer new_count) {

    // Get current value, used to check if changed
    def Integer cur_value = jenkins_instance.getScmCheckoutRetryCount()
    if (cur_value == new_count) {
        return false
    }

    try {
        jenkins_instance.setScmCheckoutRetryCount(new_count)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during scm checkout retry count change: '
            + e.getMessage())
    }

    return true
}


/**
    Set the Jenkins slave agent port

    @param Jenkins Jenkins singleton
    @param Integer New slave agent port
    @return Boolean True if changed, else false
*/
def Boolean set_slave_agent_port(Jenkins jenkins_instance, Integer port) {

    // Get current value, used to check if changed
    def Integer cur_value = jenkins_instance.getSlaveAgentPort()
    if (cur_value == port) {
        return false
    }

    try {
        jenkins_instance.setSlaveAgentPort(port)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during slave agent port change: '
            + e.getMessage())
    }

    return true
}


/* SCRIPT */
def List<Boolean> has_changed = []
try {
    def Jenkins jenkins_instance = Jenkins.getInstance()
    data = parse_data(args[0])

    // Manage configuration with user data
    has_changed.push(set_disable_remember_me(
                        jenkins_instance,
                        data['disable_remember_me']))
    has_changed.push(set_label(
                        jenkins_instance,
                        data['label']))
    has_changed.push(set_number_of_executors(
                        jenkins_instance,
                        data['number_of_executors']))
    has_changed.push(set_mode(
                        jenkins_instance,
                        data['mode']))
    has_changed.push(set_project_naming_strategy(
                        jenkins_instance,
                        data['project_naming_strategy']))
    has_changed.push(set_quiet_period(
                        jenkins_instance,
                        data['quiet_period']))
    has_changed.push(set_scm_checkout_retry_count(
                        jenkins_instance,
                        data['scm_checkout_retry_count']))
    has_changed.push(set_slave_agent_port(
                        jenkins_instance,
                        data['slave_agent_port']))

    // Save new configuration to disk
    jenkins_instance.save()
}
catch(Exception e) {
    throw new RuntimeException(e.getMessage())
}

// Build json result
result = new JsonBuilder()
result {
    changed has_changed.any()
    output has_changed
}

println result

