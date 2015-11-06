package com.example.cyril.sensortagti;

import java.util.ArrayList;

/**
 * Implements useful statistical functions.
 */
public class Statistics
{

    /**
     * Returns the min of the input sequence for each dimension.
     * @param sequence of points in 3-dim.
     * @return the min of sequence.
     */
    public static Point3D min(ArrayList<Point3D> sequence)
    {
        double minX=Double.MAX_VALUE,minY=Double.MAX_VALUE,minZ=Double.MAX_VALUE;
        for(int i=0;i<sequence.size();i++)
        {
            Point3D value=sequence.get(i);
            if(minX>value.x)minX=value.x;
            if(minY>value.y)minY=value.y;
            if(minZ>value.z)minZ=value.z;
        }
        return new Point3D(minX,minY,minZ);
    }

    /**
     *
     * Returns the max of the input sequence for each dimension.
     * @param sequence of points in 3-dim.
     * @return the max of sequence.
     */
    public static Point3D max(ArrayList<Point3D> sequence)
    {
        double maxX=Double.MIN_VALUE,maxY=Double.MIN_VALUE,maxZ=Double.MIN_VALUE;
        for(int i=0;i<sequence.size();i++)
        {
            Point3D value=sequence.get(i);
            if(maxX<value.x)maxX=value.x;
            if(maxY<value.y)maxY=value.y;
            if(maxZ<value.z)maxZ=value.z;
        }
        return new Point3D(maxX,maxY,maxZ);
    }

    /**
     * Returns the mean of the input sequence.
     * @param sequence of points in 3-dim.
     * @return the mean of sequence.
     */
    public static Point3D mean(ArrayList<Point3D> sequence)
    {
        double meanX=0.0,meanY=0.0,meanZ=0.0;
        for(int i=0;i<sequence.size();i++)
        {
            meanX+=sequence.get(i).x;
            meanY+=sequence.get(i).y;
            meanZ+=sequence.get(i).z;
        }
        meanX/=(sequence.size()*1.0);
        meanY/=(sequence.size()*1.0);
        meanZ/=(sequence.size()*1.0);
        return new Point3D(meanX,meanY,meanZ);
    }

    /**
     * Returns the variance of the input sequence.
     * @param sequence of points in 3-dim.
     * @return the variance of sequence.
     */
    public static Point3D variance(ArrayList<Point3D> sequence)
    {
        Point3D mean=mean(sequence);
        double varianceX=0.0,varianceY=0.0,varianceZ=0.0;
        for(int i=0;i<sequence.size();i++)
        {
            varianceX+=Math.pow(sequence.get(i).x-mean.x,2);
            varianceY+=Math.pow(sequence.get(i).y-mean.y,2);
            varianceZ+=Math.pow(sequence.get(i).z-mean.z,2);
        }
        varianceX/=(sequence.size()*1.0);
        varianceY/=(sequence.size()*1.0);
        varianceZ/=(sequence.size()*1.0);
        return new Point3D(varianceX,varianceY,varianceZ);
    }

    /**
     * Returns the standard deviation of the input sequence.
     * @param sequence of points in 3-dim.
     * @return the standard deviation of sequence.
     */
    public static Point3D stdDev(ArrayList<Point3D> sequence)
    {
        Point3D variance=variance(sequence);
        Double stdDevX,stdDevY,stdDevZ;
        stdDevX=Math.sqrt(variance.x);
        stdDevY=Math.sqrt(variance.y);
        stdDevZ=Math.sqrt(variance.z);
        return new Point3D(stdDevX,stdDevY,stdDevZ);
    }

    /**
     * Returns the number of saddle points (local min/max) in the x-direction, low deviation.
     * @param sequence of points in 3-dim.
     * @return the number of saddle points in the x-direction in sequence.
     */
    public static int lowSaddleX(ArrayList<Point3D> sequence)
    {
        Point3D stdDev=stdDev(sequence);
        int lowSaddleX=0;
        for(int i=1;i<sequence.size()-1;i++)
        {
            Point3D previous=sequence.get(i-1);
            Point3D current=sequence.get(i);
            Point3D next=sequence.get(i + 1);
            double devX1=Math.abs(current.x-previous.x);
            double devX2=Math.abs(current.x-next.x);
            if(previous.x>=current.x&&current.x<=next.x&&(devX1<stdDev.x&&devX2<stdDev.x))
                lowSaddleX++;
            else if(previous.x<=current.x&&current.x>=next.x&&(devX1<stdDev.x&&devX2<stdDev.x))
                lowSaddleX++;
        }
        return lowSaddleX;
    }

