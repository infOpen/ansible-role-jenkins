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

# Specific vars values for Debian family
    jenkins_repository_file_prefix : "/etc/apt/sources.list.d"
    jenkins_repository_file        : "pkg_jenkins_ci_org_debian.list"
    jenkins_repository_content     : "deb http://pkg.jenkins-ci.org/debian binary/"
    jenkins_package_name : "jenkins"

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

