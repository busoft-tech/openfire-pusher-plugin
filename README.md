# Openfire Pusher Plugin

The Push Notification Plugin is a plugin for the [Openfire XMPP server](https://www.igniterealtime.org/openfire), which adds support sending push notifications to ios and android clients.

Building
--------

This project is using the Maven-based Openfire build process, as introduced in Openfire 4.2.0. To build this plugin locally, ensure that the following are available on your local host:

* A Java Development Kit, version 7 or (preferably) 8
* Apache Maven 3

To build this project, invoke on a command shell:

    $ mvn clean package

Upon completion, the openfire plugin will be available in `target/pusher-openfire-plugin-assembly.jar`. This file should be renamed to `pusher.jar`

Installation
------------
Copy `pusher.jar` into the plugins directory of your Openfire server, or use the Openfire Admin Console to upload the plugin. The plugin will then be automatically deployed.

To upgrade to a new version, copy the new `pusher.jar` file over the existing file.

Configuration
------------

Add those properties to the System Properties and change with your own values.

### iOS

* pusher.apple.apns.bundleId (com.example.project)
* pusher.apple.apns.key (Signing Key from Apple Developer Key)
* pusher.apple.apns.teamId (Team Id from Apple Developer Center)
* pusher.apple.apns.path (/path/to/key.p8)
* pusher.apple.apns.sandbox (true|false)

### Android
* pusher.google.fcm.projectId (Project Id from Google Cloud Messaging)
* pusher.google.fcm.path (/path/to/service_account.json)

Client Configuration
------------

Clients need to use ios or android words in their resources. e.g. username@domain.com/ios | username@domain.com/android

Send enable iq to register client to get push notifications.

```
<iq type="set" id="x42">
    <enable xmlns="urn:xmpp:pusher">
        <token>deviceToken</token>
    </token>
</iq>
```

Send disable iq to unregister client.

```
<iq type='set' id='x97'>
  <disable xmlns='urn:xmpp:pusher'/>
</iq>
```
