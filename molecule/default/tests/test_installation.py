"""
Role tests
"""

import os
import pytest

from testinfra.utils.ansible_runner import AnsibleRunner

testinfra_hosts = AnsibleRunner(
    os.environ['MOLECULE_INVENTORY_FILE']).get_hosts('all')


def test_jenkins_user(host):
    """
    Tests Jenkins user
    """

    user = host.user('jenkins')

    assert user.exists
    assert user.group == 'jenkins'
    assert user.home == '/var/lib/jenkins'
    assert user.shell == '/bin/bash'


def test_jenkins_user_home(host):
    """
    Tests Jenkins user home properties
    """

    home_dir = host.file('/var/lib/jenkins')

    assert home_dir.exists
    assert home_dir.is_directory
    assert home_dir.mode == 0o755
    assert home_dir.user == 'jenkins'
    assert home_dir.group == 'jenkins'


def test_jenkins_packages(host):
    """
    Tests Jenkins package installation
    """

    assert host.package('jenkins').is_installed


@pytest.mark.parametrize('path', [
    ('/etc/apt/sources.list.d/pkg_jenkins_io_debian.list'),
    ('/etc/default/jenkins'),
    ('/var/lib/jenkins/jenkins-cli.jar'),
])
def test_jenkins_install_files(host, path):
    """
    Tests Jenkins installation files
    """

    current_file = host.file(path)

    assert current_file.exists
    assert current_file.is_file


@pytest.mark.parametrize('path,user,group,mode', [
    ('/var/lib/jenkins/.ssh/id_rsa', 'jenkins', 'jenkins', 0o600),
    ('/var/lib/jenkins/.ssh/id_rsa.pub', 'jenkins', 'jenkins', 0o644),
])
def test_jenkins_user_ssh_key(host, path, user, group, mode):
    """
    Tests Jenkins user SSH key files
    """

    current_file = host.file(path)

    assert current_file.exists
    assert current_file.is_file
    assert current_file.user == user
    assert current_file.group == group
    assert current_file.mode == mode


def test_jenkins_service_state(host):
    """
    Test Jenkins service state
    """

    assert host.service('jenkins').is_enabled
    assert host.service('jenkins').is_running


def test_listening_port(host):
    """
    Test process is running
    """

    assert host.socket('tcp://:::8080').is_listening or \
        host.socket('tcp://0.0.0.0:8080').is_listening
