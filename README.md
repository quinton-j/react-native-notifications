# React Native Notifications [![Build Status](https://travis-ci.org/wix/react-native-notifications.svg)](https://travis-ci.org/wix/react-native-notifications)

Handle all the aspects of push notifications for your app, including remote and local notifications, interactive notifications, silent notifications, and more.

**All the native iOS notifications features are supported!**

_For information regarding proper integration with [react-native-navigation](https://github.com/wix/react-native-navigation), follow [this wiki](https://github.com/wix/react-native-notifications/wiki/Android:-working-with-RNN)._


### iOS

<img src="https://s3.amazonaws.com/nrjio/interactive.gif" alt="Interactive notifications example" width=350/>

- Remote (push) notifications
- Local notifications
- Background/Managed notifications (notifications that can be cleared from the server, like Facebook messenger and Whatsapp web)
- PushKit API (for VoIP and other background messages)
- Interactive notifications (allows you to provide additional functionality to your users outside of your application such as action buttons)

### Android

- Receiving notifications in any App state (foreground, background, "dead")
- Built-in notification drawer management
- High degree of code extensibility to allow for advanced custom layouts and any specific notifications behavior as available by [Android's API](https://developer.android.com/training/notify-user/build-notification.html)
- Android equivalent of React-Native's implementation of [`PushNotificationsIOS.getInitialNotification()`](https://facebook.github.io/react-native/docs/pushnotificationios.html#getinitialnotification).

_Upcoming: local notifications, background-state Rx queue (iOS equivalent)_

## Installation

```
$ npm install react-native-notifications --save
```

### iOS

First, [Manually link](https://facebook.github.io/react-native/docs/linking-libraries-ios.html#manual-linking) the library to your Xcode project.

Then, to enable notifications support add the following line at the top of your `AppDelegate.m`

```objective-c
#import "RNNotifications.h"
```

And the following methods to support registration and receiving notifications:

```objective-c
// Required to register for notifications
- (void)application:(UIApplication *)application didRegisterUserNotificationSettings:(UIUserNotificationSettings *)notificationSettings
{
  [RNNotifications didRegisterUserNotificationSettings:notificationSettings];
}

- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{
  [RNNotifications didRegisterForRemoteNotificationsWithDeviceToken:deviceToken];
}

- (void)application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error {
  [RNNotifications didFailToRegisterForRemoteNotificationsWithError:error];
}

// Required for the notification event.
- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)notification {
  [RNNotifications didReceiveRemoteNotification:notification];
}

// Required for the localNotification event.
- (void)application:(UIApplication *)application didReceiveLocalNotification:(UILocalNotification *)notification
{
  [RNNotifications didReceiveLocalNotification:notification];
}
```

### Android

1. Add a reference to the library's native code in your global `settings.gradle`:

```gradle
include ':react-native-notifications'
project(':react-native-notifications').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-notifications/android')
```

2. Declare the library *and* Firebase Messaging as dependencies in your **app-project's** `build.gradle`:


```gradle
dependencies {
    // ...

    compile 'com.google.firebase:firebase-messaging:10.2.6'

    compile(project(':react-native-notifications')) {
        exclude group: 'com.google.firebase', module: 'firebase-messaging'
    }
}
```

**IMPORTANT:** Excluding `firebase-messaging` from `react-native-notifications`, then adding it as a direct dependency if your app is extremely important. It is a work-around for a bug in the Google Services (`com.google.gms.google-services`) plugin (see _Receiving push notifications_ below).

3. Add the library to your `MainApplication.java`:

```java
import com.wix.reactnativenotifications.RNNotificationsPackage;

// ...

    @Override
    protected List<ReactPackage> getPackages() {
        return Arrays.<ReactPackage>asList(
            new MainReactPackage(),
            // ...
            new RNNotificationsPackage(MainApplication.this)
        );
```

### Receiving push notifications

> This section is only necessary in case you wish to **receive** push notifications in your React-Native app.

Push notifications on Android are managed and dispatched using Google's [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging/) service.

1. Add Firebase Messaging to your Android app in the [Firebase console](https://console.firebase.google.com/), Settings -> Cloud Messaging.

2. [Integrate Firebase](https://firebase.google.com/docs/android/setup) in your native Android project.

If you've followed the instructions correctly your Android project will now include a file called `google-services.json`, your root `build.gradle` (`android/build.gradle`) will have an additional classpath dependency:

```gradle
buildscript {
    // ...
    dependencies {
	// ...
        classpath 'com.google.gms:google-services:3.0.0'
        // ...
    }
}
```

and the bottom of your app's `build.gradle` (`android/app/build.gradle`) will look something like:

```gradle

// ...

apply plugin: 'com.google.gms.google-services'
```

---

## Register to Push Notifications

### iOS

In order to handle notifications, you must register before- handle `remoteNotificationsRegistered` event.

In your React Native app:

```javascript
import NotificationsIOS from 'react-native-notifications';

class App extends Component {
	constructor() {
		NotificationsIOS.addEventListener('remoteNotificationsRegistered', this.onPushRegistered.bind(this));
		NotificationsIOS.addEventListener('remoteNotificationsRegistrationFailed', this.onPushRegistrationFaled.bind(this));
		NotificationsIOS.requestPermissions();
	}

	onPushRegistered(deviceToken) {
		console.log("Device Token Received", deviceToken);
	}

	onPushRegistrationFailed(error) {
		// For example:
		//
		// error={
		//   domain: 'NSCocoaErroDomain',
		//   code: 3010,
		//   localizedDescription: 'remote notifications are not supported in the simulator'
		// }
		console.error(error);
	}

	componentWillUnmount() {
  		// prevent memory leaks!
  		NotificationsIOS.removeEventListener('remoteNotificationsRegistered', this.onPushRegistered.bind(this));
		NotificationsIOS.removeEventListener('remoteNotificationsRegistrationFailed', this.onPushRegistrationFailed.bind(this));
	}
}

```

When you have the device token, POST it to your server and register the device in your notifications provider (Amazon SNS, Azure, etc.).

### Android

The React-Native code equivalent on Android is:

```javascript
import {NotificationsAndroid} from 'react-native-notifications';

// On Android, we allow for only one (global) listener per each event type.
NotificationsAndroid.setRegistrationTokenUpdateListener((deviceToken) => {
	console.log('Push-notifications registered!', deviceToken)
});
```

`deviceToken` being the token used to identify the device on the GCM.

---

## Handling Received Notifications

### Background Queue _Important!_

When a push notification is received or opened and the app is not yet running, the application will automatically be launched. However, the OS notifies the application events before the JS engine is fully initialized. Hence, your listeners/callbacks aren't yet listening for events.

To ensure you don't miss any events, the application will *not* deliver any events (notifications, actions, etc.) until your application explicitly indicates it is ready to receive them. Events will be queued up until the app is ready, you indicate your application is ready to receive events by calling:

 * `NotificationsIOS.consumeBackgroundQueue()`, and/or
 * `NotificationsAndroid.consumeBackgroundQueue()`

Typically. you will call `consumeBackgroundQueue()` immediately after setting up your event listeners (further details below).

#### Register your listeners outside the Component hierarchy

When your applications is launched in the background in response to a push notification being received, React Native will *not* `render()` your application's view/component hierarchy; in fact, your root component won't even be initialized. To handle events in the background your listeners must therefore be installed as part of the Javascript app start-up (part of, or included from, `index.ios.js`/`index.android.js`), *not* added/removed inside Components.

### iOS

When you receive a notification, the application can be in one of the following states:

1. **Forground**- When the app in running and is used by the user right now. in this case, `notificationReceivedForeground` event will be fired.
2. **Background**- When the app is running but in background state. in this case, `notificationReceivedBackground` event will be fired.
3. **Notification Opened**- When you open the notifications from the notification center. in this case, `notificationOpened` event will be fired.

Example:

```javascript
NotificationsIOS.addEventListener('notificationReceivedForeground', onNotificationReceivedForeground());
NotificationsIOS.addEventListener('notificationReceivedBackground', onNotificationReceivedBackground());
NotificationsIOS.addEventListener('notificationOpened', onNotificationOpened());

function onNotificationReceivedForeground(notification) {
	console.log("Notification Received - Foreground", notification);
}

function onNotificationReceivedBackground(notification) {
	console.log("Notification Received - Background", notification);
}

function onNotificationOpened(notification) {
	console.log("Notification opened by device user", notification);
}
```

You can remove listeners as follows:

```
NotificationsIOS.removeEventListener('notificationReceivedForeground', onNotificationReceivedForeground);
NotificationsIOS.removeEventListener('notificationReceivedBackground', onNotificationReceivedBackground);
NotificationsIOS.removeEventListener('notificationOpened', onNotificationOpened);
```

#### Notification Object
When you receive a push notification, you'll get an instance of `IOSNotification` object, contains the following methods:

- **`getMessage()`**- returns the notification's main message string.
- **`getSound()`**- returns the sound string from the `aps` object.
- **`getBadgeCount()`**- returns the badge count number from the `aps` object.
- **`getCategory()`**- returns the category from the `aps` object (related to interactive notifications).
- **`getData()`**- returns the data payload (additional info) of the notification.
- **`getType()`**- returns `managed` for managed notifications, otherwise returns `regular`.

### Android

```javascript
// On Android, we allow for only one (global) listener per each event type.
NotificationsAndroid.setNotificationReceivedListener((notification) => {
	console.log("Notification received on device", notification.getData());
});
NotificationsAndroid.setNotificationOpenedListener((notification) => {
	console.log("Notification opened by device user", notification.getData());
});
```

#### Notification Object

- **`isDataOnly()`**- indicates whether the notification contains only data (`getData()`) and not other notification properties.
- **`getData()`**- content of the `data` section of the original message (sent to Firebase's servers).
- **`getTitle()`**- the notification's title.
- **`getBody()`/`getMessage()`**- the notification's body.
- **`getIcon()`**- the notification's icon (the name of a Android _drawable_ bundled with your app).
- **`getSound()`**- the notification's sound (the name of a native Android sound asset bundled with your app).
- **`getTag()`**- an identifier for this notification. If two notifications are posted (locally or remotely) with the same _tag_ AND `id` (_not_ part of the notification's properties) the latter notification will replace/update the former.
- **`getColor()`**- a `#rrggbb` formatted color that will be used to tint your app icon and name in the system notification tray/drawer.
- **`getLargeIcon()`**- a larger icon (typically displayed on the right) of your notification. This can be specified as a **URL** (local or online), or as the name of an Android _drawable_ bundled in your app.
- **`getLightsColor()`**- a `#rrggbb` formatted color that will set the color of the flashing notification LED on device's that have one. Typically this will only light-up if the device's screen is off when the notification is received.
- **`getLightsOnMs()`**- how many milliseconds the notification LED should stay lit whilst flashing.
- **`getLightsOffMs()`**- how many milliseconds the notification LED should stay unlit between flashes.
---

#### Receiving Notifications in the Background

On Android there are a very specific set of rules regarding when when and _if_ notifications are delivered to your application.

Data-only push notifications are _always_ delivered to your application when Android receives them. Foreground, background, even if your app is dead, it will be woken to receive the notification.

Firebase push notifications that contain visual notification information (title, body etc.) are _not_ data only and will _never_ be delivered to your app if it's running in the background!
Instead the notifications go to the "system tray" application, and it automatically generates a notification in the tray.

These automatic background notifications have several limited functionality:

- They do not support the full range of options that are available to app generated "local notifications". e.g. `largeIcon` is not supported.
  Please refer to the official [Firebase server reference](https://firebase.google.com/docs/cloud-messaging/http-server-ref#table2) to see what is supported.

- When a user taps on an system tray generated notification the Android OS will actually re-launch your app's primary/launcher activity. If you're using stock React Native this effectively restarts your app.

- Because your app is relaunched, you will _not_ receive a notification open event to your listener. Instead the `data` (`title`, `body` etc. will be omitted) from the push notification will be available in your application's _initial notification_ (see below).

> If you want fine-grained control over your application it's suggested you send "data-only" notifications, and use this data to generate a local notification with `NotificationsAndroid.localNotification()`.

## Querying Initial Notification

React-Native's [`PushNotificationsIOS.getInitialNotification()`](https://facebook.github.io/react-native/docs/pushnotificationios.html#getinitialnotification) allows for the async retrieval of the original notification used to open the App on iOS, but it has no equivalent implementation for Android.

We provide a similar implementation on Android using `NotificationsAndroid.getInitialNotification()` which returns a promise:

```javascript
import {NotificationsAndroid} from 'react-native-notifications';

NotificationsAndroid.getInitialNotification()
  .then((notification) => {
  		console.log("Initial notification was:", notification || "N/A");
	})
  .catch((err) => console.error("getInitialNotifiation() failed", err));

```

Notifications are considered 'initial' when a user taps a notification and **ANY** of the following are true:

 - The app was not running at all ("dead" state) when the notification was tapped.
 - The app was running in the background, but with **no running activities** associated with it.
 - The app was in the background _when the push notification was received_ AND the notification was not a "data-only" notification, hence the tapped notification was automatically generated by the system tray _not_ your app (see "Receiving Notifications in the Background").

## Triggering Local Notifications

### iOS

You can manually trigger local notifications in your JS code, to be posted immediately or in the future.
Triggering local notifications is fully compatible with React Native `PushNotificationsIOS` library.

Example:

```javascript
let localNotification = NotificationsIOS.localNotification({
	alertBody: "Local notificiation!",
	alertTitle: "Local Notification Title",
	alertAction: "Click here to open",
	soundName: "chime.aiff",
	category: "SOME_CATEGORY",
	userInfo: { }
});
```

Notification object contains:

- **`fireDate`**- The date and time when the system should deliver the notification (optinal - default is immidiate dispatch).
- `alertBody`- The message displayed in the notification alert.
- `alertTitle`- The title of the notification, displayed in the notifications center.
- `alertAction`- The "action" displayed beneath an actionable notification.
- `soundName`- The sound played when the notification is fired (optional).
- `category`- The category of this notification, required for [interactive notifications](#interactive--actionable-notifications-ios-only) (optional).
- `userInfo`- An optional object containing additional notification data.

### Android

Much like on iOS, notifications can be triggered locally. The API to do so is a simplified version of the iOS equivalent that works more natually with the Android perception of push (remote) notifications:

```javascript
NotificationsAndroid.localNotification({
	title: "Local notification",
	body: "This notification was generated by the app!",
	data: {
	    extra: "Some data"
	}
});
```
The supported properties are:

`data`, `title`, `body`, `icon`, `tag`, `sound`, `color`, `largeIcon`, `lightsColor`, `lightsOnMs` and `lightsOffMs`.

Please refer to the "Notification Object" section for details about each property.

Any custom information you wish to be available when the notification is tapped should be placed inside an object given as the `data` field.

### Cancel Local Notification

The `NotificationsIOS.localNotification()` and `NotificationsAndroid.localNotification()` methods return unique `notificationId` values, which can be used in order to cancel or update/replace specific local notifications.

You can cancel local notification by calling `NotificationsIOS.cancelLocalNotification(notificationId)` or `NotificationsAndroid.cancelLocalNotification(notificationId)`.

Example (iOS):

```javascript
let someLocalNotification = NotificationsIOS.localNotification({
	alertBody: "Local notificiation!",
	alertTitle: "Local Notification Title",
	alertAction: "Click here to open",
	soundName: "chime.aiff",
	category: "SOME_CATEGORY",
	userInfo: { }
});

NotificationsIOS.cancelLocalNotification(someLocalNotification);
```

On Android, it is the pair of an `id` AND `tag` (which may be `null` omitted) which uniquely identify a notification. You can cancel a notification with a `tag` as follows:

```javascript
NotificationsAndroid.cancelLocalNotification(0, "someTag");
```

### Cancel All Local Notifications

Example (iOS):

```javascript
NotificationsIOS.cancelAllLocalNotifications();
```

---

## Managed Notifications (iOS only)

Managed notifications are notifications that can be cleared by a server request.
You can find this feature in facebook messenger, when you receive a message in your mobile, but open it in facebook web. More examples are Whatsapp web and gmail app.

In order to handle managed notifications, your app must support background notifications, and the server should send the notifications you'd like to "manage" a bit differently. Let's start.

First, enable the *Remote notifications* checkbox under **capabilities - Background Modes**:
![Background Modes](http://docs.urbanairship.com/_images/ios-background-push-capabilities1.png)

Then, add the following lines to `info.plist`:

```xml
<key>UIBackgroundModes</key>
<array>
	<string>remote-notification</string>
</array>
```

That's it for the client side!

Now the server should push the notification a bit differently- background instead of reguler. You should also provide the action (`CREATE` notification or `CLEAR` notification), and `notificationId` as a unique identifier of the notification.

**Regular** notification payload:

```javascript
{
  aps: {
    alert: {
      body: "This is regular notification"
	},
	badge: 5,
	sound: "chime.aiff",
  }
}
```

**Managed** notification payload:

```javascript
{
  aps: {
  	"content-available": 1
  },
  managedAps: {
    action: "CREATE", // set it to "CLEAR" in order to clear the notification remotely
    notificationId: "1234", // must be unique identifier
    sound: "chime.aiff",
    alert: {
      body: "This is managed notification"
    }
  }
}
```

---

## PushKit API (iOS only)

The PushKit framework provides the classes for your iOS apps to receive background pushes from remote servers. it has better support for background notifications compared to regular push notifications with `content-available: 1`. More info in [iOS PushKit documentation](https://developer.apple.com/library/ios/documentation/NetworkingInternet/Reference/PushKit_Framework/).

### Register to PushKit
After [preparing your app to receive VoIP push notifications](https://developer.apple.com/library/ios/documentation/Performance/Conceptual/EnergyGuide-iOS/OptimizeVoIP.html), add the following lines to `appDelegate.m` in order to support PushKit events:

```objective-c
#import "RNNotifications.h"
#import <PushKit/PushKit.h>
```

And the following methods:

```objective-c
// PushKit API Support
- (void)pushRegistry:(PKPushRegistry *)registry didUpdatePushCredentials:(PKPushCredentials *)credentials forType:(NSString *)type
{
  [RNNotifications didUpdatePushCredentials:credentials forType:type];
}

- (void)pushRegistry:(PKPushRegistry *)registry didReceiveIncomingPushWithPayload:(PKPushPayload *)payload forType:(NSString *)type
{
  [RNNotifications didReceiveRemoteNotification:payload.dictionaryPayload];
}
```

In your ReactNative code, add event handler for `pushKitRegistered` event and call to `registerPushKit()`:

```javascript
constructor() {
	NotificationsIOS.addEventListener('pushKitRegistered', this.onPushKitRegistered.bind(this));
    NotificationsIOS.registerPushKit();
}

onPushKitRegistered(deviceToken) {
	console.log("PushKit Token Received: " + deviceToken);
}

componentWillUnmount() {
	// Don't forget to remove the event listeners to prevent memory leaks!
	NotificationsIOS.removeEventListener('pushKitRegistered', onPushKitRegistered(this));
}
```

> 1. Notice that PushKit device token and regular notifications device token are different, so you must handle two different tokens in the server side in order to support this feature.
> 2. PushKit will not request permissions from the user for push notifications.


---

## Interactive / Actionable Notifications

> This section provides description for iOS. For notifications customization on Android, refer to [our wiki](https://github.com/wix/react-native-notifications/wiki/Android-Customizations#customizing-notifications-layout).

Interactive notifications allow you to reply to a message right from the notification banner or take action right from the lock screen.

On the Lock screen and within Notification Center, you swipe from right to left
to reveal actions. Destructive actions, like trashing an email, are color-coded red. Relatively neutral actions, like dismissing an alert or declining an invitation, are color-coded gray.

For banners, you pull down to reveal actions as buttons. For popups, the actions are immediately visible — the buttons are right there.

You can find more info about interactive notifications [here](http://www.imore.com/interactive-notifications-ios-8-explained).

![Interactive Notifications](http://i.imgur.com/XrVzy9w.gif)


Notification **actions** allow the user to interact with a given notification.

Notification **categories** allow you to group multiple actions together, and to connect the actions with the push notification itself.

In order to support interactive notifications, firstly add the following methods to `appDelegate.m` file:

```objective-c
// Required for the notification actions.
- (void)application:(UIApplication *)application handleActionWithIdentifier:(NSString *)identifier forLocalNotification:(UILocalNotification *)notification withResponseInfo:(NSDictionary *)responseInfo completionHandler:(void (^)())completionHandler
{
  [RNNotifications handleActionWithIdentifier:identifier forLocalNotification:notification withResponseInfo:responseInfo completionHandler:completionHandler];
}

- (void)application:(UIApplication *)application handleActionWithIdentifier:(NSString *)identifier forRemoteNotification:(NSDictionary *)userInfo withResponseInfo:(NSDictionary *)responseInfo completionHandler:(void (^)())completionHandler
{
  [RNNotifications handleActionWithIdentifier:identifier forRemoteNotification:userInfo withResponseInfo:responseInfo completionHandler:completionHandler];
}
```

Then, follow the basic workflow of adding interactive notifications to your app:

1. Config the actions.
2. Group actions together into categories.
3. Register to push notifications with the configured categories.
4. Push a notification (or trigger a [local](#triggering-local-notifications) one) with the configured category name.

### Example
#### Config the Actions
We will config two actions: upvote and reply.

```javascript
import NotificationsIOS, { NotificationAction, NotificationCategory } from 'react-native-notifications';

let upvoteAction = new NotificationAction({
  activationMode: "background",
  title: String.fromCodePoint(0x1F44D),
  identifier: "UPVOTE_ACTION"
}, (action, completed) => {
  console.log("ACTION RECEIVED");
  console.log(JSON.stringify(action));

  // You must call to completed(), otherwise the action will not be triggered
  completed();
});

let replyAction = new NotificationAction({
  activationMode: "background",
  title: "Reply",
  behavior: "textInput",
  authenticationRequired: true,
  identifier: "REPLY_ACTION"
}, (action, completed) => {
  console.log("ACTION RECEIVED");
  console.log(action);

  completed();
});

```

#### Config the Category
We will group `upvote` action and `reply` action into a single category: `EXAMPLE_CATEGORY `. If the notification contains `EXAMPLE_CATEGORY ` under `category` field, those actions will appear.

```javascript
let exampleCategory = new NotificationCategory({
  identifier: "EXAMPLE_CATEGORY",
  actions: [upvoteAction, replyAction],
  context: "default"
});
```

#### Register to Push Notifications
Instead of basic registration like we've done before, we will register the device to push notifications with the category we've just created.

```javascript
NotificationsIOS.requestPermissions([exampleCategory]);
```

#### Push an Interactive Notification
Notification payload should look like this:

```javascript
{
  aps: {
	// ... (alert, sound, badge, etc)
	category: "EXAMPLE_CATEGORY"
  }
}
```

The [example app](https://github.com/wix/react-native-notifications/tree/master/example) contains this interactive notification example, you can follow there.

### `NotificationAction` Payload

- `title` - Action button title.
- `identifier` - Action identifier (must be unique).
- `activationMode` - Indicating whether the app should activate to the foreground or background.
	- `foreground` (default) - Activate the app and put it in the foreground.
	- `background` - Activate the app and put it in the background. If the app is already in the foreground, it remains in the foreground.
- `behavior` - Indicating additional behavior that the action supports.
	- `default` - No additional behavior.
	- `textInput` - When button is tapped, the action opens a text input. the text will be delivered to your action callback.
- `destructive` - A Boolean value indicating whether the action is destructive. When the value of this property is `true`, the system displays the corresponding button differently to indicate that the action is destructive.
- `authenticationRequired` - A Boolean value indicating whether the user must unlock the device before the action is performed.

### `NotificationCategory` Payload

# Table of Content


#### Set application icon badges count (iOS only)

Set to specific number:
```javascript
NotificationsIOS.setBadgesCount(2);
```
Clear badges icon:
```javascript
NotificationsIOS.setBadgesCount(0);
```
## License
The MIT License.

See [LICENSE](LICENSE)