    /**
     * Returns the number of saddle points (local min/max) in the x-direction, high deviation.
     * @param sequence of points in 3-dim.
     * @return the number of saddle points in the x-direction in sequence.
     */
    public static int highSaddleX(ArrayList<Point3D> sequence)
    {
        Point3D stdDev=stdDev(sequence);
        int highSaddleX=0;
        for(int i=1;i<sequence.size()-1;i++)
        {
            Point3D previous=sequence.get(i-1);
            Point3D current=sequence.get(i);
            Point3D next=sequence.get(i + 1);
            double devX1=Math.abs(current.x-previous.x);
            double devX2=Math.abs(current.x-next.x);
            if(previous.x>=current.x&&current.x<=next.x&&(devX1>stdDev.x||devX2>stdDev.x))
                highSaddleX++;
            else if(previous.x<=current.x&&current.x>=next.x&&(devX1>stdDev.x||devX2>stdDev.x))
                highSaddleX++;
        }
        return highSaddleX;
    }

    /**
     * Returns the number of saddle points (local min/max) in the y-direction, low deviation.
     * @param sequence of points in 3-dim.
     * @return the number of saddle points in the y-direction in sequence.
     */
    public static int lowSaddleY(ArrayList<Point3D> sequence)
    {
        Point3D stdDev=stdDev(sequence);
        int lowSaddleY=0;
        for(int i=1;i<sequence.size()-1;i++)
        {
            Point3D previous=sequence.get(i-1);
            Point3D current=sequence.get(i);
            Point3D next=sequence.get(i+1);
            double devY1=Math.abs(current.y-previous.y);
            double devY2=Math.abs(current.y-next.y);
            if(previous.y>=current.y&&current.y<=next.y&&(devY1<stdDev.y&&devY2<stdDev.y))
                lowSaddleY++;
            else if(previous.y<=current.y&&current.y>=next.y&&(devY1<stdDev.y&&devY2<stdDev.y))
                lowSaddleY++;
        }
        return lowSaddleY;
    }

    /**
     * Returns the number of saddle points (local min/max) in the y-direction, high deviation.
     * @param sequence of points in 3-dim.
     * @return the number of saddle points in the y-direction in sequence.
     */
    public static int highSaddleY(ArrayList<Point3D> sequence)
    {
        Point3D stdDev=stdDev(sequence);
        int highSaddleY=0;
        for(int i=1;i<sequence.size()-1;i++)
        {
            Point3D previous=sequence.get(i-1);
            Point3D current=sequence.get(i);
            Point3D next=sequence.get(i+1);
            double devY1=Math.abs(current.y-previous.y);
            double devY2=Math.abs(current.y-next.y);
            if(previous.y>=current.y&&current.y<=next.y&&(devY1>stdDev.y||devY2>stdDev.y))
                highSaddleY++;
            else if(previous.y<=current.y&&current.y>=next.y&&(devY1>stdDev.y||devY2>stdDev.y))
                highSaddleY++;
        }
        return highSaddleY;
    }

    /**
     * Returns the number of saddle points (local min/max) in the z-direction, low deviation.
     * @param sequence of points in 3-dim.
     * @return the number of saddle points in the z-direction in sequence.
     */
    public static int lowSaddleZ(ArrayList<Point3D> sequence)
    {
        Point3D stdDev=stdDev(sequence);
        int lowSaddleZ=0;
        for(int i=1;i<sequence.size()-1;i++)
        {
            Point3D previous=sequence.get(i-1);
            Point3D current=sequence.get(i);
            Point3D next=sequence.get(i+1);
            double devZ1=Math.abs(current.z-previous.z);
            double devZ2=Math.abs(current.z-next.z);
            if(previous.z>=current.z&&current.z<=next.z&&(devZ1<stdDev.z&&devZ2<stdDev.z))
                lowSaddleZ++;
            else if(previous.z<=current.z&&current.z>=next.z&&(devZ1<stdDev.z&&devZ2<stdDev.z))
                lowSaddleZ++;
        }
        return lowSaddleZ;
    }

