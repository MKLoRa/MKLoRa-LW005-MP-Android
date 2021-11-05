package com.moko.support.lw005.entity;

import java.util.UUID;

public enum OrderServices {
    SERVICE_DEVICE_NAME(UUID.fromString("00001800-0000-1000-8000-00805F9B34FB")),
    SERVICE_DEVICE_INFO(UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")),
    SERVICE_CUSTOM(UUID.fromString("0000AA00-0000-1000-8000-00805F9B34FB")),
    SERVICE_ADV(UUID.fromString("0000AA02-0000-1000-8000-00805F9B34FB")),
    ;
    private UUID uuid;

    OrderServices(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}
