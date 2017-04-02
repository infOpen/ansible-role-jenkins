#!/usr/bin/env groovy

import groovy.json.*
import jenkins.model.*
import jenkins.model.IdStrategy.*
import jenkins.security.plugins.ldap.*
import hudson.model.*
import hudson.security.*
import hudson.security.csrf.CrumbIssuer
import hudson.security.LDAPSecurityRealm.CacheConfiguration
import hudson.security.LDAPSecurityRealm.EnvironmentProperty
import hudson.util.Secret


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
    Compare two ldap security realm to check if they have the same properties

    @param LDAPSecurityRealm Current jenkins security realm
    @param LDAPSecurityRealm Needed security realm
    @return Boolean True if the two security realms have the same properties
*/
def are_same_ldap_security_realm(LDAPSecurityRealm realm_a, LDAPSecurityRealm realm_b) {

    try {

        def List<Boolean> has_changed = []

        has_changed.push(realm_a.getLDAPURL() != realm_b.getLDAPURL())
        has_changed.push(
            realm_a.getDisplayNameAttributeName()
            != realm_b.getDisplayNameAttributeName())
        has_changed.push(
            realm_a.getMailAddressAttributeName()
            != realm_b.getMailAddressAttributeName())
        has_changed.push(realm_a.userSearchBase != realm_b.userSearchBase)
        has_changed.push(realm_a.userSearch != realm_b.userSearch)
        has_changed.push(realm_a.groupSearchBase != realm_b.groupSearchBase)
        has_changed.push(
            realm_a.getGroupSearchFilter() != realm_b.getGroupSearchFilter())
        has_changed.push(realm_a.managerDN != realm_b.managerDN)
        has_changed.push(
            realm_a.inhibitInferRootDN != realm_b.inhibitInferRootDN)
        has_changed.push(
            realm_a.disableMailAddressResolver
            != realm_b.disableMailAddressResolver)
        has_changed.push(realm_a.getCacheSize() != realm_b.getCacheSize())
        has_changed.push(realm_a.getCacheTTL() != realm_b.getCacheTTL())
        has_changed.push(
            realm_a.getUserIdStrategy() != realm_b.getUserIdStrategy())
        has_changed.push(
            realm_a.getGroupIdStrategy() != realm_b.getGroupIdStrategy())
        has_changed.push(!are_same_group_membership_strategies(
            realm_a.getGroupMembershipStrategy(),
            realm_b.getGroupMembershipStrategy()))
        has_changed.push(
            realm_a.getExtraEnvVars().toString()
            != realm_b.getExtraEnvVars().toString())
        has_changed.push(
            realm_a.getManagerPassword() != realm_b.getManagerPassword())

        return !has_changed.any()
    }
    catch(Exception e) {
        throw new RuntimeException(
            'Error during ldap security realm compare step, ' + e.getMessage())
    }
}


/**
    Compare two private security realm to check if they have the same properties

    @param HudsonPrivateSecurityRealm Current jenkins security realm
    @param HudsonPrivateSecurityRealm Needed security realm
    @return Boolean True if the two security realms have the same properties
*/
def are_same_private_security_realm(HudsonPrivateSecurityRealm realm_a,
                                    HudsonPrivateSecurityRealm realm_b) {

    try {

        def List<Boolean> has_changed = []

        has_changed.push(realm_a.getAllowsSignup() != realm_b.getAllowsSignup())
        has_changed.push(realm_a.isEnableCaptcha() != realm_b.isEnableCaptcha())

        return !has_changed.any()
    }
    catch(Exception e) {
        throw new RuntimeException(
            'Error during private security realm compare step, '
            + e.getMessage())
    }
}


/**
    Compare two security realm to check if they have the same properties

    @param SecurityRealm Current jenkins security realm
    @param SecurityRealm Needed security realm
    @return Boolean True if the two security realms have the same properties
*/
def are_same_security_realm(SecurityRealm realm_a, SecurityRealm realm_b) {

    try {
        if (realm_a.getClass().getName() != realm_b.getClass().getName()) {
            return false
        }

        if (realm_a instanceof HudsonPrivateSecurityRealm) {
            return are_same_private_security_realm(realm_a, realm_b)
        }

        if (realm_a instanceof LDAPSecurityRealm) {
            return are_same_ldap_security_realm(realm_a, realm_b)
        }

    }
    catch(Exception e) {
        throw new RuntimeException(
            'Error during security realm compare step, ' + e.getMessage())
    }
}


/**
    Compare two group membership strategies to check if they have the same properties

    @param LDAPGroupMembershipStrategy Current jenkins security realm
    @param LDAPGroupMembershipStrategy Needed security realm
    @return Boolean True if the two strategies have the same properties
*/
def are_same_group_membership_strategies(
                        LDAPGroupMembershipStrategy strategy_a,
                        LDAPGroupMembershipStrategy strategy_b) {

    try {
        if (strategy_a.getClass().getName() != strategy_b.getClass().getName()) {
            return false
        }

        if (strategy_a instanceof FromUserRecordLDAPGroupMembershipStrategy) {
            if (strategy_a.getAttributeName() == strategy_b.getAttributeName()) {
                return true
            }
        }

        if (strategy_a instanceof FromGroupSearchLDAPGroupMembershipStrategy) {
            if (strategy_a.getFilter() == strategy_b.getFilter()) {
                return true
            }
        }

        return false
    }
    catch(Exception e) {
        throw new RuntimeException(
            'Error during group membership strategies compare step, '
            + e.getMessage()
        )
    }
}


