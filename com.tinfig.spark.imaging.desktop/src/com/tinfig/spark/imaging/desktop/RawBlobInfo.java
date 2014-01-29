package com.tinfig.spark.imaging.desktop;

public class RawBlobInfo
{
    public final int id;
    public final float x;
    public final float y;
    public final float width;
    public final float height;

    public RawBlobInfo(int id, float x, float y, float width, float height)
    {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }

        if (obj instanceof RawBlobInfo == false)
        {
            return false;
        }

        return id == ((RawBlobInfo) obj).id;
    }

    @Override
    public int hashCode()
    {
        return 37 * id;
    }
}
