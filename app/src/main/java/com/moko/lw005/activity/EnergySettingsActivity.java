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
import com.moko.lw005.databinding.Lw005ActivityEnergySettingsBinding;
import com.moko.lw005.dialog.AlertMessageDialog;
import com.moko.lw005.utils.SPUtiles;
import com.moko.lw005.utils.ToastUtils;
import com.moko.support.lw005.LoRaLW005MokoSupport;
import com.moko.support.lw005.OrderTaskAssembler;
import com.moko.support.lw005.entity.ControlKeyEnum;
import com.moko.support.lw005.entity.OrderCHAR;
import com.moko.support.lw005.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnergySettingsActivity extends BaseActivity {


    private Lw005ActivityEnergySettingsBinding mBind;
    private boolean mReceiverTag = false;
    private boolean savedParamsError;
    private boolean mIsCustomized;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = Lw005ActivityEnergySettingsBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
        EventBus.getDefault().register(this);
        mIsCustomized = SPUtiles.getBooleanValue(this, AppConstants.SP_KEY_CUSTOMIZED_LW005, false);
        if (!mIsCustomized) {
            mBind.etEnergyReportInterval.setHint("1~60");
            mBind.etEnergySaveInterval.setHint("1~60");
        }
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
        showSyncingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.getEnergyConfigInterval());
        orderTasks.add(OrderTaskAssembler.getPowerChangeValue());
        orderTasks.add(OrderTaskAssembler.getTotalEnergy());
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
                    case CHAR_CONTROL:
                        if (value.length >= 4) {
                            int header = value[0] & 0xFF;// 0xED
                            int flag = value[1] & 0xFF;// read or write
                            int cmd = value[2] & 0xFF;
                            if (header != 0xED)
                                return;
                            ControlKeyEnum configKeyEnum = ControlKeyEnum.fromParamKey(cmd);
                            if (configKeyEnum == null) {
                                return;
                            }
                            int length = value[3] & 0xFF;
                            if (flag == 0x00) {
                                // read
                                switch (configKeyEnum) {
                                    case KEY_TOTAL_ENERGY:
                                        if (length > 0) {
                                            int total = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 8));
                                            int constant = MokoUtils.toInt(Arrays.copyOfRange(value, 10, 12));
                                            if (constant != 0) {
                                                float totalEnergy = total * 1.0f / constant;
                                                String energyTotalMonthly = MokoUtils.getDecimalFormat("0.0").format(totalEnergy);
                                                mBind.tvTotalEnergy.setText(energyTotalMonthly);
                                            }
                                        }
                                        break;
                                }
                            }
                        }
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
                                    case KEY_ENERGY_CONFIG_INTERVAL:
                                        if (result != 1) {
                                            savedParamsError = true;
                                        }
                                        break;
                                    case KEY_POWER_CHANGE_VALUE:
                                        if (result != 1) {
                                            savedParamsError = true;
                                        }
                                        if (savedParamsError) {
                                            ToastUtils.showToast(EnergySettingsActivity.this, "Opps！Save failed. Please check the input characters and try again.");
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
                                    case KEY_ENERGY_CONFIG_INTERVAL:
                                        if (length > 0) {
                                            int saveInterval = value[4] & 0xFF;
                                            int reportInterval = value[5] & 0xFF;
                                            mBind.etEnergySaveInterval.setText(String.valueOf(saveInterval));
                                            mBind.etEnergyReportInterval.setText(String.valueOf(reportInterval));
                                        }
                                        break;
                                    case KEY_POWER_CHANGE_VALUE:
                                        if (length > 0) {
                                            int changeValue = value[4] & 0xFF;
                                            mBind.etPowerChangeValue.setText(String.valueOf(changeValue));
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
        final String reportIntervalStr = mBind.etEnergyReportInterval.getText().toString();
        if (TextUtils.isEmpty(reportIntervalStr))
            return false;
        final int reportInterval = Integer.parseInt(reportIntervalStr);
        if (reportInterval < 1 || reportInterval > (mIsCustomized ? 255 : 60))
            return false;
        final String saveIntervalStr = mBind.etEnergySaveInterval.getText().toString();
        if (TextUtils.isEmpty(saveIntervalStr))
            return false;
        final int saveInterval = Integer.parseInt(saveIntervalStr);
        if (saveInterval < 1 || saveInterval > (mIsCustomized ? 255 : 60))
            return false;
        if (reportInterval < saveInterval)
            return false;
        final String powerChangeValueStr = mBind.etPowerChangeValue.getText().toString();
        if (TextUtils.isEmpty(powerChangeValueStr))
            return false;
        final int powerChangeValue = Integer.parseInt(powerChangeValueStr);
        if (powerChangeValue < 1 || powerChangeValue > 100)
            return false;
        return true;

    }

    private void saveParams() {
        final String reportIntervalStr = mBind.etEnergyReportInterval.getText().toString();
        final int reportInterval = Integer.parseInt(reportIntervalStr);
        final String saveIntervalStr = mBind.etEnergySaveInterval.getText().toString();
        final int saveInterval = Integer.parseInt(saveIntervalStr);
        final String powerChangeValueStr = mBind.etPowerChangeValue.getText().toString();
        final int powerChangeValue = Integer.parseInt(powerChangeValueStr);
        savedParamsError = false;
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setEnergyConfigInterval(reportInterval, saveInterval));
        orderTasks.add(OrderTaskAssembler.setPowerChangeValue(powerChangeValue));
        LoRaLW005MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }
}
