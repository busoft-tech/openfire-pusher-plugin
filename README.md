# openfire-pusher-plugin
Openfire Pusher Plugin

* pusher.apple.apns.bundleId
* pusher.apple.apns.key
* pusher.apple.apns.teamId
* pusher.apple.apns.path
* pusher.apple.apns.sandbox 

* pusher.google.fcm.projectId
* pusher.google.fcm.path

```
<iq type="set" id="x42">
    <enable xmlns="urn:xmpp:pusher">
        <token>FE46C9376DAF2EC38E299A266E3EAA383BE0B6142B77C059E3282EB01B8606AC</token>
    </token>
</iq>
```

```
<iq type='set' id='x97'>
  <disable xmlns='urn:xmpp:pusher'/>
</iq>
```