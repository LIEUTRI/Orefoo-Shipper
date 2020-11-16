package com.luanvan.shipper.Adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.luanvan.shipper.Fragments.ManagerFragment;
import com.luanvan.shipper.Fragments.MeFragment;
import com.luanvan.shipper.Fragments.OrderFragment;
import com.luanvan.shipper.Fragments.RevenueFragment;

import java.util.ArrayList;

public class ViewPagerFragmentAdapter extends FragmentStateAdapter {
  private ArrayList<Fragment> fragments = new ArrayList<>();
  public ViewPagerFragmentAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
    super(fragmentManager, lifecycle);
  }

  public void setFragments(ArrayList<Fragment> fragments) {
    this.fragments = fragments;
  }

  @NonNull
  @Override
  public Fragment createFragment(int position) {
    return fragments.get(position);
  }

  @Override
  public int getItemCount() {
    return fragments.size();
  }
}
