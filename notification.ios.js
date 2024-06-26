"use strict";
import { NativeModules } from "react-native";
const RNNotifications = NativeModules.RNNotifications;

export default class IOSNotification {
  _data: Object;
  _alert: string | Object;
  _sound: string;
  _badge: number;
  _category: string;
  _type: string; // regular / managed
  _finishCalled: boolean;
  _thread: string;

  constructor(notification: Object) {
    this._data = {};

    if (notification.aps &&
      notification.aps["content-available"] &&
      notification.aps["content-available"] === 1 &&
      !notification.aps.alert &&
      !notification.aps.sound &&
      notification.managedAps) {
      // managed notification
      this._alert = notification.managedAps.alert;
      this._sound = notification.managedAps.sound;
      this._badge = notification.aps.badge;
      this._category = notification.managedAps.category;
      this._type = "managed";
      this._thread = notification.aps["thread-id"];
    } else if (
      notification.aps &&
      notification.aps.alert) {
      // regular notification
      this._alert = notification.aps.alert;
      this._sound = notification.aps.sound;
      this._badge = notification.aps.badge;
      this._category = notification.aps.category;
      this._type = "regular";
      this._thread = notification.aps["thread-id"];
    }

    Object.keys(notification).filter(key => key !== "aps").forEach(key => {
      this._data[key] = notification[key];
    });
    this._finishCalled = false;
  }

  getMessage(): ?string | ?Object {
    return this._alert;
  }

  getSound(): ?string {
    return this._sound;
  }

  getBadgeCount(): ?number {
    return this._badge;
  }

  getCategory(): ?string {
    return this._category;
  }

  getData(): ?Object {
    return this._data;
  }

  getType(): ?string {
    return this._type;
  }

  getPushType(): ?string {
    return this._data._pushType;
  }

  finish(finishResult) {
    if (!this._finishCalled && this._data._completionHandlerId) {
      this._finishCalled = true;
      const result = finishResult || REMOTE_NOTIFICATION_RESULT.NoData;
      if (!Object.values(REMOTE_NOTIFICATION_RESULT).includes(result)) {
        throw new Error("Invalid REMOTE_NOTIFICATION_RESULT value");
      }
      RNNotifications.finishRemoteNotification(this._data._completionHandlerId, result);
    }
  }

  getThread(): ?string {
    return this._thread;
  }
}

export const REMOTE_NOTIFICATION_RESULT = {
  NewData: "NewData",
  NoData: "NoData",
  Failed: "Failed"
};
