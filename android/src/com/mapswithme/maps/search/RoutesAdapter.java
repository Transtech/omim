package com.mapswithme.maps.search;

import android.content.res.Resources;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.mapswithme.maps.R;
import com.mapswithme.transtech.route.RouteManager;
import com.mapswithme.transtech.route.RouteTrip;
import com.mapswithme.util.Graphics;
import com.mapswithme.util.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

class RoutesAdapter extends RecyclerView.Adapter<RoutesAdapter.ViewHolder>
{
    private final LayoutInflater mInflater;
    private List<RouteTrip> allTrips = new ArrayList<RouteTrip>();

    interface OnRouteSelectedListener
    {
        void onRouteSelected( int routeId, String routeName );
    }

    private OnRouteSelectedListener mListener;

    RoutesAdapter( Fragment fragment )
    {
        final String packageName = fragment.getActivity().getPackageName();
        final boolean isNightTheme = ThemeUtils.isNightTheme();
        final Resources resources = fragment.getActivity().getResources();

        allTrips = RouteManager.findPlannedRoutes( fragment.getActivity() );

        if( fragment instanceof OnRouteSelectedListener )
            mListener = (OnRouteSelectedListener) fragment;

        mInflater = LayoutInflater.from( fragment.getActivity() );
    }

    @Override
    public ViewHolder onCreateViewHolder( ViewGroup parent, int viewType )
    {
        final View view = mInflater.inflate( R.layout.item_search_routes, parent, false );
        return new ViewHolder( view );
    }

    @Override
    public void onBindViewHolder( ViewHolder holder, int position )
    {
        holder.setTextAndIcon( allTrips.get( position ).getName(), R.drawable.ic_category_pharmacy );
    }

    @Override
    public int getItemCount()
    {
        return allTrips.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        private final TextView mText;

        public ViewHolder( View itemView )
        {
            super( itemView );
            mText = (TextView) itemView;
            itemView.setOnClickListener( this );
            Graphics.tint( mText );
        }

        @Override
        public void onClick( View v )
        {
            final int position = getAdapterPosition();
            int routeId = allTrips.get( position ).getId();
            String routeName = allTrips.get( position ).getName();
            if( mListener != null )
                mListener.onRouteSelected( routeId, routeName );
        }

        void setTextAndIcon(String text, @DrawableRes int iconResId)
        {
            mText.setText(text);
            mText.setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, 0, 0);
        }

    }
}
