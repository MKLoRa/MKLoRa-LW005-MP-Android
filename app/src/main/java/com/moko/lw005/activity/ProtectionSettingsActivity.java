package com.moko.lw005.activity;


import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.lw005.AppConstants;
import com.moko.lw005.R;
import com.moko.lw005.dialog.LoadingMessageDialog;
import com.moko.support.lw005.LoRaLW005MokoSupport;
import com.moko.support.lw005.OrderTaskAssembler;
import com.moko.support.lw005.entity.OrderCHAR;
import com.moko.support.lw005.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

public class ProtectionSettingsActivity extends BaseActivity {


    private boolean mReceiverTag = false;
    private int deviceSpecification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lw005_activity_protection_settings);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
        showSyncingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.getDeviceSpecification());
        LoRaLW005MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                finish();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
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
                            if (flag == 0x00) {
                                // read
                                switch (configKeyEnum) {
                                    case KEY_DEVICE_SPECIFICATION:
                                        if (length > 0) {
                                            deviceSpecification = value[4] & 0xFF;
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
        if (mReceiverTag) {
            mReceiverTag = false;
            // 注销广播
            unregisterReceiver(mReceiver);
        }
        EventBus.getDefault().unregister(this);
    }

    private LoadingMessageDialog mLoadingMessageDialog;

    public void showSyncingProgressDialog() {
        mLoadingMessageDialog = new LoadingMessageDialog();
        mLoadingMessageDialog.setMessage("Syncing..");
        mLoadingMessageDialog.show(getSupportFragmentManager());

    }

    public void dismissSyncProgressDialog() {
        if (mLoadingMessageDialog != null)
            mLoadingMessageDialog.dismissAllowingStateLoss();
    }

    public void onBack(View view) {
        backHome();
    }

    @Override
    public void onBackPressed() {
        backHome();
    }

    private void backHome() {
        finish();
    }

    public void onOverLoadProtection(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, OverLoadProtectionActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_SPECIFICATION, deviceSpecification);
        startActivity(intent);
    }

    public void onOverVoltageProtection(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, OverVoltageProtectionActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_SPECIFICATION, deviceSpecification);
        startActivity(intent);
    }

    public void onSagVoltageProtection(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, SagVoltageProtectionActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_SPECIFICATION, deviceSpecification);
        startActivity(intent);
    }

    public void onOverCurrentProtection(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(this, OverCurrentProtectionActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_SPECIFICATION, deviceSpecification);
        startActivity(intent);
    }
}
