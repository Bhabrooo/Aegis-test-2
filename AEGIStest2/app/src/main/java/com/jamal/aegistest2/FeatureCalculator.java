package com.jamal.aegistest2;

import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.util.Vector;

/**
 * Created by Jamal on 29-Mar-16.
 */
public class FeatureCalculator {
    private static final int raw_length = 128;

    public double raw_x[],raw_y[],raw_z[];
    public double features[]; // <----- This array will holds the results/calculated features
    private StandardDeviation stdD;
    private Skewness skew;
    private Kurtosis kurt;
    private Percentile perc;
    private Covariance covar;

    private double stdD_val, skew_val, kurt_val, min_X_val, perc_val, covar_val, corr_XY_val, corr_YZ_val; //copy of all features for ease of access

    //Default Constructor
    FeatureCalculator(){
        raw_x = new double[raw_length];
        raw_y = new double[raw_length];
        raw_z = new double[raw_length];
        features= new double[7];
        stdD = new StandardDeviation();
        skew = new Skewness();
        kurt = new Kurtosis();
        perc = new Percentile();
        covar = new Covariance();
        stdD_val=0;
        skew_val=0;
        kurt_val=0;
        min_X_val=0;
        perc_val=0;
        covar_val=0;
        corr_XY_val=0;
        corr_YZ_val=0;
        for (int i=0; i<7; i++){
            features[i]=0;
        }
    }

    void load_data(Vector<Double> x_ble, Vector<Double> y_ble, Vector<Double> z_ble){
        for(int i=0;i<raw_length;i++){
            raw_x[i]=x_ble.elementAt(i);
            raw_y[i]=y_ble.elementAt(i);
            raw_z[i]=z_ble.elementAt(i);
        }
    }

    void update_data(Vector<Double> x_ble, Vector<Double> y_ble, Vector<Double> z_ble){
        stdD_val=0;
        skew_val=0;
        kurt_val=0;
        min_X_val=0;
        perc_val=0;
        covar_val=0;
        corr_XY_val=0;
        corr_YZ_val=0;
        for(int i=0;i<raw_length;i++){
            raw_x[i]=x_ble.elementAt(i);
            raw_y[i]=y_ble.elementAt(i);
            raw_z[i]=z_ble.elementAt(i);
        }
    }

    void get_features(){
        stdD_val = stdD.evaluate(raw_x);
        skew_val = skew.evaluate(raw_x);
        kurt_val = kurt.evaluate(raw_x);

        min_X_val=0;
        for(int i=0;i<raw_length;i++){
            if(raw_x[i]<=min_X_val){
                min_X_val=raw_x[i];
            }
        }

        double perc_Arr[]= new double[25]; // first 25 percentiles
        perc_val=0;                        // sum of first 25 percentiles
        for(int i=1;i<=25;i++){
            perc_Arr[i-1]=perc.evaluate(raw_x,i);
            perc_val=perc_val+ perc_Arr[i-1];
        }

        // insert code below for co-relation of XY and YZ axis
        corr_XY_val=(covar.covariance(raw_x,raw_y))/((stdD.evaluate(raw_x))*(stdD.evaluate(raw_y)));
        corr_YZ_val=(covar.covariance(raw_y,raw_z))/((stdD.evaluate(raw_y))*(stdD.evaluate(raw_z)));

        features[0]=stdD_val;
        features[1]=skew_val;
        features[2]=kurt_val;
        features[3]=min_X_val;
        features[4]=perc_val;
        features[5]=corr_XY_val;
        features[6]=corr_YZ_val;
    }
}
