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
                        String crumb_issuer,
                        Boolean crumb_ignore_client_ip) {

    // Get current strategy
    def CrumbIssuer cur_crumb_issuer = jenkins_instance.getCrumbIssuer()
    def String cur_crumb_issuer_class = jenkins_instance.getCrumbIssuer().getClass().getName()

    // Check if the current crumb issuer is needed crumb issuer
    if (crumb_issuer == '') {
        if (cur_crumb_issuer != null) {
            jenkins_instance.setCrumbIssuer(null)
            return true
        }
    }
    else if (crumb_issuer == 'hudson.security.csrf.DefaultCrumbIssuer') {
        if ((cur_crumb_issuer_class != crumb_issuer)
            || (cur_crumb_issuer.isExcludeClientIPFromCrumb() != crumb_ignore_client_ip)) {
            jenkins_instance.setCrumbIssuer(
              new hudson.security.csrf.DefaultCrumbIssuer(crumb_ignore_client_ip)
            )
            return true
        }
    }
    else {
        throw new Exception("Unmanaged crumb issuer : " + crumb_issuer)
    }

    return false
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
def Boolean user_changed = false
def Boolean auth_changed = false
def Boolean realm_changed = false
def data

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()

    // Get user data
    data = parse_data(args[0])

    // Manage security realm
    def current_realm = jenkins_instance.getSecurityRealm()
    def needed_realm = create_security_realm(data['security_realm'])

    // Check if realm has changed
    if (!are_same_security_realm(current_realm, needed_realm)) {
        jenkins_instance.setSecurityRealm(needed_realm)
        realm_changed = true
    }

    // Manage user account if instance use internal database
    if (data['security_realm']['realm_class'] == 'HudsonPrivateSecurityRealm') {
        user_changed = manage_user_account(needed_realm, data['user'])
        jenkins_instance.setSecurityRealm(needed_realm)
    }

    // Manage authorization strategy
    def strategy = manage_authorization_strategy(
                        jenkins_instance, data['authorization_strategy'])

    // Manage authorization
    auth_changed = manage_authorization(strategy, data['user'])
    jenkins_instance.setAuthorizationStrategy(strategy)

    // Manage crumb issuer
    crumb_changed = manage_crumb_issuer(
                        jenkins_instance,
                        data['crumb_issuer'],
                        data['crumb_exclude_client_ip'])

    // Save new configuration to disk
    jenkins_instance.save()
}
catch(Exception e) {
    throw new RuntimeException(e.getMessage())
}

// Build json result
def result = new JsonBuilder()
def has_changed = user_changed || realm_changed || auth_changed || crumb_changed
result {
    changed has_changed
    output data
}

println result

