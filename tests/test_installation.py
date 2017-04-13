"""
Role tests
"""

from testinfra.utils.ansible_runner import AnsibleRunner

testinfra_hosts = AnsibleRunner('.molecule/ansible_inventory').get_hosts('all')


def test_jenkins_user(User):
    """
    Tests Jenkins user
    """

    user = User('jenkins')

    assert user.exists
    assert user.group == 'jenkins'
    assert user.home == '/var/lib/jenkins'
    assert user.shell == '/bin/bash'


def test_jenkins_user_home(File):
    """
    Tests Jenkins user home properties
    """

    home_dir = File('/var/lib/jenkins')

    assert home_dir.exists
    assert home_dir.is_directory
    assert home_dir.mode == 0o755
    assert home_dir.user == 'jenkins'
    assert home_dir.group == 'jenkins'


def test_jenkins_packages(Package):
    """
    Tests Jenkins package installation
    """

    assert Package('jenkins').is_installed


def test_jenkins_install_files(File):
    """
    Tests Jenkins installation files
    """

    files = [
      '/etc/apt/sources.list.d/pkg_jenkins_io_debian.list',
      '/etc/default/jenkins',
      '/var/lib/jenkins/jenkins-cli.jar',
    ]

    for current_file in files:
        assert File(current_file).exists
        assert File(current_file).is_file


def test_jenkins_user_ssh_key(File):
    """
    Tests Jenkins user SSH key files
    """

    private_key = File('/var/lib/jenkins/.ssh/id_rsa')
    public_key = File('/var/lib/jenkins/.ssh/id_rsa.pub')

    # Private key testing
    assert private_key.exists
    assert private_key.is_file
    assert private_key.user == 'jenkins'
    assert private_key.group == 'jenkins'
    assert private_key.mode == 0o600

    # public key testing
    assert public_key.exists
    assert public_key.is_file
    assert public_key.user == 'jenkins'
    assert public_key.group == 'jenkins'
    assert public_key.mode == 0o644


def test_jenkins_service_state(Command, Service, SystemInfo):
    """
    Test Jenkins service state
    """

    assert Service('jenkins').is_enabled
    assert 'is running' in Command.check_output('service jenkins status')


def test_listening_port(Socket):
    """
    Test process is running
    """

    assert Socket('tcp://:::8080').is_listening or \
        Socket('tcp://0.0.0.0:8080').is_listening
