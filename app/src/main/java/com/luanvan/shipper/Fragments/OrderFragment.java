package com.luanvan.shipper.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.luanvan.shipper.R;
import com.luanvan.shipper.components.Shared;

public class OrderFragment extends Fragment {

    private SwitchMaterial switchOrderStatus;
    private MaterialToolbar toolbar;
    private ImageButton ibRefresh;

    public OrderFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        switchOrderStatus = view.findViewById(R.id.switchOrderStatus);
        toolbar = view.findViewById(R.id.toolbar);
        ibRefresh = view.findViewById(R.id.ibRefresh);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        loadSettings();

        switchOrderStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    Toast.makeText(getActivity(), getResources().getString(R.string.receiving_orders), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.not_receiving_orders), Toast.LENGTH_LONG).show();
                }
                saveSettings();
            }
        });
    }

    private void saveSettings(){
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(Shared.SETTINGS, Context.MODE_PRIVATE).edit();
        editor.putBoolean(Shared.KEY_SWITCH_STATUS, switchOrderStatus.isChecked());

        editor.apply();
    }
    private void loadSettings(){
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Shared.SETTINGS, Context.MODE_PRIVATE);

        // updateUI
        switchOrderStatus.setChecked(sharedPreferences.getBoolean(Shared.KEY_SWITCH_STATUS, false));
    }
}