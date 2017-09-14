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
import hudson.slaves.CloudRetentionStrategy
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
                                data['network'],
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
    Check if two docker cloud base templates have same properties

    @param DockerTemplateBase First cloud template base object
    @param DockerTemplateBase Second cloud template base object
    @return Boolean True if configuration have same properties
*/
def Boolean are_same_template_bases(DockerTemplateBase base_a, DockerTemplateBase base_b) {

    try {
        def List<Boolean> has_changed = []

        has_changed.push(base_a.getImage() != base_b.getImage())
        has_changed.push(base_a.getDnsString() != base_b.getDnsString())
        has_changed.push(base_a.getVolumesString() != base_b.getVolumesString())
        has_changed.push(base_a.getVolumesFromString() != base_b.getVolumesFromString())
        has_changed.push(base_a.getMacAddress() != base_b.getMacAddress())
        has_changed.push(base_a.getDisplayName() != base_b.getDisplayName())
        has_changed.push(base_a.getMemoryLimit() != base_b.getMemoryLimit())
        has_changed.push(base_a.getMemorySwap() != base_b.getMemorySwap())
        has_changed.push(base_a.getCpuShares() != base_b.getCpuShares())
        has_changed.push(base_a.getDockerCommandArray() != base_b.getDockerCommandArray())
        has_changed.push(base_a.getPortMappings() != base_b.getPortMappings())
        has_changed.push(base_a.getLxcConf() != base_b.getLxcConf())
        has_changed.push(base_a.getEnvironmentsString() != base_b.getEnvironmentsString())
        has_changed.push(base_a.getExtraHostsString() != base_b.getExtraHostsString())

        return !has_changed.any()
    }
    catch(Exception e) {
        throw new Exception(
            'Check if two Docker retention policies have same properties error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Check if two docker cloud retention policies have same properties

    @param CloudRetentionStrategy First cloud retention policy object
    @param CloudRetentionStrategy Second cloud retention policy object
    @return Boolean True if configuration have same properties
*/
def Boolean are_same_retention_policies(CloudRetentionStrategy retention_a, CloudRetentionStrategy retention_b) {

    try {
        if (retention_a.getClass() != retention_b.getClass()) {
            return false
        }

        if (retention_a.getIdleMinutes() != retention_b.getIdleMinutes()) {
            return false
        }

        return true
    }
    catch(Exception e) {
        throw new Exception(
            'Check if two Docker retention policies have same properties error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Check if two docker cloud launchers have same properties

    @param DockerComputerLauncher First launcher object
    @param DockerComputerLauncher Second launcher object
    @return Boolean True if configuration have same properties
*/
def Boolean are_same_launchers(DockerComputerLauncher launcher_a,
                               DockerComputerLauncher launcher_b) {

    try {
        def List<Boolean> has_changed = []

        if (launcher_a.getClass().getName() != launcher_b.getClass().getName()) {
            return false
        }

        def String launchers_class = launcher_a.getClass().getName()
        if (launchers_class == 'com.nirima.jenkins.plugins.docker.launcher.DockerComputerSSHLauncher') {
            def SSHConnector connector_a = launcher_a.getSshConnector()
            def SSHConnector connector_b = launcher_b.getSshConnector()

            has_changed.push(connector_a.port != connector_b.port)
            has_changed.push(connector_a.getCredentialsId() != connector_b.getCredentialsId())
            has_changed.push(connector_a.retryWaitTime != connector_b.retryWaitTime)
            has_changed.push(connector_a.maxNumRetries != connector_b.maxNumRetries)
            has_changed.push(connector_a.launchTimeoutSeconds != connector_b.launchTimeoutSeconds)
            has_changed.push(connector_a.suffixStartSlaveCmd != connector_b.suffixStartSlaveCmd)
            has_changed.push(connector_a.prefixStartSlaveCmd != connector_b.prefixStartSlaveCmd)
            has_changed.push(connector_a.javaPath != connector_b.javaPath)
            has_changed.push(connector_a.jvmOptions != connector_b.jvmOptions)

            if ((connector_a.jdkInstaller != null) && (connector_b.jdkInstaller != null)) {
                has_changed.push(connector_a.jdkInstaller.id != connector_b.jdkInstaller.id)
            } else if ((connector_a.jdkInstaller != null) || (connector_b.jdkInstaller != null)) {
                has_changed.push(true)
            }
        } else if (launchers_class == 'com.nirima.jenkins.plugins.docker.launcher.DockerComputerJNLPLauncher') {
            has_changed.push(launcher_a.getJnlpLauncher().equals(launcher_b.getJnlpLauncher()))
            has_changed.push(launcher_a.getUser() != launcher_b.getUser())
        } else {
            throw new Exception('Docker launcher class unmanaged')
        }

        return !has_changed.any()
    }
    catch(Exception e) {
        throw new Exception(
            'Check if two Docker launchers have same content error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Check if two docker cloud templates have same properties

    @param List<DockerTemplate> First cloud object
    @param List<DockerTemplate> Second cloud object
    @return Boolean True if configuration have same properties
*/
def Boolean are_same_templates(List<DockerTemplate> templates_a, List<DockerTemplate> templates_b) {

    try {
        def List<Boolean> has_changed = []
        def int index = 0

        if (templates_a.size() != templates_b.size()) {
            return false
        }

        for (DockerTemplate template_a : templates_a) {
            has_changed.push(!are_same_template_bases(
                template_a.getDockerTemplateBase(),
                templates_b[index].getDockerTemplateBase()))
            has_changed.push(template_a.getLabelString() != templates_b[index].getLabelString())
            has_changed.push(template_a.getMode() != templates_b[index].getMode())
            has_changed.push(template_a.getNumExecutors() != templates_b[index].getNumExecutors())
            has_changed.push(!are_same_retention_policies(
                template_a.getRetentionStrategy(),
                templates_b[index].getRetentionStrategy()))
            has_changed.push(!are_same_launchers(
                template_a.getLauncher(), templates_b[index].getLauncher()))
            has_changed.push(template_a.getRemoteFs() != templates_b[index].getRemoteFs())
            has_changed.push(template_a.getInstanceCap() != templates_b[index].getInstanceCap())
            has_changed.push(template_a.getRemoteFsMapping() != templates_b[index].getRemoteFsMapping())
            has_changed.push(template_a.getLabelSet() != templates_b[index].getLabelSet())
            has_changed.push(template_a.getPullStrategy() != templates_b[index].getPullStrategy())
            has_changed.push(template_a.getShortDescription() != templates_b[index].getShortDescription())
            index++
        }

        return !has_changed.any()
    }
    catch(Exception e) {
        throw new Exception(
            'Check if two Docker templates lists have same content error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Check if two docker cloud configurations have same properties

    @param DockerCloud First cloud object
    @param DockerCloud Second cloud object
    @return Boolean True if configuration have same properties
*/
def Boolean is_same_cloud(DockerCloud cloud_a, DockerCloud cloud_b) {

    try {
        def List<Boolean> has_changed = []

        has_changed.push(cloud_a.containerCap != cloud_b.containerCap)
        has_changed.push(cloud_a.connectTimeout != cloud_b.connectTimeout)
        has_changed.push(cloud_a.readTimeout != cloud_b.readTimeout)
        has_changed.push(!are_same_templates(cloud_a.templates, cloud_b.templates))
        has_changed.push(cloud_a.serverUrl != null ? !cloud_a.serverUrl.equals(cloud_b.serverUrl) : cloud_b.serverUrl != null)
        has_changed.push(cloud_a.version != null ? !cloud_a.version.equals(cloud_b.version) : cloud_b.version != null)
        has_changed.push(cloud_a.credentialsId != null ? !cloud_a.credentialsId.equals(cloud_b.credentialsId) : cloud_b.credentialsId != null)
        has_changed.push(cloud_a.connection != null ? !cloud_a.connection.equals(cloud_b.connection) : cloud_b.connection != null)

        return !has_changed.any()
    }
    catch(Exception e) {
        throw new Exception(
            'Check if two Docker cloud have same properties error, '
            + 'error message : ' + e.getMessage())
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
        if (is_same_cloud(cloud, new_cloud)) {
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