    /**
     * Returns the number of saddle points (local min/max) in the z-direction, high deviation.
     * @param sequence of points in 3-dim.
     * @return the number of saddle points in the z-direction in sequence.
     */
    public static int highSaddleZ(ArrayList<Point3D> sequence)
    {
        Point3D stdDev=stdDev(sequence);
        int highSaddleZ=0;
        for(int i=1;i<sequence.size()-1;i++)
        {
            Point3D previous=sequence.get(i - 1);
            Point3D current=sequence.get(i);
            Point3D next=sequence.get(i+1);
            double devZ1=Math.abs(current.z-previous.z);
            double devZ2=Math.abs(current.z-next.z);
            if(previous.z>=current.z&&current.z<=next.z&&(devZ1>stdDev.z||devZ2>stdDev.z))
                highSaddleZ++;
            else if(previous.z<=current.z&&current.z>=next.z&&(devZ1>stdDev.z||devZ2>stdDev.z))
                highSaddleZ++;
        }
        return highSaddleZ;
    }

    /**
     *
     * @param sequence of points in 3-dim.
     * @return the number of local min in the x-direction in sequence.
     */
    public static int minX(ArrayList<Point3D> sequence)
    {
        int minX=0;
        for(int i=1;i<sequence.size()-1;i++)
        {
            Point3D previous=sequence.get(i-1);
            Point3D current=sequence.get(i);
            Point3D next=sequence.get(i + 1);
            if(previous.x>current.x&&current.x<next.x)
                minX++;
        }
        return minX;
    }

    /**
     *
     * @param sequence of points in 3-dim.
     * @return the number of local max in the x-direction in sequence.
     */
    public static int maxX(ArrayList<Point3D> sequence)
    {
        int maxX=0;
        for(int i=1;i<sequence.size()-1;i++)
        {
            Point3D previous=sequence.get(i-1);
            Point3D current=sequence.get(i);
            Point3D next=sequence.get(i+1);
            if(previous.x<current.x&&current.x>next.x)
                maxX++;
        }
        return maxX;
    }

    /**
     *
     * @param sequence of points in 3-dim.
     * @return the number of local min in the y-direction in sequence.
     */
    public static int minY(ArrayList<Point3D> sequence)
    {
        int minY=0;
        for(int i=1;i<sequence.size()-1;i++)
        {
            Point3D previous=sequence.get(i-1);
            Point3D current=sequence.get(i);
            Point3D next=sequence.get(i + 1);
            if(previous.y>current.y&&current.y<next.y)
                minY++;
        }
        return minY;
    }

    /**
     *
     * @param sequence of points in 3-dim.
     * @return the number of local max in the y-direction in sequence.
     */
    public static int maxY(ArrayList<Point3D> sequence)
    {
        int maxY=0;
        for(int i=1;i<sequence.size()-1;i++)
        {
            Point3D previous=sequence.get(i-1);
            Point3D current=sequence.get(i);
            Point3D next=sequence.get(i + 1);
            if(previous.y<current.y&&current.y>next.y)
                maxY++;
        }
        return maxY;
    }

    /**
     *
     * @param sequence of points in 3-dim.
     * @return the number of local min in the z-direction in sequence.
     */
    public static int minZ(ArrayList<Point3D> sequence)
    {
        int minZ=0;
        for(int i=1;i<sequence.size()-1;i++)
        {
            Point3D previous=sequence.get(i-1);
            Point3D current=sequence.get(i);
            Point3D next=sequence.get(i + 1);
            if(previous.z>current.z&&current.z<next.z)
                minZ++;
        }
        return minZ;
    }

    /**
     *
     * @param sequence of points in 3-dim.
     * @return the number of local max in the z-direction in sequence.
     */
    public static int maxZ(ArrayList<Point3D> sequence)
    {
        int maxZ=0;
        for(int i=1;i<sequence.size()-1;i++)
        {
            Point3D previous=sequence.get(i-1);
            Point3D current=sequence.get(i);
            Point3D next=sequence.get(i + 1);
            if(previous.z<current.z&&current.z>next.z)
                maxZ++;
        }
        return maxZ;
    }

