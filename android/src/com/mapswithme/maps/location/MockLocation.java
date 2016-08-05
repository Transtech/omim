package com.mapswithme.maps.location;

import android.location.Location;

/**
 * Class MockLocation
 * <p/>
 * Created by agough on 5/08/16 11:27 AM
 */
public class MockLocation extends Location
{
    public MockLocation(String provider)
    {
        super( provider );
    }

    /**
     * Construct a new Location object that is copied from an existing one.
     */
    public MockLocation(Location l)
    {
        super( l );
    }

    public boolean isFromMockProvider()
    {
        return true;
    }
}
