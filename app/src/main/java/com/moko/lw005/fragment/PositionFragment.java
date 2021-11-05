package com.moko.lw005.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.moko.ble.lib.task.OrderTask;
import com.moko.lw005.R;
import com.moko.lw005.R2;
import com.moko.lw005.activity.DeviceInfoActivity;
import com.moko.support.lw005.LoRaLW005MokoSupport;
import com.moko.support.lw005.OrderTaskAssembler;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PositionFragment extends Fragment {
    private static final String TAG = PositionFragment.class.getSimpleName();
    @BindView(R2.id.iv_offline_fix)
    ImageView ivOfflineFix;
    private boolean mOfflineFixEnable;

    private DeviceInfoActivity activity;

    public PositionFragment() {
    }


    public static PositionFragment newInstance() {
        PositionFragment fragment = new PositionFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.lw005_fragment_pos, container, false);
        ButterKnife.bind(this, view);
        activity = (DeviceInfoActivity) getActivity();
        return view;
    }

    public void setOfflineFix(int enable) {
        mOfflineFixEnable = enable == 1;
        ivOfflineFix.setImageResource(mOfflineFixEnable ? R.drawable.lw005_ic_checked : R.drawable.lw005_ic_unchecked);
    }

    public void changeOfflineFix() {
        mOfflineFixEnable = !mOfflineFixEnable;
        activity.showSyncingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setOfflineLocation(mOfflineFixEnable ? 1 : 0));
        orderTasks.add(OrderTaskAssembler.getOfflineLocation());
        LoRaLW005MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }
}
