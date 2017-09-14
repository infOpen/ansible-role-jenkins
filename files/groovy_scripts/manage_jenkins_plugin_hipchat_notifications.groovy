#!/usr/bin/env groovy

import groovy.json.*
import hudson.model.*
import jenkins.model.*
import jenkins.plugins.hipchat.model.notifications.Notification.Color
import jenkins.plugins.hipchat.model.NotificationConfig
import jenkins.plugins.hipchat.model.NotificationType


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
    Create new hipchat notification configuration

    @param Map Needed configuration
    @return NotificationConfig New notification configuration
*/
def NotificationConfig create_notification(Map data) {

    try {

        def Color color = Enum.valueOf(Color, data['color'])

        def NotificationType notification_type
        notification_type = Enum.valueOf(NotificationType,
                                         data['notification_type'])

        def NotificationConfig notification
        notification = new NotificationConfig(data['notify_enabled'],
                                              data['text_format'],
                                              notification_type,
                                              color,
                                              data['message_template'])

        return notification
    }
    catch(Exception e) {
        throw new Exception(
            'Manage hipchat notification create error, error message : '
            + e.getMessage())
    }
}


/**
    Manage hipchat new notification configuration

    @param Descriptor Hipchat plugin descriptor
    @param List<NotificationConfig> notifications
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean add_notification(Descriptor desc,
                             List<NotificationConfig> notifications,
                             Map data) {

    try {

        // If no notifications, create an empty list
        if (notifications == null) {
            notifications = []
        }

        // Create the notification to add
        def NotificationConfig notification = create_notification(data)

        // Change setting if different value
        notifications.push(notification)
        desc.setDefaultNotifications(notifications)
        return true
    }
    catch(Exception e) {
        throw new Exception(
            'Manage hipchat new notification error, error message : '
            + e.getMessage())
    }
}


/**
    Check if hipchat notification notify need update

    @param NotificationConfig Hipchat notification config
    @param Map Needed configuration
    @return Boolean True if configuration need change everything, else false
*/
def Boolean check_notify_enabled(NotificationConfig notification, Map data) {

    try {

        def Boolean cur_notify_enabled = notification.isNotifyEnabled()
        def Boolean new_notify_enabled = data['notify_enabled']

        // Check if setting need update
        return (cur_notify_enabled != new_notify_enabled)
    }
    catch(Exception e) {
        throw new Exception(
            'Check hipchat notification notify error, error message : '
            + e.getMessage())
    }
}


/**
    Check if hipchat notification text format need update

    @param NotificationConfig Hipchat notification config
    @param Map Needed configuration
    @return Boolean True if configuration need change everything, else false
*/
def Boolean check_text_format(NotificationConfig notification, Map data) {

    try {

        def Boolean cur_text_format = notification.isTextFormat()
        def Boolean new_text_format = data['text_format']

        // Check if setting need update
        return (cur_text_format != new_text_format)
    }
    catch(Exception e) {
        throw new Exception(
            'Check hipchat notification text format error, error message : '
            + e.getMessage())
    }
}


/**
    Check if hipchat notification color need update

    @param NotificationConfig Hipchat notification config
    @param Map Needed configuration
    @return Boolean True if configuration need change everything, else false
*/
def Boolean check_color(NotificationConfig notification, Map data) {

    try {

        def String cur_color = notification.getColor().toString()
        def String new_color = data['color']

        // Check if setting need update
        return (cur_color.toLowerCase() != new_color.toLowerCase())
    }
    catch(Exception e) {
        throw new Exception(
            'Check hipchat notification color error, error message : '
            + e.getMessage())
    }
}


/**
    Check if hipchat notification message template need update

    @param NotificationConfig Hipchat notification config
    @param Map Needed configuration
    @return Boolean True if configuration need change everything, else false
*/
def Boolean check_message_template(NotificationConfig notification, Map data) {

    try {

        def String cur_message_template = notification.getMessageTemplate()
        def String new_message_template = data['message_template']

        // Check if setting need update
        return (cur_message_template != new_message_template)
    }
    catch(Exception e) {
        throw new Exception(
            'Check hipchat notification message template error, '
            + 'error message : ' + e.getMessage())
    }
}


/**
    Manage hipchat notification configuration update

    @param Descriptor Hipchat plugin descriptor
    @param List<NotificationConfig> notifications
    @param Integer Notification index
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean update_notification(Descriptor desc,
                                List<NotificationConfig> notifications,
                                Integer index,
                                Map data) {

    try {

        def List<Boolean> has_changed = []
        def NotificationConfig notification = notifications[index]


        // Manage data changes
        has_changed.push(check_notify_enabled(notification, data))
        has_changed.push(check_text_format(notification, data))
        has_changed.push(check_color(notification, data))
        has_changed.push(check_message_template(notification, data))

        // Change setting if different value
        if (! has_changed.any()) {
            return false
        }
        notifications[index] = create_notification(data)
        desc.setDefaultNotifications(notifications)
        return true
    }
    catch(Exception e) {
        throw new Exception(
            'Manage hipchat notification update error, error message : '
            + e.getMessage())
    }
}


/**
    Manage hipchat notifications, only one by notification type

    @param Descriptor Hipchat plugin descriptor
    @param Map Needed configuration
    @return Boolean True if configuration change everything, else false
*/
def Boolean manage_notifications(Descriptor desc, Map data) {

    try {
        // Get current notifications
        def List<NotificationConfig> notifications
        notifications = desc.getDefaultNotifications()

        // Get notification with this type if exists
        def Integer index = notifications.findIndexOf {
            it.getNotificationType().name() == data['notification_type']
        }

        if (index == -1) {
            if (data['state'] == 'present') {
                return add_notification(desc, notifications, data)
            }
            return false
        }

        if (data['state'] == 'absent') {
            notifications.remove(index)
            desc.setDefaultNotifications(notifications)
            return true
        }

        return update_notification(desc, notifications, index, data)
    }
    catch(Exception e) {
        throw new Exception(
            'Manage hipchat notifications management error, error message : '
            + e.getMessage())
    }
}


/* SCRIPT */

def Boolean has_changed = false

try {
    def Jenkins jenkins_instance = Jenkins.getInstance()
    def desc = jenkins_instance.getDescriptor(
        'jenkins.plugins.hipchat.HipChatNotifier')

    // Get arguments data
    def Map data = parse_data(args[0])

    // Manage new configuration
    has_changed = manage_notifications(desc, data)

    // Save new configuration to disk
    desc.save()
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

