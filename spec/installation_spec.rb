require 'serverspec'

if ENV['TRAVIS']
    set :backend, :exec
end

describe 'jenkins Ansible role' do

    if ['debian', 'ubuntu'].include?(os[:family])
        describe 'Specific Debian and Ubuntu family checks' do

            it 'install role packages' do
                packages = Array[ 'jenkins' ]

                packages.each do |pkg_name|
                    expect(package(pkg_name)).to be_installed
                end
            end
        end

        FILES_EXISTS = [
            '/etc/apt/sources.list.d/pkg_jenkins_ci_org_debian.list',
            '/etc/default/jenkins',
            '/var/lib/jenkins/jenkins-cli.jar',
            '/var/lib/jenkins//updates_jenkins.json',
            '/var/lib/jenkins/jenkins.model.ArtifactManagerConfiguration.xml',
            '/var/lib/jenkins/jenkins.mvn.GlobalMavenConfig.xml',
            '/var/lib/jenkins/jenkins.model.JenkinsLocationConfiguration.xml',
            '/var/lib/jenkins/hudson.tasks.Maven.xml',
            '/var/lib/jenkins/hudson.tasks.Shell.xml',
            '/var/lib/jenkins/org.jenkinsci.plugins.ansible.AnsibleInstallation.xml',
            '/var/lib/jenkins/hudson.plugins.ansicolor.AnsiColorBuildWrapper.xml',
            '/var/lib/jenkins/hudson.tasks.Ant.xml',
            '/var/lib/jenkins/hudson.scm.CVSSCM.xml',
            '/var/lib/jenkins/ru.yandex.jenkins.plugins.debuilder.DebianPackageBuilder.xml',
            '/var/lib/jenkins/ru.yandex.jenkins.plugins.debuilder.DebianPackagePublisher.xml',
            '/var/lib/jenkins/envInject.xml',
            '/var/lib/jenkins/envinject-plugin-configuration.xml',
            '/var/lib/jenkins/github-plugin-configuration.xml',
            '/var/lib/jenkins/hudson.plugins.git.GitSCM.xml',
            '/var/lib/jenkins/org.jenkinsci.plugins.gitclient.JGitTool.xml',
            '/var/lib/jenkins/org.jenkinsci.plugins.graphiteIntegrator.GraphitePublisher.xml',
            '/var/lib/jenkins/hudson.tasks.Mailer.xml',
            '/var/lib/jenkins/hudson.maven.MavenModuleSet.xml',
            '/var/lib/jenkins/jenkins.plugins.publish_over_ssh.BapSshPublisherPlugin.xml',
            '/var/lib/jenkins/org.jvnet.hudson.plugins.SSHBuildWrapper.xml',
            '/var/lib/jenkins/hudson.scm.SubversionSCM.xml'
        ]

        FILES_MANAGED_ANSIBLE = [
            '/etc/default/jenkins'
        ]

        FILES_EXISTS.each do |file_name|
            describe file(file_name) do
                it { should exist }
            end
        end

        FILES_MANAGED_ANSIBLE.each do |file_name|
            describe file(file_name) do
                it { should exist }
                its(:content) {
                    should match /Ansible managed(: Testing)?/
                }
            end
        end

    end

    describe port(8080) do
        it { should be_listening }
    end

end

