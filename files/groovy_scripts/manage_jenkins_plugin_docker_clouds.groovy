#!/usr/bin/env groovy

import com.nirima.jenkins.plugins.docker.DockerCloud
import com.nirima.jenkins.plugins.docker.DockerImagePullStrategy
import com.nirima.jenkins.plugins.docker.DockerTemplate
import com.nirima.jenkins.plugins.docker.DockerTemplateBase
import com.nirima.jenkins.plugins.docker.launcher.DockerComputerLauncher
import com.nirima.jenkins.plugins.docker.launcher.DockerComputerSSHLauncher
import com.nirima.jenkins.plugins.docker.strategy.DockerOnceRetentionStrategy
import groovy.json.*
import hudson.model.*
import hudson.plugins.sshslaves.SSHConnector
import hudson.slaves.Cloud
import hudson.slaves.RetentionStrategy
import jenkins.model.*


/**
    Convert Json string to Groovy Object

    @param String arg Json string to parse
    @return Map Groovy object used to get data
*/
def Map parse_data(String arg) {

    try {
        def JsonSlurper jsonSlurper = new JsonSlurper()
        return jsonSlurper.parseText(arg)
    }
    catch(Exception e) {
        throw new Exception("Parse data error, incoming data : ${arg}, "
                            + "error message : ${e.getMessage()}")
    }
}


/**
    Create new template base

    @param List Needed configuration
    @return DockerTemplateBase New docker template base
*/
def DockerTemplateBase create_template_base(Map data) {

    try {

        def DockerTemplateBase template_base

        template_base = new DockerTemplateBase(
                                data['image'],
                                data['dns_hosts'].join(' '),
                                data['docker_command'],
                                data['volumes'].join("\n"),
                                data['volumes_from'].join("\n"),
                                data['environments'].join("\n"),
                                data['lxc_conf_string'],
                                data['hostname'],
                                data['memory_limit'],
                                data['memory_swap'],
                                data['cpu_shares'],
                                data['bind_ports'].join(' '),
                                data['bind_all_ports'],
                                data['privileged'],
                                data['tty'],
                                data['mac_address'])

        return template_base
    }
    catch(Exception e) {
        throw new Exception(
            'Manage docker template base create error, error message : '
            + e.getMessage())
    }
}


/**
    Get docker pull strategy constant

    @param String Needed configuration
    @return DockerComputerLauncher New docker computer launcher
*/
def DockerImagePullStrategy get_pull_strategy(String strategy_name) {

    try {

        def DockerImagePullStrategy pull_strategy
        pull_strategy = Enum.valueOf(DockerImagePullStrategy, strategy_name)

        return pull_strategy
    }
    catch(Exception e) {
        throw new Exception(
            'Manage docker pull strategy get error, error message : '
            + e.getMessage())
    }
}


/**
    Create new SSH launcher

    @param Map Needed configuration
    @return DockerComputerSSHLauncher New docker SSH launcher
*/
def DockerComputerSSHLauncher create_ssh_launcher(Map data) {

    try {

        def SSHConnector ssh_connector = new SSHConnector (
                                                data['port'],
                                                data['credentials_id'],
                                                data['jvm_options'].join(' '),
                                                data['java_path'],
                                                data['prefix_start_slave_cmd'],
                                                data['suffix_start_slave_cmd'],
                                                data['launch_timeout'],
                                                data['max_num_retries'],
                                                data['retry_wait_time'])

        def DockerComputerSSHLauncher ssh_launcher
        ssh_launcher = new DockerComputerSSHLauncher(ssh_connector)
        return ssh_launcher
    }
    catch(Exception e) {
        throw new Exception(
            'SSH docker launcher create error, error message : '
            + e.getMessage())
    }
}


/**
    Create new Docker launcher

    @param Map Needed configuration
    @return DockerComputerSSHLauncher New docker SSH launcher
*/
def DockerComputerLauncher create_launcher(Map data) {

    try {

        def DockerComputerLauncher launcher

        switch (data['class']) {

            case 'ssh':
                launcher = create_ssh_launcher(data)
                break
            default:
                throw new Exception('Launcher type not managed')
        }

        return launcher
    }
    catch(Exception e) {
        throw new Exception(
            'Docker launcher create error, error message : ' + e.getMessage())
    }
}


/**
    Create new Docker retention strategy

    @param Map Needed configuration
    @return RetentionStrategy New retention strategy
*/
def RetentionStrategy create_ret_strategy(Map data) {

    try {

        def RetentionStrategy ret_strategy

        switch (data['class']) {

            case 'once':
                ret_strategy = new DockerOnceRetentionStrategy(
                                                        data['idle_minutes'])
                break
            default:
                throw new Exception('Retention strategy type not managed')
        }

        return ret_strategy
    }
    catch(Exception e) {
        throw new Exception(
            'Docker retention strategy create error, error message : '
            + e.getMessage())
    }
}


