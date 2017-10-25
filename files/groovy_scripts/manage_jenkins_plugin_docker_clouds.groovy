#!/usr/bin/env groovy

import com.nirima.jenkins.plugins.docker.DockerCloud
import com.nirima.jenkins.plugins.docker.DockerImagePullStrategy
import com.nirima.jenkins.plugins.docker.DockerTemplate
import com.nirima.jenkins.plugins.docker.DockerTemplateBase
import com.nirima.jenkins.plugins.docker.strategy.DockerOnceRetentionStrategy
import groovy.json.*
import hudson.model.*
import hudson.plugins.sshslaves.SSHConnector
import hudson.slaves.Cloud
import hudson.slaves.CloudRetentionStrategy
import hudson.slaves.RetentionStrategy
import io.jenkins.docker.connector.DockerComputerConnector
import io.jenkins.docker.connector.DockerComputerSSHConnector
import io.jenkins.docker.connector.DockerComputerSSHConnector.InjectSSHKey
import io.jenkins.docker.connector.DockerComputerSSHConnector.ManuallyConfiguredSSHKey
import io.jenkins.docker.connector.DockerComputerSSHConnector.SSHKeyStrategy
import jenkins.model.*
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerEndpoint


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
                                data['pull_credentials_id'],
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
    Create SSH strategy oject

    @param Map Needed configuration
    @return SSHKeyStrategy New docker SSH strategy
*/
def SSHKeyStrategy create_ssh_key_strategy(Map data) {

    try {

        if (data['ssh_key_strategy_name'] == 'inject_ssh_key') {
            return new InjectSSHKey(data['ssh_key_strategy_value'])
        }
        else if (data['ssh_key_strategy_name'] == 'manually_configured_ssh_key') {
            return new ManuallyConfiguredSSHKey(data['ssh_key_strategy_value'])
        }
        else {
            throw new Exception('Docker SSH key strategy class unmanaged')
        }

    }
    catch(Exception e) {
        throw new Exception(
            'Manage docker SSH key strategy create error, error message : '
            + e.getMessage())
    }
}


/**
    Create new SSH connector

    @param Map Needed configuration
    @return DockerComputerSSHConnector New docker SSH connector
*/
def DockerComputerSSHConnector create_ssh_connector(Map data) {

    try {

        def SSHKeyStrategy ssh_key_strategy = create_ssh_key_strategy(data)
        def DockerComputerSSHConnector ssh_connector
        ssh_connector = new DockerComputerSSHConnector(ssh_key_strategy)

        ssh_connector.setPort(data['port'])
        ssh_connector.setJvmOptions(data['jvm_options'].join(' '))
        ssh_connector.setJavaPath(data['java_path'])
        ssh_connector.setPrefixStartSlaveCmd(data['prefix_start_slave_cmd'])
        ssh_connector.setSuffixStartSlaveCmd(data['suffix_start_slave_cmd'])
        ssh_connector.setLaunchTimeoutSeconds(data['launch_timeout'])

        return ssh_connector
    }
    catch(Exception e) {
        throw new Exception(
            'SSH docker connector create error, error message : '
            + e.getMessage())
    }
}


/**
    Create new Docker connector

    @param Map Needed configuration
    @return DockerComputerConnector New docker SSH launcher
*/
def DockerComputerConnector create_connector(Map data) {

    try {

        def DockerComputerConnector connector

        switch (data['class']) {

            case 'ssh':
                connector = create_ssh_connector(data)
                break
            default:
                throw new Exception('Connector type not managed')
        }

        return connector
    }
    catch(Exception e) {
        throw new Exception(
            'Docker connector create error, error message : ' + e.getMessage())
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
        def DockerComputerConnector connector = create_connector(data['connector'])

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
                        data['instance_cap'].toString(),
                        [])

        // Additional settings
        tpl.setNumExecutors(data['num_executors'])
        tpl.setMode(Enum.valueOf(Node.Mode, data['mode']))
        tpl.setConnector(connector)
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

        def DockerServerEndpoint dockerHost = new DockerServerEndpoint(
            data['server_url'],
            data['credentials_id'],
        )
        def DockerCloud cloud = new DockerCloud(
                                        data['name'],
                                        templates,
                                        dockerHost,
                                        data['container_cap'],
                                        data['connect_timeout'],
                                        data['read_timeout'],
                                        data['version'],
                                        data['docker_hostname'])

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
    Check if two docker cloud connector have same properties

    @param DockerComputerConnector First connector object
    @param DockerComputerConnector Second connector object
    @return Boolean True if configuration have same properties
*/
def Boolean are_same_connectors(DockerComputerConnector connector_a,
                                DockerComputerConnector connector_b) {

    try {
        def List<Boolean> has_changed = []

        if (connector_a.getClass().getName() != connector_b.getClass().getName()) {
            return false
        }

        def String connectors_class = connector_a.getClass().getName()
        if (connectors_class == 'io.jenkins.docker.connector.DockerComputerSSHConnector') {
            has_changed.push(connector_a.getPort() != connector_b.getPort())
            has_changed.push(connector_a.getJvmOptions() != connector_b.getJvmOptions())
            has_changed.push(connector_a.getJavaPath() != connector_b.getJavaPath())
            has_changed.push(connector_a.getPrefixStartSlaveCmd() != connector_b.getPrefixStartSlaveCmd())
            has_changed.push(connector_a.getSuffixStartSlaveCmd() != connector_b.getSuffixStartSlaveCmd())
            has_changed.push(connector_a.getLaunchTimeoutSeconds() != connector_b.getLaunchTimeoutSeconds())
        } else if (connectors_class == 'io.jenkins.docker.connector.DockerComputerJNLPConnector') {
            has_changed.push(connector_a.getJnlpLauncher().equals(connector_b.getJnlpLauncher()))
            has_changed.push(connector_a.getUser() != connector_b.getUser())
        } else {
            throw new Exception('Docker connector class unmanaged')
        }

        return !has_changed.any()
    }
    catch(Exception e) {
        throw new Exception(
            'Check if two Docker connectors have same content error, '
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
            has_changed.push(!are_same_connectors(
                template_a.getConnector(), templates_b[index].getConnector()))
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
        docker_host_a = cloud_a.getDockerHost()
        docker_host_b = cloud_a.getDockerHost()

        has_changed.push(cloud_a.containerCap != cloud_b.containerCap)
        has_changed.push(cloud_a.connectTimeout != cloud_b.connectTimeout)
        has_changed.push(cloud_a.readTimeout != cloud_b.readTimeout)
        has_changed.push(!are_same_templates(cloud_a.templates, cloud_b.templates))
        has_changed.push(cloud_a.serverUrl != null ? !cloud_a.serverUrl.equals(cloud_b.serverUrl) : cloud_b.serverUrl != null)
        has_changed.push(cloud_a.version != null ? !cloud_a.version.equals(cloud_b.version) : cloud_b.version != null)
        has_changed.push(cloud_a.credentialsId != null ? !cloud_a.credentialsId.equals(cloud_b.credentialsId) : cloud_b.credentialsId != null)
        has_changed.push(cloud_a.connection != null ? !cloud_a.connection.equals(cloud_b.connection) : cloud_b.connection != null)
        has_changed.push(docker_host_a.getCredentialsId() != docker_host_b.getCredentialsId())
        has_changed.push(docker_host_a.getUri() != docker_host_b.getUri())

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