    /**
     * Filters (bandpass around the mean) the input sequence by the input factor.
     * @param sequence of points in 3-dim.
     * @param factor of filtering.
     */
    public static void filter(ArrayList<Point3D> sequence,int factor)
    {
        // Base case.
        if(factor<=0)return;
        // Recursive case.
        for(int i=0;i<sequence.size();i++)
        {
            Point3D mean=mean(sequence);
            Point3D stdDev=stdDev(sequence);
            Point3D current=sequence.get(i);
            double devX=Math.abs(current.x-mean.x);
            double devY=Math.abs(current.y-mean.y);
            double devZ=Math.abs(current.z-mean.z);
            double x,y,z;
            // x.
            if(devX>stdDev.x&&current.x<mean.x)
                x=mean.x-stdDev.x;
            else if(devX>stdDev.x&&current.x>mean.x)
                x=mean.x+stdDev.x;
            else
                x=current.x;
            // y.
            if(devY>stdDev.y&&current.y<mean.y)
                y=mean.y-stdDev.y;
            else if(devY>stdDev.y&&current.y>mean.y)
                y=mean.y+stdDev.y;
            else
                y=current.y;
            // z.
            if(devZ>stdDev.z&&current.z<mean.z)
                z=mean.z-stdDev.z;
            else if(devZ>stdDev.z&&current.z>mean.z)
                z=mean.z+stdDev.z;
            else
                z=current.z;
            // Set point.
            sequence.set(i,new Point3D(x,y,z));
        }
        filter(sequence,factor-1);
    }

    /**
     * Scales the input sequence to a mean of 0.
     * @param sequence of points in 3-dim.
     */
    public static void scale(ArrayList<Point3D> sequence)
    {
        Point3D mean=mean(sequence);
        for(int i=0;i<sequence.size();i++)
        {
            Point3D current=sequence.get(i);
            double x=current.x-mean.x;
            double y=current.y-mean.y;
            double z=current.z-mean.z;
            sequence.set(i,new Point3D(x,y,z));
        }
    }

    /**
     * Sensitizes the input sequence by the input factor.
     * @param sequence of points in 3-dim.
     * @param factor of sensitization.
     */
    public static void sensitize(ArrayList<Point3D> sequence,int factor)
    {
        // Base case.
        if(factor<=0)return;
        // Recursive case.
        for(int i=0;i<sequence.size();i++)
        {
            Point3D current=sequence.get(i);
            double x=Math.exp(current.x);
            double y=Math.exp(current.y);
            double z=Math.exp(current.z);
            sequence.set(i,new Point3D(x,y,z));
        }
        sensitize(sequence, factor - 1);
    }

    /**
     * Gets the changes in measurements of the input recorder (derivative).
     * @param recorder of points in 3-dim.
     * @param changes of recorder.
     */
    public static void getChanges(ArrayList<Point3D> recorder,ArrayList<Point3D> changes)
    {
        for(int i=1;i<recorder.size();i++)
        {
            Point3D previous=recorder.get(i-1);
            Point3D current=recorder.get(i);
            double x=current.x-previous.x;
            double y=current.y-previous.y;
            double z = current.z - previous.z;
            changes.add(new Point3D(x, y, z));
        }
    }

    /**
     * Gets the area under the curve in the measurements of the input recorder.
     * @param recorder of points in 3-dim.
     * @param area of recorder.
     */
    public static void getArea(ArrayList<Point3D> recorder,ArrayList<Point3D> area)
    {
        Point3D cumulativeArea=new Point3D(0.0,0.0,0.0);
        for(int i=0;i<recorder.size();i++)
        {
            Point3D current=recorder.get(i);
            cumulativeArea.x+=current.x;
            cumulativeArea.y+=current.y;
            cumulativeArea.z+=current.z;
            double x=cumulativeArea.x;
            double y=cumulativeArea.y;
            double z=cumulativeArea.z;
            area.add(new Point3D(x, y, z));
        }
    }

}
