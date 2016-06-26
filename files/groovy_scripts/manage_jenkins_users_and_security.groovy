#!/usr/bin/env groovy

import groovy.json.*
import jenkins.model.*
import hudson.model.*
import hudson.security.*


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
    Check if jenkins user account exists

    @param String Username to check
    @return User The needed jenkins user
    @nullable
*/
def is_user_exists(String username) {

    def Map context = [:]
    def user = User.get(username, false, context)

    return user != null
}


/**
    Check if jenkins user email need to be updated

    @param User User object with current data
    @param String User email
    @return Boolean True if email is different
*/
def is_email_need_update(User user, String email) {

    def current_email = null
    def property = user.getProperty(hudson.tasks.Mailer.UserProperty)

    if (property != null) {
        current_email = property.getAddress()
    }

    return current_email != email
}


/**
    Check if jenkins user keys need to be updated

    @param User User object with current data
    @param List User keys
    @return Boolean True if keys are different
*/
def is_keys_need_update(User user, List keys) {

    def current_keys = null
    def property = user.getProperty(
                    org.jenkinsci.main.modules.cli.auth.ssh.UserPropertyImpl)

    if (property != null) {
        // Remove empty strings
        current_keys = property.authorizedKeys.split('\\\\\\\\s+') - ""
    }

    return ! current_keys.equals(keys)
}


/**
    Check if jenkins user full name need to be updated

    @param User User object with current data
    @param String User full name
    @return Boolean True if full name is different
*/
def is_full_name_need_update(User user, String full_name) {

    def current_full_name = user.getFullName()

    return current_full_name != full_name
}


/**
    Set jenkins user email

    @param User User object
    @param String Email to set
    @return null
*/
def set_user_email(User user, String new_email) {

    def email = new hudson.tasks.Mailer.UserProperty(new_email)

    user.addProperty(email)
}


/**
    Set jenkins user keys

    @param User User object
    @param List Keys to set
    @return null
*/
def set_user_keys(User user, List new_keys) {

    def keys = new org.jenkinsci.main.modules.cli.auth.ssh.UserPropertyImpl(
                        new_keys.join('\n'))

    user.addProperty(keys)
}


/**
    Set jenkins user full name

    @param User User object
    @param String Full name to set
    @return null
*/
def set_user_full_name(User user, String new_full_name) {

    user.setFullName(new_full_name)
}


/**
    Create user account

    @param HudsonPrivateSecurityRealm Users realm
    @param Map Data of user to be created
    @return Boolean True if user created
*/
def create_user_account(HudsonPrivateSecurityRealm realm, Map data) {

    // Create user account
    def User user = realm.createAccount(data.username, data.password)

    // Set additional informations to user
    set_user_email(user, data.email)
    set_user_keys(user, data.public_keys)
    set_user_full_name(user, data.full_name)

    user.save()
    return true
}


/**
    Update user account

    @param HudsonPrivateSecurityRealm Users realm
    @param Map Data of user to be updated
    @return Boolean True if user updated
*/
def update_user_account(HudsonPrivateSecurityRealm realm, Map data) {

    // Get user from realm
    def user = realm.getUser(data.username)
    def Boolean changed = false

    // If needed, set additional informations to user
    if (is_email_need_update(user, data.email)) {
        set_user_email(user, data.email)
        changed = true
    }
    if (is_keys_need_update(user, data.public_keys)) {
        set_user_keys(user, data.public_keys)
        changed = true
    }
    if (is_full_name_need_update(user, data.full_name)) {
        set_user_full_name(user, data.full_name)
        changed = true
    }

    user.save()
    return changed
}


/**
    Manage user accounts

    @param HudsonPrivateSecurityRealm Users realm
    @param List User list to be created
    @return Boolean True if users created
*/
def manage_user_account(HudsonPrivateSecurityRealm realm, Map user) {
    def Boolean changed = false

    //if (is_user_exists(realm, user.username)) {
    if (is_user_exists(user.username)) {
        def user_changed = update_user_account(realm, user)
        changed = changed || user_changed
    }
    else {
        create_user_account(realm, user)
        changed = true
    }

    return changed
}


