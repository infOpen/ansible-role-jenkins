# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|

  config.vm.box = "ubuntu/trusty64"

  # Network configuration
  config.vm.network "forwarded_port", guest: 8080, host: 8080


  # Ansible provisionning
  config.vm.provision "ansible" do |ansible|

    # Playbook used to test role
    ansible.playbook  = "tests/test_vagrant.yml"

  end

end

