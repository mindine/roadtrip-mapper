package com.example.roadtripmapper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class WaypointAdapter extends RecyclerView.Adapter<WaypointAdapter.WaypointViewHolder> {

    private List<LatLng> waypoints;

    public WaypointAdapter(List<LatLng> waypoints) {
        this.waypoints = waypoints;
    }

    @NonNull
    @Override
    public WaypointViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waypoint, parent, false);
        return new WaypointViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WaypointViewHolder holder, int position) {
        LatLng point = waypoints.get(position);
        holder.tvWaypoint.setText("LatLng(" + point.latitude + ", " + point.longitude + ")");
    }

    @Override
    public int getItemCount() {
        return waypoints.size();
    }

    static class WaypointViewHolder extends RecyclerView.ViewHolder {
        TextView tvWaypoint;

        WaypointViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWaypoint = itemView.findViewById(R.id.tvWaypoint);
        }
    }
}
