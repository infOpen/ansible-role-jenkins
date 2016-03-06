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


### How configure credentials

This is an example of YAML structure to create credentials for Jenkins.


### Specific vars values for Debian family

    jenkins_repository_file_prefix : "/etc/apt/sources.list.d"
    jenkins_repository_file        : "pkg_jenkins_ci_org_debian.list"

    jenkins_default_cfg_prefix : "/etc/default"
    jenkins_default_cfg_file   : "jenkins"

    jenkins_package_name : "jenkins"
    jenkins_service_name : "jenkins"

    jenkins_system_dependencies :
      - python-httplib2

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

