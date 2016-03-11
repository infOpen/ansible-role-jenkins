# jenkins

[![Build Status](https://travis-ci.org/infOpen/ansible-role-jenkins.svg?branch=master)](https://travis-ci.org/infOpen/ansible-role-jenkins)

Install jenkins package.

## Requirements

This role requires Ansible 2.0 or higher, and platform requirements are listed
in the metadata file.

## Testing

This role has two test methods :

- localy with Vagrant (need vagrant-triggers plugin installed) :
    vagrant up

- automaticaly by Travis

Vagrant should be used to check the role before push changes to Github.

## Role Variables

Follow the possible variables with their default values

### Defaults file for jenkins

    # Ubuntu repository vars
    jenkins_repository_key_url : "https://jenkins-ci.org/debian/jenkins-ci.org.key"
    jenkins_repository_content : "deb http://pkg.jenkins-ci.org/debian binary/"
    jenkins_package_state      : "latest"

    jenkins_system_dependencies : []
    jenkins_system_dependencies_state : "present"

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
      - "--httpListenAddress={{ jenkins_etc_listen_address }}"
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

    # Jenkins clouds
    jenkins_main_cfg_clouds : []


    # CONFIGURATION
    #--------------
    jenkins_configuration_files_owner : "{{ jenkins_etc_user }}"
    jenkins_configuration_files_group : "{{ jenkins_etc_group }}"
    jenkins_configuration_files_mode  : "0644"

    # Main configuration
    jenkins_config_disable_remember_me : False
    jenkins_config_label : ''
    jenkins_config_mode : 'NORMAL'
    jenkins_config_num_executors : 2
    jenkins_config_project_naming_strategy :
      pattern: '\w+'
      description: 'Alphanumeric pattern'
      force: True
    jenkins_config_quiet_period : 5
    jenkins_config_scm_checkout_retry_count : 0
    jenkins_config_slave_agent_port : 0

    # Jenkins users configuration
    jenkins_manage_users_and_security: True
    jenkins_deployment_user:
      username: 'ansible'
      full_name: 'Ansible deployment user'
      email: 'ansible@foo.bar'
      password: 'ansible'
      ssh:
        private: ''
        public: ''

    jenkins_users:
      - username: 'admin'
        full_name: 'Jenkins administrator user'
        email: 'ansible@foo.bar'
        password: 'admin'
        roles:
          - 'jenkins-administer'
        public_keys: []

    # Jenkins realm management
    jenkins_security_realm:
      class: 'HudsonPrivateSecurityRealm'

    # Jenkins authorization strategy management
    jenkins_authorization_strategy:
      class: 'GlobalMatrixAuthorizationStrategy'

    # Jenkins location settings
    jenkins_location_administrator_email: 'root@localhost'
    jenkins_location_administrator_full_name: 'Jenkins administrator'
    jenkins_location_url: 'http://jenkins.foo.bar/'


    # Credentials configuration
    #==========================
    jenkins_credentials_domains_to_empty: []
    jenkins_credentials: []

    # Plugins configuration
    #======================

    # Plugins management
    jenkins_plugins : []
    jenkins_plugins_state: 'latest'

    # Plugins : git
    jenkins_plugin_git_global_full_name  : "Jenkins GitUser"
    jenkins_plugin_git_global_email : "git@foo.bar"
    jenkins_plugin_git_create_account_based_on_email : False

    # Plugins : mailer
    jenkins_plugin_mailer_charset: 'UTF-8'
    jenkins_plugin_mailer_default_suffix: ''
    jenkins_plugin_mailer_reply_to: ''
    jenkins_plugin_mailer_smtp_host: ''
    jenkins_plugin_mailer_smtp_password: ''
    jenkins_plugin_mailer_smtp_port: 25
    jenkins_plugin_mailer_smtp_user: ''
    jenkins_plugin_mailer_use_ssl: False

    # Plugins : github
    jenkins_plugins_github_remove_servers: True
    jenkins_plugins_github_servers: []

    # Plugins: debian package builder
    jenkins_plugins_debian_package_builder_remove_repositories: True
    jenkins_plugins_debian_package_builder:
      gpg:
        name: 'Foo Bar'
        email: 'foo@bar.fr'
        public_key: 'foo_public_key'
        private_key: 'foo_private_key'
        passphrase: 'foo_passphrase'
      repo: []

    # Plugins : gitlab
    jenkins_plugins_gitlab_api_token: ''
    jenkins_plugins_gitlab_host_url: ''
    jenkins_plugins_gitlab_ignore_cert_error: False

### Specific vars values for Debian family

    jenkins_repository_file_prefix : "/etc/apt/sources.list.d"
    jenkins_repository_file        : "pkg_jenkins_ci_org_debian.list"

    jenkins_default_cfg_prefix : "/etc/default"
    jenkins_default_cfg_file   : "jenkins"

    jenkins_package_name : "jenkins"
    jenkins_service_name : "jenkins"

    jenkins_system_dependencies :
      - python-httplib2

## How configure ...

### Application accounts

Default settings create an administrator account, follows the structure to
manage your Jenkins users

Example :

    jenkins_users:
      - username: 'admin'
        full_name: 'Jenkins administrator user'
        email: 'admin@foo.bar'
        password: 'admin'
        roles:
          - 'jenkins-administer'
        public_keys: []
      - username: 'user'
        full_name: 'Jenkins read only user'
        email: 'user@foo.bar'
        password: 'user'
        roles:
          - 'jenkins-read'
        public_keys: []

### Credentials

We manage three credentials types :
  - SSH with passphrase
  - User/Password
  - String

This is an example of YAML structure to create these credentials for Jenkins.

    jenkins_credentials:
      - credentials_type: 'ssh_with_passphrase'
        credentials_domain: 'global'
        scope: 'global'
        id: 'ssh-jenkins-master-user'
        username: 'jenkins'
        private_key_source_type: 'user'
        private_key_source_data: 'foo'
        private_key_passphrase: 'bar'
        description: 'Jenkins user on local server'
        state: 'present'
      - credentials_type: 'password'
        credentials_domain: 'global'
        scope: 'global'
        id: 'simple-jenkins-user'
        username: 'jenkins'
        password: 'jenkins'
        description: 'Jenkins testing password credentials'
        state: 'present'
      - credentials_type: 'text'
        credentials_domain: 'global'
        scope: 'global'
        id: 'simple-text-credentials'
        text: 'loving_jenkins'
        description: 'Jenkins testing text credentials'
        state: 'present'

You can remove all credentials linked to a domain. Just set the domain list to
"jenkins_credentials_domains_to_empty".

Example:

    jenkins_credentials_domains_to_empty:
      - 'global'

### Debian Package Builder plugin

You can manage the plugin configuration with these settings, example with
repositories for publisher step:

    jenkins_plugins_debian_package_builder:
      gpg:
        name: 'Foo Bar'
        email: 'foo@bar.fr'
        public_key: 'foo_public_key'
        private_key: 'foo_private_key'
        passphrase: 'foo_passphrase'
      repo:
        - name: 'repo_1'
          method: 'scpb'
          fqdn: 'foo.bar.fr'
          incoming: '/foo'
          login: 'foo'
          key_path: '/bar/foo.key'
          options: 'foo_options'
          state: 'present'
        - name: 'repo_2'
          method: 'scpb'
          fqdn: 'foobar.bar.fr'
          incoming: '/foobar'
          login: 'foobar'
          key_path: '/bar/foobar.key'
          options: 'foobar_options'
          state: 'absent'

You can remove all repositories before plugin configuration. Just set True to
"jenkins_plugins_debian_package_builder_remove_repositories" variable.

### Github servers and credentials

You can manage Github plugin serveurs with this structure:

    jenkins_plugins_github_servers:
      - manage_hooks: False
        credentials_id: 'simple-text-credentials'
        custom_url: ''
        client_cache_size: 20

You can remove all servers before plugin configuration. Just set True to
"jenkins_plugins_github_remove_servers" variable.

## Dependencies

- achaussier.openjdk-jre
- achaussier.openjdk-jdk

## Example Playbook

    - hosts: servers
      roles:
         - { role: achaussier.jenkins }

## License

MIT

## Author Information

Alexandre Chaussier (for Infopen company)
- http://www.infopen.pro
- a.chaussier [at] infopen.pro