/**
    Create new template

    @param Map Needed configuration
    @return DockerTemplate New docker template
*/
def DockerTemplate create_template(Map data) {

    try {

        // Create base template
        def DockerTemplateBase template_base
        template_base = create_template_base(data['template_base'])

        // Create launcher
        def DockerComputerLauncher launcher = create_launcher(data['launcher'])

        // Create retention strategy
        def RetentionStrategy ret_strategy
        ret_strategy = create_ret_strategy(data['retention_strategy'])

        // Create pull strategy
        def DockerImagePullStrategy pull_strategy
        pull_strategy = get_pull_strategy(data['pull_strategy'])

        // Create template
        def DockerTemplate tpl
        tpl = new DockerTemplate(
                        template_base,
                        data['label_string'],
                        data['remote_fs'],
                        data['remote_fs_mapping'],
                        data['instance_cap'].toString())

        // Additional settings
        tpl.setNumExecutors(data['num_executors'])
        tpl.setMode(Enum.valueOf(Node.Mode, data['mode']))
        tpl.setLauncher(launcher)
        tpl.setPullStrategy(pull_strategy)
        tpl.setRetentionStrategy(ret_strategy)
        tpl.setRemoveVolumes(data['remove_volumes'])

        return tpl
    }
    catch(Exception e) {
        throw new Exception(
            'Manage docker template create error, error message : '
            + e.getMessage())
    }
}


/**
    Create new template list

    @param List Needed configuration
    @return List<DockerTemplate> New docker template list
*/
def List<DockerTemplate> create_templates(List<Object> data) {

    try {

        def List<DockerTemplate> templates = []

        for (def tpl_data : data) {
            def DockerTemplate tpl = create_template(tpl_data)
            templates.add(tpl)
        }

        return templates
    }
    catch(Exception e) {
        throw new Exception(
            'Manage docker template list create error, error message : '
            + e.getMessage())
    }
}


/**
    Create new docker cloud

    @param Map Needed configuration
    @return DockerCloud New docker cloud configuration
*/
def DockerCloud create_cloud(Map data) {

    try {

        def List<? extends DockerTemplate> templates
        templates = create_templates(data['templates'])

        def DockerCloud cloud = new DockerCloud(
                                        data['name'],
                                        templates,
                                        data['server_url'],
                                        data['container_cap'],
                                        data['connect_timeout'],
                                        data['read_timeout'],
                                        data['credentials_id'],
                                        data['version'])

        return cloud
    }
    catch(Exception e) {
        throw new Exception(
            'Manage docker cloud create error, error message : '
            + e.getMessage())
    }
}


/**
    Add new docker cloud

    @param Jenkins Jenkins instance
    @param Map Needed configuration
    @return Boolean True if cloud added
*/
def Boolean add_cloud(Jenkins jenkins_instance, Map data) {

    try {

        def DockerCloud cloud = create_cloud(data)

        jenkins_instance.clouds.add(cloud)
        return true
    }
    catch(Exception e) {
        throw new Exception(
            'Adding docker cloud error, error message : '
            + e.getMessage())
    }
}


/**
    Manage docker cloud configuration update

    @param Jenkins Jenkins instance
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean update_cloud(Jenkins jenkins_instance,
                                DockerCloud cloud,
                                Map data) {

    try {

        def List<Boolean> has_changed = []
        def DockerCloud new_cloud = create_cloud(data)

        // Check if something changes
        if (cloud.equals(new_cloud)) {
            return false
        }
        jenkins_instance.clouds.replace(cloud, new_cloud)
        return true
    }
    catch(Exception e) {
        throw new Exception(
            'Manage docker cloud update error, error message : '
            + e.getMessage())
    }
}


/**
    Manage docker cloud

    @param Jenkins Jenkins instance
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_docker_cloud(Jenkins jenkins_instance, Map data) {

    try {
        // Get current cloud by name
        def Cloud cloud = jenkins_instance.getCloud(data['name'])

        if (cloud == null) {
            if (data['state'] == 'present') {
                return add_cloud(jenkins_instance, data)
            }
            return false
        }

        if (data['state'] == 'absent') {
            jenkins_instance.clouds.remove(cloud)
            return true
        }

        return update_cloud(jenkins_instance, cloud, data)
    }
    catch(Exception e) {
        throw new Exception(
            'Docker cloud management error, error message : ' + e.getMessage())
    }
}


/* SCRIPT */

def Boolean has_changed = false

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()

    // Get arguments data
    def Map data = parse_data(args[0])

    // Manage new configuration
    has_changed = manage_docker_cloud(jenkins_instance, data)

    // Save new configuration to disk
    jenkins_instance.save()
}
catch(Exception e) {
    throw new RuntimeException(e.getMessage())
}

// Build json result
result = new JsonBuilder()
result {
    changed has_changed.any()
    output {
        changed has_changed
    }
}

println result