/**
    Create Jenkins LDAP group membership strategy

    @param String Group membership strategy class
    @param String Group membership strategy value
    @return LDAPGroupMembershipStrategy Group membership strategy
*/
def manage_group_membership_strategy(String strategy_class, String strategy_value) {

    try {
        if (strategy_class == 'FromGroupSearchLDAPGroupMembershipStrategy') {
            return new FromGroupSearchLDAPGroupMembershipStrategy(strategy_value)
        }
        else if (strategy_class == 'FromUserRecordLDAPGroupMembershipStrategy') {
            return new FromUserRecordLDAPGroupMembershipStrategy(strategy_value)
        }
        throw new RuntimeException(
            'Unmanaged LDAP group membership strategy class !')
    }
    catch(Exception e) {
        throw new RuntimeException(
            'Error during LDAP group membership strategy create step, '
            + e.getMessage())
    }
}


/**
    Create Jenkins ID strategy

    @param String ID membership strategy class
    @return IdStrategy Group membership strategy
*/
def manage_id_strategy(String strategy_class) {

    try {
        if (strategy_class == 'IdStrategy.CaseInsensitive') {
            return new IdStrategy.CaseInsensitive()
        }
        else if (strategy_class == 'IdStrategy.CaseSensitive') {
            return new IdStrategy.CaseSensitive()
        }
        else if (strategy_class == 'IdStrategy.CaseSensitiveEmailAddress') {
            return new IdStrategy.CaseSensitiveEmailAddress()
        }
        throw new RuntimeException('Unmanaged ID strategy class !')
    }
    catch(Exception e) {
        throw new RuntimeException(
            'Error during ID strategy create step, ' + e.getMessage())
    }
}


/**
    Create Jenkins LDAP environment properties

    @param ArrayList[] LDAP environment properties
    @return EnvironmentProperty[] Environement properties array
*/
def manage_environment_properties(ArrayList properties) {

    try {
        def EnvironmentProperty[] environment_properties = new EnvironmentProperty[properties.size()]
        def int i = 0;

        for (property in properties) {
            environment_properties[i++] = new EnvironmentProperty(property.name, property.value)
        }

        return environment_properties

    }
    catch(Exception e) {
        throw new RuntimeException(
            'Error during environment properties create step, ' + e.getMessage())
    }
}


/**
    Create Jenkins LDAP security realm

    @param Jenkins Current jenkins instance
    @return SecurityRealm Security realm managed
*/
def create_ldap_security_realm(Map realm_data) {

    try {
        def LDAPSecurityRealm.EnvironmentProperty[] environment_properties = manage_environment_properties(realm_data['environment_properties'])
        return new LDAPSecurityRealm(
            realm_data['server'],
            realm_data['root_dn'],
            realm_data['user_search_base'],
            realm_data['user_search'],
            realm_data['group_search_base'],
            realm_data['group_search_filter'],
            manage_group_membership_strategy(
                realm_data['group_membership_strategy_class'],
                realm_data['group_membership_strategy_value']
            ),
            realm_data['manager_dn'],
            Secret.fromString(realm_data['manager_password_secret']),
            realm_data['inhibit_infer_root_dn'],
            realm_data['disable_mail_address_resolver'],
            new CacheConfiguration(
                realm_data['cache_size'], realm_data['cache_ttl']),
            environment_properties,
            realm_data['display_name_attribute_name'],
            realm_data['mail_address_attribute_name'],
            manage_id_strategy(realm_data['user_id_strategy']),
            manage_id_strategy(realm_data['group_id_strategy'])
        )
    }
    catch(Exception e) {
        throw new RuntimeException(
            'Error during LDAP security realm create step, ' + e.getMessage())
    }
}


/**
    Create security realm based on user realm data

    @param Map User information about desired realm
    @return SecurityRealm Security realm managed
*/
def create_security_realm(Map realm_data) {

    if (realm_data['realm_class'] == 'HudsonPrivateSecurityRealm') {

        // Today capcha support parameter is not managed
        // TODO: Add capcha support parameter management
        return new HudsonPrivateSecurityRealm(
            realm_data['allow_signup'],
            realm_data['capcha_enabled'],
            null
        )
    }
    else if (realm_data['realm_class'] == 'LDAPSecurityRealm') {
        return create_ldap_security_realm(realm_data)
    }
}


/* SCRIPT */
def Boolean has_changed = false
def data

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()

    // Get user data
    data = parse_data(args[0])

    // Manage security realm
    def current_realm = jenkins_instance.getSecurityRealm()
    def needed_realm = create_security_realm(data)

    // Check if realm has changed
    if (!are_same_security_realm(current_realm, needed_realm)) {
        jenkins_instance.setSecurityRealm(needed_realm)
        jenkins_instance.save()
        has_changed = true
    }

}
catch(Exception e) {
    throw new RuntimeException(e.getMessage())
}

// Build json result
def result = new JsonBuilder()
result {
    changed has_changed
    output data
}

println result
