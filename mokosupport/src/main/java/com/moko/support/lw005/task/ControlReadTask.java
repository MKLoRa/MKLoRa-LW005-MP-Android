package com.moko.support.lw005.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.lw005.entity.ControlKeyEnum;
import com.moko.support.lw005.entity.OrderCHAR;

public class ControlReadTask extends OrderTask {
    public byte[] data;

    public ControlReadTask() {
        super(OrderCHAR.CHAR_CONTROL, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
    }

    @Override
    public byte[] assemble() {
        return data;
    }

    public void setData(ControlKeyEnum key) {
        switch (key) {
            case KEY_SWITCH_STATUS:
            case KEY_NETWORK_STATUS:
            case KEY_TOTAL_ENERGY:
            case KEY_LOAD_STATUS:
            case KEY_MAC:
                createGetConfigData(key.getParamsKey());
                break;
        }
    }

    private void createGetConfigData(int configKey) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x00,
                (byte) configKey,
                (byte) 0x00
        };
    }
}
