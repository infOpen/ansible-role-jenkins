#!/usr/bin/python


from ansible.module_utils.basic import *  # NOQA
from os.path import basename


def main():

    module = AnsibleModule(
        argument_spec=dict(
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
    script = "%s/%s.groovy" % (module.params['groovy_scripts_path'],
                               basename(__file__))

    rc, stdout, stderr = module.run_command(
        "java -jar %s -s '%s' groovy %s" %
        (jenkins_cli_path, jenkins_url, script))

    if (rc != 0):
        module.fail_json(msg=stderr)

    print stdout


if __name__ == '__main__':
    main()
