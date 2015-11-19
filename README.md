jenkins
=======

[![Build Status](https://travis-ci.org/infOpen/ansible-role-jenkins.svg?branch=master)](https://travis-ci.org/infOpen/ansible-role-jenkins)

Install jenkins package.

Requirements
------------

This role requires Ansible 1.9 or higher, and platform requirements are listed
in the metadata file.

Testing
-------

This role has two test methods :

- localy with Vagrant :
    vagrant up

- automaticaly by Travis

Vagrant should be used to check the role before push changes to Github.

Role Variables
--------------

Follow the possible variables with their default values

# Defaults file for jenkins

    # Ubuntu repository vars
    jenkins_repository_key_url : "https://jenkins-ci.org/debian/jenkins-ci.org.key"
    jenkins_package_state      : "latest"
    jenkins_repository_content : "deb http://pkg.jenkins-ci.org/debian binary/"

    # Configuration file settings
    jenkins_default_cfg_file_owner : root
    jenkins_default_cfg_file_group : root
    jenkins_default_cfg_file_mode  : "0640"

    # Configuration file content variables
    jenkins_etc_name  : "jenkins"
    jenkins_etc_user  : "jenkins"
    jenkins_etc_group : "jenkins"
    jenkins_etc_run_standalone : True
    jenkins_etc_max_open_files : 8192
    jenkins_etc_umask          : "022"
    jenkins_etc_listen_address : 127.0.0.1
    jenkins_etc_http_port      : 8080
    jenkins_etc_ajp_port       : -1
    jenkins_etc_servlet_context_prefix : "/{{ jenkins_etc_name }}"

    # Location and files configuration
    jenkins_etc_java_location : "/usr/bin/java"
    jenkins_etc_war_location  : >
      /usr/share/{{ jenkins_etc_name }}/{{ jenkins_etc_name }}.war
    jenkins_etc_home_location : "/var/lib/{{ jenkins_etc_name }}"
    jenkins_etc_log_location  : >
      /var/log/{{ jenkins_etc_name }}/{{ jenkins_etc_name }}.log
    jenkins_etc_pid_file      : >
      /var/run/{{ jenkins_etc_name }}/{{ jenkins_etc_name }}.pid

    # Java and jenkins arguments
    jenkins_etc_java_args :
      - "-Djava.awt.headless=true"
    jenkins_etc_args :
      - "--webroot=/var/cache/{{ jenkins_etc_name }}/war"
      - "--httpPort={{ jenkins_etc_http_port }}"
      - "--ajp13Port={{ jenkins_etc_ajp_port }}"

    # Jenkins cli
    jenkins_base_url         : "http://localhost:{{ jenkins_etc_http_port }}"
    jenkins_cli_download_url : "{{ jenkins_base_url }}/jnlpJars/jenkins-cli.jar"
    jenkins_cli : "{{ jenkins_etc_home_location }}/jenkins-cli.jar"

    # Jenkins update center variables
    jenkins_update_center_url_download : >
      https://updates.jenkins-ci.org/update-center.json
    jenkins_update_center_url_post : >
      {{ jenkins_base_url }}/updateCenter/byId/default/postBack
    jenkins_update_file : "{{ jenkins_etc_home_location }}/updates_jenkins.json"

    # Jenkins waiting availability test
    jenkins_waiting_available_retries : 10
    jenkins_waiting_available_delay   : 5

    # Plugins
    jenkins_plugins : []

    # API URLs
    #---------
    jenkins_api_plugins_list : >
      pluginManager/api/json?depth=1&tree=plugins[shortName,version]

    # CONFIGURATION
    #--------------

    # Main configuration
    jenkins_main_cfg_version : ''
    jenkins_main_cfg_num_executors : 2
    jenkins_main_cfg_mode : 'NORMAL'
    jenkins_main_cfg_use_security : True
    jenkins_main_cfg_authorization_strategy : >
      hudson.security.AuthorizationStrategy$Unsecured
    jenkins_main_cfg_security_realm : 'hudson.security.SecurityRealm$None'
    jenkins_main_cfg_disable_remember_me : False
    jenkins_main_cfg_project_naming_strategy : >
      jenkins.model.ProjectNamingStrategy$DefaultProjectNamingStrategy
    jenkins_main_cfg_workspace_dir : '${ITEM_ROOTDIR}/workspace'
    jenkins_main_cfg_builds_dir : '${ITEM_ROOTDIR}/builds'
    jenkins_main_cfg_quiet_period : 5
    jenkins_main_cfg_scm_checkout_retry_count : 0
    jenkins_main_cfg_views : []
    jenkins_main_cfg_primary_view : All
    jenkins_main_cfg_slave_agent_port : 0
    jenkins_main_cfg_label : ''

    # Plugins templates
    jenkins_plugin_templates :
      - name     : ansible
        template : "org.jenkinsci.plugins.ansible.AnsibleInstallation.xml.j2"
        dest     : >
          {{ jenkins_etc_home_location -}}
          /org.jenkinsci.plugins.ansible.AnsibleInstallation.xml
      - name     : envinject
        template : "envInject.xml.j2"
        dest     : "{{ jenkins_etc_home_location }}/envInject.xml"
      - name     : envinject
        template : "envinject-plugin-configuration.xml.j2"
        dest     : >
          {{ jenkins_etc_home_location }}/envinject-plugin-configuration.xml
      - name     : github
        template : "github-plugin-configuration.xml.j2"
        dest     : >
          {{ jenkins_etc_home_location }}/github-plugin-configuration.xml
      - name     : git
        template : "hudson.plugins.git.GitSCM.xml.j2"
        dest     : >
          {{ jenkins_etc_home_location }}/hudson.plugins.git.GitSCM.xml
      - name     : git-client
        template : "org.jenkinsci.plugins.gitclient.JGitTool.xml.j2"
        dest     : >
          {{ jenkins_etc_home_location }}
          /org.jenkinsci.plugins.gitclient.JGitTool.xml }}

    # Plugins : ansible
    jenkins_plugin_cfg_ansible_installations : []

    # Plugin : envinject
    jenkins_plugin_cfg_envinject_global_password_entries : []
    jenkins_plugin_cfg_envinject_hide_injected_vars : False
    jenkins_plugin_cfg_envinject_enable_permissions : False

    # Plugins : github
    jenkins_plugin_cfg_github_configs : []

    # Plugins : git
    jenkins_plugin_cfg_git_generation   : 1
    jenkins_plugin_cfg_git_global_name  : foobar
    jenkins_plugin_cfg_git_global_email : "foo@foo.bar"
    jenkins_plugin_cfg_git_create_account_based_on_email : False

    # Plugins : git-client
    jenkins_plugin_cfg_git_client_installations : []

# Specific vars values for Debian family

    jenkins_repository_file_prefix : "/etc/apt/sources.list.d"
    jenkins_repository_file        : "pkg_jenkins_ci_org_debian.list"

    jenkins_default_cfg_prefix : "/etc/default"
    jenkins_default_cfg_file   : "jenkins"

    jenkins_package_name : "jenkins"
    jenkins_service_name : "jenkins"

Dependencies
------------

None

Example Playbook
----------------

    - hosts: servers
      roles:
         - { role: achaussier.jenkins }

License
-------

MIT

Author Information
------------------

Alexandre Chaussier (for Infopen company)
- http://www.infopen.pro
- a.chaussier [at] infopen.pro

