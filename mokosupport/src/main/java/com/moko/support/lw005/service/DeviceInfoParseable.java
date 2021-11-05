package com.moko.support.lw005.service;

import com.moko.support.lw005.entity.DeviceInfo;

public interface DeviceInfoParseable<T> {
    T parseDeviceInfo(DeviceInfo deviceInfo);
}
