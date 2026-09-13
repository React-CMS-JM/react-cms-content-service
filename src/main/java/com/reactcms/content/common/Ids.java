package com.reactcms.content.common;

import java.util.UUID;

public final class Ids {
    private Ids() {
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }
}
