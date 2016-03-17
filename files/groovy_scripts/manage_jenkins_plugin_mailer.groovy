#!/usr/bin/env groovy

import jenkins.model.*
import jenkins.model.ProjectNamingStrategy
import jenkins.model.ProjectNamingStrategy.PatternProjectNamingStrategy \
    as PatternProjectNaming
import hudson.model.*
import hudson.util.Secret
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
    Check if one string is empty or null

    @param String String to test
    @return Boolean True if this value is empty or null
*/
def Boolean is_empty_or_null(String value) {

    return ((value == '') || (value == null))
}


/**
    Check if two strings are empty or null

    @param String First string to test
    @param String second string to test
    @return Boolean True if these two values are empty or null
*/
def Boolean are_empty_or_null(String value_a, String value_b) {

    def Boolean is_empty_value_a = is_empty_or_null(value_a)
    def Boolean is_empty_value_b = is_empty_or_null(value_b)

    return is_empty_value_a && is_empty_value_b
}


/**
    Set the Mailer plugin authentication

    @param Descriptor Mailer plugin descriptor
    @param String New value for mailer plugin smtp user
    @param String New value for mailer plugin smtp password
    @return Boolean True if changed, else false
*/
def Boolean set_mailer_plugin_smtp_auth(Descriptor desc,
                                        String new_smtp_user,
                                        String new_smtp_password) {

    // Get current value, used to check if changed
    def String cur_smtp_user = desc.getSmtpAuthUserName()
    def String cur_smtp_password = desc.getSmtpAuthPassword()

    def Boolean is_same_user = (
                    cur_smtp_user == new_smtp_user
                    || are_empty_or_null(new_smtp_user, cur_smtp_user))

    def Boolean is_same_password = (
                    cur_smtp_password == new_smtp_password
                    || are_empty_or_null(new_smtp_password, cur_smtp_password))

    if (is_same_user && is_same_password) {
        return false
    }

    try {
        desc.setSmtpAuth(new_smtp_user, new_smtp_password)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during mailer plugin authenticaton change')
    }

    return true
}


/**
    Set the Mailer plugin smtp host

    @param Descriptor Mailer plugin descriptor
    @param String New value for mailer plugin smtp host
    @return Boolean True if changed, else false
*/
def Boolean set_mailer_plugin_smtp_host(Descriptor desc,
                                        String new_smtp_host) {

    // Get current value, used to check if changed
    def String cur_smtp_host = desc.getSmtpServer()

    if (cur_smtp_host == new_smtp_host) {
        return false
    }

    try {
        desc.setSmtpHost(new_smtp_host)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during mailer plugin smtp host change')
    }

    return true
}


/**
    Set the Mailer plugin smtp port

    @param Descriptor Mailer plugin descriptor
    @param Integer New value for mailer plugin smtp port
    @return Boolean True if changed, else false
*/
def Boolean set_mailer_plugin_smtp_port(Descriptor desc,
                                        String new_smtp_port) {

    // Get current value, used to check if changed
    def String cur_smtp_port = desc.getSmtpPort()

    if (cur_smtp_port == new_smtp_port) {
        return false
    }

    try {
        //desc.setSmtpPort(String.format("%d", new_smtp_port))
        desc.setSmtpPort(new_smtp_port.toString())
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during mailer plugin smtp port change')
    }

    return true
}


/**
    Set the Mailer plugin charset

    @param Descriptor Mailer plugin descriptor
    @param String New value for mailer plugin charset
    @return Boolean True if changed, else false
*/
def Boolean set_mailer_plugin_charset(Descriptor desc, String new_charset) {

    // Get current value, used to check if changed
    def String cur_charset = desc.getCharset()

    if (cur_charset == new_charset) {
        return false
    }

    try {
        desc.setCharset(new_charset)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during mailer plugin charset change')
    }

    return true
}


/**
    Set the Mailer plugin default suffix

    @param Descriptor Mailer plugin descriptor
    @param String New value for mailer plugin default suffix
    @return Boolean True if changed, else false
*/
def Boolean set_mailer_plugin_default_suffix(Descriptor desc,
                                             String new_suffix) {

    // Get current value, used to check if changed
    def String cur_suffix = desc.getDefaultSuffix()
    def Boolean is_same_suffix = (cur_suffix == new_suffix)

    if (is_same_suffix || are_empty_or_null(new_suffix, cur_suffix)) {
        return false
    }

    try {
        desc.setDefaultSuffix(new_suffix)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during mailer plugin default suffix change')
    }

    return true
}


/**
    Set the Mailer plugin reply to address

    @param Descriptor Mailer plugin descriptor
    @param String New value for mailer plugin reply to address
    @return Boolean True if changed, else false
*/
def Boolean set_mailer_plugin_reply_to(Descriptor desc, String new_reply_to) {

    // Get current value, used to check if changed
    def String cur_reply_to = desc.getReplyToAddress()
    def Boolean is_same_address = (cur_reply_to == new_reply_to)

    if (is_same_address || are_empty_or_null(new_reply_to, cur_reply_to)) {
        return false
    }

    try {
        desc.setReplyToAddress(new_reply_to)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during mailer plugin reply to address change')
    }

    return true
}


/**
    Set the Mailer plugin SSL usage

    @param Descriptor Mailer plugin descriptor
    @param Boolean New value for mailer plugin SSL usage
    @return Boolean True if changed, else false
*/
def Boolean set_mailer_plugin_use_ssl(Descriptor desc, Boolean new_use_ssl) {

    // Get current value, used to check if changed
    def Boolean cur_use_ssl = desc.getUseSsl()

    if (cur_use_ssl == new_use_ssl) {
        return false
    }

    try {
        desc.setUseSsl(new_use_ssl)
    }
    catch(Exception e) {
        throw new Exception(
            'An error occurs during mailer plugin SSL usage change')
    }

    return true
}


/* SCRIPT */

def List <Boolean> has_changed = []
def String new_charset = ""
def String new_default_suffix = ""
def String new_reply_to = ""
def String new_smtp_host = ""
def String new_smtp_password = ""
def String new_smtp_port = '25'
def String new_smtp_user = ""
def Boolean new_use_ssl = false

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()
    def Descriptor desc
    desc = jenkins_instance.getDescriptor('hudson.tasks.Mailer')

    // Get user data
    data = parse_data(args[0])

    // Get arguments data
    new_charset = data['charset']
    new_default_suffix = data['default_suffix']
    new_reply_to = data['reply_to']
    new_smtp_host = data['smtp_host']
    new_smtp_password = data['smtp_password']
    new_smtp_port = data['smtp_port'].toString()
    new_smtp_user = data['smtp_user']
    new_use_ssl = data['use_ssl']

    // Manage configuration with user data
    has_changed.push(set_mailer_plugin_smtp_auth(desc, new_smtp_user,
                                                 new_smtp_password))
    has_changed.push(set_mailer_plugin_default_suffix(desc,
                                                      new_default_suffix))
    has_changed.push(set_mailer_plugin_smtp_host(desc, new_smtp_host))
    has_changed.push(set_mailer_plugin_smtp_port(desc, new_smtp_port))
    has_changed.push(set_mailer_plugin_reply_to(desc, new_reply_to))
    has_changed.push(set_mailer_plugin_use_ssl(desc, new_use_ssl))
    has_changed.push(set_mailer_plugin_charset(desc, new_charset))

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
        charset new_charset
        reply_to new_reply_to
        smtp_host new_smtp_host
        smtp_password new_smtp_password
        smtp_port new_smtp_port
        smtp_user new_smtp_user
        use_ssl new_use_ssl
    }
}

println result

