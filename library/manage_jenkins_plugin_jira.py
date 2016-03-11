#!/usr/bin/python


from ansible.module_utils.basic import *  # NOQA
import json
from os.path import basename


def main():

    module = AnsibleModule(
        argument_spec=dict(
            jira_url=dict(
                type='str',
                required=True),
            jira_alt_url=dict(
                type='str',
                required=False,
                default=''),
            use_http_auth=dict(
                type='bool',
                required=False),
            user=dict(
                type='str',
                required=False,
                default=''),
            password=dict(
                type='str',
                required=False,
                default=''),
            group_visibility=dict(
                type='str',
                required=False,
                default=''),
            role_visibility=dict(
                type='str',
                required=False,
                default=''),
            supports_wiki_style=dict(
                type='bool',
                required=False,
                default=True),
            record_scm_changes=dict(
                type='bool',
                required=False,
                default=True),
            timeout=dict(
                type='int',
                required=False,
                default=60),
            datetime_pattern=dict(
                type='str',
                required=False,
                default=''),
            user_pattern=dict(
                type='str',
                required=False,
                default=''),
            append_change_timestamp=dict(
                type='bool',
                required=False,
                default=True),
            update_for_all_status=dict(
                type='bool',
                required=False,
                default=True),
            state=dict(
                type='str',
                required=False,
                default='present',
                choice=['present', 'absent']),
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

    script = "%s/%s.groovy" % (module.params['groovy_scripts_path'],
                               basename(__file__))

    rc, stdout, stderr = module.run_command(
        "java -jar %s -s '%s' groovy %s '%s'" %
        (module.params['cli_path'], module.params['url'], script,
         json.dumps(module.params)))

    if (rc != 0):
        module.fail_json(msg=stderr)

    json_stdout = json.loads(stdout)
    module.exit_json(changed=bool(json_stdout['changed']),
                     output=json_stdout['output'])


if __name__ == '__main__':
    main()
