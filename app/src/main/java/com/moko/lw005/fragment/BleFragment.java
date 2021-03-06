package com.moko.lw005.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.moko.ble.lib.task.OrderTask;
import com.moko.lw005.R;
import com.moko.lw005.R2;
import com.moko.lw005.activity.DeviceInfoActivity;
import com.moko.lw005.entity.TxPowerEnum;
import com.moko.support.lw005.LoRaLW005MokoSupport;
import com.moko.support.lw005.OrderTaskAssembler;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BleFragment extends Fragment implements SeekBar.OnSeekBarChangeListener {
    private static final String TAG = BleFragment.class.getSimpleName();
    private final String FILTER_ASCII = "[ -~]*";

    @BindView(R2.id.et_adv_name)
    EditText etAdvName;
    @BindView(R2.id.et_adv_interval)
    EditText etAdvInterval;
    @BindView(R2.id.iv_connectable)
    ImageView ivConnectable;
    @BindView(R2.id.iv_login_mode)
    ImageView ivLoginMode;
    @BindView(R2.id.tv_change_password)
    TextView tvChangePassword;
    @BindView(R2.id.sb_tx_power)
    SeekBar sbTxPower;
    @BindView(R2.id.tv_tx_power_value)
    TextView tvTxPowerValue;

    //    private boolean mOfflineFixEnable;
    private DeviceInfoActivity activity;

    public BleFragment() {
    }


    public static BleFragment newInstance() {
        BleFragment fragment = new BleFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.lw005_fragment_ble, container, false);
        ButterKnife.bind(this, view);
        activity = (DeviceInfoActivity) getActivity();
        sbTxPower.setOnSeekBarChangeListener(this);
        InputFilter inputFilter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        etAdvName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16), inputFilter});
        return view;
    }

    public void setBleAdvName(String advName) {
        etAdvName.setText(advName);
    }

    public void setBleAdvInterval(int advInterval) {
        etAdvInterval.setText(String.valueOf(advInterval));
    }

    private boolean isConnectable;

    public void setBleConnectable(int connectable) {
        isConnectable = connectable == 1;
        ivConnectable.setImageResource(isConnectable ? R.drawable.lw005_ic_checked : R.drawable.lw005_ic_unchecked);
    }

    private boolean isLoginMode;

    public void setBleLoginMode(int loginMode) {
        isLoginMode = loginMode == 1;
        ivLoginMode.setImageResource(isLoginMode ? R.drawable.lw005_ic_checked : R.drawable.lw005_ic_unchecked);
        tvChangePassword.setVisibility(isLoginMode ? View.VISIBLE : View.GONE);
    }

    public void setBleTxPower(int txPower) {
        TxPowerEnum txPowerEnum = TxPowerEnum.fromTxPower(txPower);
        int progress = txPowerEnum.ordinal();
        sbTxPower.setProgress(progress);
    }


    public void changeConnectable() {
        isConnectable = !isConnectable;
        ivConnectable.setImageResource(isConnectable ? R.drawable.lw005_ic_checked : R.drawable.lw005_ic_unchecked);
    }

    public void changeLoginMode() {
        isLoginMode = !isLoginMode;
        ivLoginMode.setImageResource(isLoginMode ? R.drawable.lw005_ic_checked : R.drawable.lw005_ic_unchecked);
        tvChangePassword.setVisibility(isLoginMode ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        TxPowerEnum txPowerEnum = TxPowerEnum.fromOrdinal(progress);
        if (txPowerEnum == null)
            return;
        int txPower = txPowerEnum.getTxPower();
        tvTxPowerValue.setText(String.format("%ddBm", txPower));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public boolean isValid() {
        final String advIntervalStr = etAdvInterval.getText().toString();
        if (TextUtils.isEmpty(advIntervalStr))
            return false;
        final int interval = Integer.parseInt(advIntervalStr);
        if (interval < 1 || interval > 100) {
            return false;
        }
        return true;
    }

    public void saveParams() {
        final String advNameStr = etAdvName.getText().toString();
        final String advIntervalStr = etAdvInterval.getText().toString();
        final int interval = Integer.parseInt(advIntervalStr);
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setBleAdvName(advNameStr));
        orderTasks.add(OrderTaskAssembler.setBleAdvInterval(interval));
        int progress = sbTxPower.getProgress();
        TxPowerEnum txPowerEnum = TxPowerEnum.fromOrdinal(progress);
        orderTasks.add(OrderTaskAssembler.setBleTxPower(txPowerEnum.getTxPower()));
        orderTasks.add(OrderTaskAssembler.setBleConnectable(isConnectable ? 1 : 0));
        orderTasks.add(OrderTaskAssembler.setBleLoginMode(isLoginMode ? 1 : 0));
        LoRaLW005MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }
}
