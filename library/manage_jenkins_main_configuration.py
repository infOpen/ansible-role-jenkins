#!/usr/bin/python


from ansible.module_utils.basic import *  # NOQA
import json
from os.path import basename


module_args = dict(
    disable_remember_me=dict(
        type='bool',
        required=True),
    label=dict(
        type='str',
        required=True),
    mode=dict(
        type='str',
        required=True),
    number_of_executors=dict(
        type='int',
        required=True),
    project_naming_strategy=dict(
        type='dict',
        required=True),
    quiet_period=dict(
        type='int',
        required=True),
    scm_checkout_retry_count=dict(
        type='int',
        required=True),
    slave_agent_port=dict(
        type='int',
        required=True),
    deployment_ssh_key=dict(
        type='str',
        required=False,
        default='/var/lib/jenkins/.ssh/id_rsa'),
    cli_path=dict(
        type='str',
        required=False,
        default='/var/lib/jenkins/jenkins-cli.jar'),
    groovy_scripts_path=dict(
        type='str',
        required=False,
        default='/var/lib/jenkins/groovy_scripts'),
    url=dict(
        type='str',
        required=False,
        default='http://localhost:8080')
)


def main():

    module = AnsibleModule(module_args)

    script = "%s/manage_jenkins_main_configuration.groovy" % (
        module.params['groovy_scripts_path'])

    rc, stdout, stderr = module.run_command(
        "java -jar %s -remoting -s '%s' -i '%s' groovy %s '%s'" %
        (module.params['cli_path'], module.params['url'],
         module.params['deployment_ssh_key'], script,
         json.dumps(module.params)))

    if (rc != 0):
        module.fail_json(msg=stderr)

    json_stdout = json.loads(stdout)
    module.exit_json(changed=bool(json_stdout['changed']),
                     output=json_stdout['output'])


if __name__ == '__main__':
    main()
