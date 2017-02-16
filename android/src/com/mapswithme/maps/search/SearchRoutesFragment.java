package com.mapswithme.maps.search;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import com.mapswithme.maps.R;
import com.mapswithme.maps.base.BaseMwmRecyclerFragment;

public class SearchRoutesFragment extends BaseMwmRecyclerFragment
        implements RoutesAdapter.OnRouteSelectedListener
{
    @Override
    protected RecyclerView.Adapter createAdapter()
    {
        return new RoutesAdapter( this );
    }

    @Override
    protected int getLayoutRes()
    {
        return R.layout.fragment_search_routes;
    }

    @Override
    public void onActivityCreated( @Nullable Bundle savedInstanceState )
    {
        super.onActivityCreated( savedInstanceState );
        ((SearchFragment) getParentFragment()).setRecyclerScrollListener( getRecyclerView() );
    }

    @Override
    public void onRouteSelected( int routeId, String routeName )
    {
        if( !passRoute( getParentFragment(), routeId, routeName ) )
            passRoute( getActivity(), routeId, routeName );
    }

    private static boolean passRoute( Object listener, int routeId, String routeName )
    {
        if( !(listener instanceof RoutesAdapter.OnRouteSelectedListener) )
            return false;

        ((RoutesAdapter.OnRouteSelectedListener) listener).onRouteSelected( routeId, routeName );
        return true;
    }
}
