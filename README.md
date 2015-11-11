jenkins
=======

[![Build Status](https://travis-ci.org/infOpen/ansible-role-jenkins.svg?branch=master)](https://travis-ci.org/infOpen/ansible-role-jenkins)

Install jenkins package.

Requirements
------------

This role requires Ansible 1.5 or higher, and platform requirements are listed
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
    jenkins_config_name  : "jenkins"
    jenkins_config_user  : "jenkins"
    jenkins_config_group : "jenkins"
    jenkins_config_run_standalone : True
    jenkins_config_max_open_files : 8192
    jenkins_config_umask          : "022"
    jenkins_config_http_port      : 8080
    jenkins_config_ajp_port       : -1
    jenkins_config_servlet_context_prefix : "/{{ jenkins_config_name }}"

    # Location and files configuration
    jenkins_config_java_location : "/usr/bin/java"
    jenkins_config_war_location  : >
      /usr/share/{{ jenkins_config_name }}/{{ jenkins_config_name }}.war
    jenkins_config_home_location : "/var/lib/{{ jenkins_config_name }}"
    jenkins_config_log_location  : >
      /var/log/{{ jenkins_config_name }}/{{ jenkins_config_name }}.log
    jenkins_config_pid_file      : >
      /var/run/{{ jenkins_config_name }}/{{ jenkins_config_name }}.pid

    # Java and jenkins arguments
    jenkins_config_java_args :
      - "-Djava.awt.headless=true"
    jenkins_config_args :
      - "--webroot=/var/cache/{{ jenkins_config_name }}/war"
      - "--httpPort={{ jenkins_config_http_port }}"
      - "--ajp13Port={{ jenkins_config_ajp_port }}"

    # Jenkins cli
    jenkins_base_url         : "http://localhost:{{ jenkins_config_http_port }}"
    jenkins_cli_download_url : "{{ jenkins_base_url }}/jnlpJars/jenkins-cli.jar"
    jenkins_cli : "{{ jenkins_config_home_location }}/jenkins-cli.jar"

    # Jenkins update center variables
    jenkins_update_center_url_download : >
      https://updates.jenkins-ci.org/update-center.json
    jenkins_update_center_url_post : >
      {{ jenkins_base_url }}/updateCenter/byId/default/postBack
    jenkins_update_file : "{{ jenkins_config_home_location }}/updates_jenkins.json"

    # Jenkins waiting availability test
    jenkins_waiting_available_retries : 10
    jenkins_waiting_available_delay   : 5

    # Plugins
    jenkins_plugins : []

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
- a.chaussier [at] infopen.pro } }}"

