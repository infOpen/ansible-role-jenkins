#!/usr/bin/python


from ansible.module_utils.basic import *  # NOQA
from os.path import basename


def main():

    module = AnsibleModule(
        argument_spec=dict(
            use_ssh_key=dict(
                type='bool',
                required=False,
                default=True),
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
    )

    jenkins_cli_path = module.params['cli_path']
    jenkins_url = module.params['url']
    deployment_ssh_key = module.params['deployment_ssh_key']
    script = "%s/%s.groovy" % (module.params['groovy_scripts_path'],
                               basename(__file__))

    if module.params['use_ssh_key'] is False:
        rc, stdout, stderr = module.run_command(
            "java -jar %s -s '%s' -noKeyAuth groovy %s" %
            (jenkins_cli_path, jenkins_url, script))
    else:
        rc, stdout, stderr = module.run_command(
            "java -jar %s -s '%s' -i '%s' groovy %s" %
            (jenkins_cli_path, jenkins_url, deployment_ssh_key, script))

    if (rc != 0):
        module.fail_json(msg=stderr)

    print stdout


if __name__ == '__main__':
    main()
