package edu.bc.kimahc.soundProcessing;

import android.util.Log;

public class LowPassFilter {

	public static float[] filter(float[] signal, int SR, float maxFreq){
		long startTime = System.nanoTime();
		if(signal == null){
			System.out.println("Low pass: signal null");
			return null;
		}
		float pi = (float) Math.PI;
		float Q = (float) (1/Math.sqrt(2.));
		float w0    = 2*pi*maxFreq/SR;
		float c     = (float) Math.cos(w0);
		float s     = (float) Math.sin(w0);
		float alpha = s/(2*Q);

		// H(s) = 1 / (s^2 + s/Q + 1);

        float b0 =  (1 - c)/2;
        float b1 =   1 - c;
        float b2 =  (1 - c)/2;
        float a0 =   1 + alpha;
        float a1 =  -2*c;
        float a2 =   1 - alpha;
        
    	b0 /= a0;
		b1 /= a0;
		b2 /= a0;
		a1 /= a0;
		a2 /= a0;

		float[] filteredSignal = new float[signal.length];
		
		float x1 = 0, x2 = 0, y1 = 0, y2 = 0, sample = 0;
		
		for(int i = 0; i < signal.length; i++){
			sample = b0*signal[i] + b1*x1 + b2*x2 - a1*y1 - a2*y2;
			
			x2 = x1;
			x1 = signal[i];
			y2 = y1;
			y1 = sample;
			filteredSignal[i] = sample;
		}
		long endTime = System.nanoTime();
        //Log.i("filter", ""+(endTime-startTime)/1000000l);
		
		return filteredSignal;
	}
}
