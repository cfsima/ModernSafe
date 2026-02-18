package io.github.cfsima.modernsafe.wrappers;

import io.github.cfsima.modernsafe.wrappers.icecreamsandwich.WrapNotificationBuilder;

public class CheckWrappers {

    public static boolean mNotificationBuilderAvailable;

    static {
        try {
            WrapNotificationBuilder.checkAvailable();
            mNotificationBuilderAvailable = true;
        } catch (Throwable t) {
            mNotificationBuilderAvailable = false;
        }
    }
}