/**
    Manage authorization for user accounts

    @param GlobalMatrixAuthorizationStrategy Glocal security strategy
    @param User User to be managed
    @return Boolean True if users created
*/
def manage_authorization(GlobalMatrixAuthorizationStrategy strategy,
                         Map user) {
    def Boolean changed = false

    for (role in user.roles) {
        def Permission permission

        switch (role) {
            case "jenkins-administer":
                permission = Jenkins.ADMINISTER
                break
            case "jenkins-read":
                permission = Jenkins.READ
                break
            case "item-build":
                permission = Item.BUILD
                break
            case "item-discover":
                permission = Item.DISCOVER
                break
            case "item-read":
                permission = Item.READ
                break
            case "item-workspace":
                permission = Item.WORKSPACE
                break
            case "view-read":
                permission = View.READ
                break
        }

        if (! strategy.hasPermission(user.username, permission)) {
            changed = true
        }
        strategy.add(permission, user.username)
    }

    return changed
}


/**
    Manage jenkins realm

    @param Jenkins Current jenkins instance
    @return SecurityRealm Security realm managed
*/
def manage_security_realm(Jenkins jenkins_instance, Map needed_realm) {

    // Get current realm
    def cur_realm = jenkins_instance.getSecurityRealm()

    // Check if the current realm is needed realm
    if (needed_realm['class'] == 'HudsonPrivateSecurityRealm') {
        if (cur_realm instanceof HudsonPrivateSecurityRealm) {
            return cur_realm
        }
        return new HudsonPrivateSecurityRealm(false)
    }
}


/**
    Manage authorization strategy

    @param Jenkins Current jenkins instance
    @return AuthorizationStrategy Authorization strategy managed
*/
def manage_authorization_strategy(Jenkins jenkins_instance,
                                  Map needed_strategy) {

    // Get current strategy
    def cur_strategy = jenkins_instance.getAuthorizationStrategy()

    // Check if the current strategy is needed strategy
    if (needed_strategy['class'] == 'GlobalMatrixAuthorizationStrategy') {
        if (cur_strategy instanceof GlobalMatrixAuthorizationStrategy) {
            return cur_strategy
        }
        return new GlobalMatrixAuthorizationStrategy()
    }
}


/**
    Manage crumb issuer

    @param Jenkins Current jenkins instance
    @return Boolean True if crumb issuer changed
*/
def manage_crumb_issuer(Jenkins jenkins_instance,
                        String needed_crumb_issuer) {

    // Get current strategy
    def cur_crumb_issuer = jenkins_instance.getCrumbIssuer()

    // Check if the current crumb issuer is needed crumb issuer
    // Only null is managed today
    if (needed_crumb_issuer == '') {
        if (cur_crumb_issuer != null) {
            jenkins_instance.setCrumbIssuer(null)
            return true
        }
    }
}


/* SCRIPT */
def Boolean user_changed = false
def Boolean auth_changed = false
def data

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()

    // Get user data
    data = parse_data(args[0])

    // Manage security realm
    def realm = manage_security_realm(jenkins_instance, data['security_realm'])

    // Manage user account
    user_changed = manage_user_account(realm, data['user'])
    jenkins_instance.setSecurityRealm(realm)

    // Manage authorization strategy
    def strategy = manage_authorization_strategy(
                        jenkins_instance, data['authorization_strategy'])

    // Manage authorization
    auth_changed = manage_authorization(strategy, data['user'])
    jenkins_instance.setAuthorizationStrategy(strategy)

    // Manage crumb issuer
    crumb_changed = manage_crumb_issuer(jenkins_instance, data['crumb_issuer'])

    // Save new configuration to disk
    jenkins_instance.save()
}
catch(Exception e) {
    throw new RuntimeException(e.getMessage())
}

// Build json result
def result = new JsonBuilder()
def has_changed = user_changed || auth_changed || crumb_changed
result {
    changed has_changed
    output data
}

println result

