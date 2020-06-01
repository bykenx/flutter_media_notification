package com.tarafdari.flutter_media_notification;

class Utils {
    static <T> T getDefault(T object, T defaultValue) {
        if (object == null) {
            return defaultValue;
        }
        return object;
    }


}
