package com.moko.lw005.activity;


import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.lw005.AppConstants;
import com.moko.lw005.databinding.Lw005ActivityAboutBinding;
import com.moko.lw005.databinding.Lw005ActivityOverLoadProtectionBinding;
import com.moko.lib.loraui.dialog.AlertMessageDialog;
import com.moko.lw005.utils.ToastUtils;
import com.moko.support.lw005.LoRaLW005MokoSupport;
import com.moko.support.lw005.OrderTaskAssembler;
import com.moko.support.lw005.entity.OrderCHAR;
import com.moko.support.lw005.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OverLoadProtectionActivity extends BaseActivity {


    private Lw005ActivityOverLoadProtectionBinding mBind;
    private boolean mReceiverTag = false;
    private boolean savedParamsError;
    private int deviceSpecification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = Lw005ActivityOverLoadProtectionBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
        EventBus.getDefault().register(this);
        deviceSpecification = getIntent().getIntExtra(AppConstants.EXTRA_KEY_DEVICE_SPECIFICATION, 0);
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
        showSyncingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.getOverLoadProtection());
        LoRaLW005MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                finish();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (!MokoConstants.ACTION_CURRENT_DATA.equals(action))
            EventBus.getDefault().cancelEventDelivery(event);
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
            }
            if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                dismissSyncProgressDialog();
            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                switch (orderCHAR) {
                    case CHAR_PARAMS:
                        if (value.length >= 4) {
                            int header = value[0] & 0xFF;// 0xED
                            int flag = value[1] & 0xFF;// read or write
                            int cmd = value[2] & 0xFF;
                            if (header != 0xED)
                                return;
                            ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                            if (configKeyEnum == null) {
                                return;
                            }
                            int length = value[3] & 0xFF;
                            if (flag == 0x01) {
                                // write
                                int result = value[4] & 0xFF;
                                switch (configKeyEnum) {
                                    case KEY_OVER_LOAD_PROTECTION:
                                        if (result != 1) {
                                            savedParamsError = true;
                                        }
                                        if (savedParamsError) {
                                            ToastUtils.showToast(OverLoadProtectionActivity.this, "Opps！Save failed. Please check the input characters and try again.");
                                        } else {
                                            AlertMessageDialog dialog = new AlertMessageDialog();
                                            dialog.setMessage("Saved Successfully！");
                                            dialog.setConfirm("OK");
                                            dialog.setCancelGone();
                                            dialog.show(getSupportFragmentManager());
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                // read
                                switch (configKeyEnum) {
                                    case KEY_OVER_LOAD_PROTECTION:
                                        if (length > 0) {
                                            mBind.cbOverLoadProtection.setChecked(value[4] == 1);
                                            byte[] overLoadBytes = Arrays.copyOfRange(value, 5, 7);
                                            int overLoad = MokoUtils.toInt(overLoadBytes);
                                            mBind.etOverLoadThreshold.setText(String.valueOf(overLoad));
                                            mBind.etTimeThreshold.setText(String.valueOf(value[7] & 0xFF));
                                        }
                                        break;
                                }
                            }
                        }
                        break;
                }
            }
        });
    }


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent != null) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            dismissSyncProgressDialog();
                            finish();
                            break;
                    }
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    public void onBack(View view) {
        backHome();
    }

    @Override
    public void onBackPressed() {
        backHome();
    }

    private void backHome() {
        if (mReceiverTag) {
            mReceiverTag = false;
            // 注销广播
            unregisterReceiver(mReceiver);
        }
        EventBus.getDefault().unregister(this);
        setResult(RESULT_OK);
        finish();
    }

    public void onSave(View view) {
        if (isWindowLocked())
            return;
        if (isValid()) {
            showSyncingProgressDialog();
            saveParams();
        } else {
            ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
        }
    }

    private boolean isValid() {
        final String overLoadStr = mBind.etOverLoadThreshold.getText().toString();
        if (TextUtils.isEmpty(overLoadStr))
            return false;
        final int overLoad = Integer.parseInt(overLoadStr);

        int max = 4416;
        if (deviceSpecification == 1) {
            max = 2160;
        } else if (deviceSpecification == 2) {
            max = 3588;
        }
        if (overLoad < 10 || overLoad > max)
            return false;
        final String timeStr = mBind.etTimeThreshold.getText().toString();
        if (TextUtils.isEmpty(timeStr))
            return false;
        final int time = Integer.parseInt(timeStr);
        if (time < 1 || time > 30)
            return false;
        return true;

    }

    private void saveParams() {
        final String overLoadStr = mBind.etOverLoadThreshold.getText().toString();
        final int overLoad = Integer.parseInt(overLoadStr);
        final String timeStr = mBind.etTimeThreshold.getText().toString();
        final int time = Integer.parseInt(timeStr);
        savedParamsError = false;
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setOverLoadProtection(mBind.cbOverLoadProtection.isChecked() ? 1 : 0, overLoad, time));
        LoRaLW005MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }
}
