package com.moko.support.lw005.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.lw005.entity.ControlKeyEnum;
import com.moko.support.lw005.entity.OrderCHAR;

import java.util.Calendar;

import androidx.annotation.IntRange;

public class ControlWriteTask extends OrderTask {
    public byte[] data;

    public ControlWriteTask() {
        super(OrderCHAR.CHAR_CONTROL, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
    }

    @Override
    public byte[] assemble() {
        return data;
    }


    public void restart() {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ControlKeyEnum.KEY_RESTART.getParamsKey(),
                (byte) 0x00
        };
    }

    public void setSwitchStatus(@IntRange(from = 0, to = 1) int status) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ControlKeyEnum.KEY_SWITCH_STATUS.getParamsKey(),
                (byte) 0x01,
                (byte) status
        };
    }

    public void restore() {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ControlKeyEnum.KEY_RESTORE.getParamsKey(),
                (byte) 0x00
        };
    }

    public void setTime() {
        Calendar calendar = Calendar.getInstance();
        long time = calendar.getTimeInMillis() / 1000;
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; ++i) {
            bytes[i] = (byte) (time >> 8 * (3 - i) & 255);
        }
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ControlKeyEnum.KEY_TIME.getParamsKey(),
                (byte) 0x04,
                bytes[0],
                bytes[1],
                bytes[2],
                bytes[3],
        };
    }
}
