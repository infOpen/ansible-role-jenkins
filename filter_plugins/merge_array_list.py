from ansible import errors


#
# Additionnal Jinja2 filter to merge array list to a simple array
#

def merge_array_list(arg):
    """
        Merge multiple arrays into a single array
        :param arg: lists
        :type arg: list
        :return: The final array
        :rtype: list
    """

    # Check if arg is a string with managed content or boolean
    if type(arg) != list:
        raise errors.AnsibleFilterError('Invalid value type, should be array')

    final_list = []

    for cur_list in arg:
        final_list += cur_list

    return final_list


class FilterModule(object):
    """ Filters to manage new merge_array_list filter"""

    filter_map = {
        'merge_array_list': merge_array_list
    }
    def filters(self):
        return self.filter_map
