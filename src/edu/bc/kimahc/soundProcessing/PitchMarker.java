package edu.bc.kimahc.soundProcessing;

import edu.bc.kimahc.seniorthesis2013.GlobalAppData;
import android.util.Log;

public class PitchMarker {
	static float f = 0.7f; //constant that controls the search range
	static GlobalAppData global;
	public PitchMarker(){
		
	}
	
	public static float[] pitchMark(float[] signal, float frequency, int fs){
		if(frequency < 0){
			System.out.println("freq < 0: cant pitch mark");
			return null;
		}
		if(signal == null){
			System.out.println("null signal: cant pitch mark");
			return null;
		}
		float[] valley =  pitchMarkValley(signal, frequency, fs);
		float[] peak =  pitchMarkPeak(signal, frequency, fs);
		float valleySum = 0, peakSum = 0, valleyVar = 0, peakVar = 0, valleyAvg, peakAvg, estimatedPeriod;
		float detectedPeriod = (float)fs/frequency;	//gives the period in number of samples

		for(int i = 2; i < valley.length; i=i+2){
			estimatedPeriod = valley[i] - valley[i-2];
			valleySum += estimatedPeriod;
			valleyVar += (detectedPeriod - estimatedPeriod)*(detectedPeriod - estimatedPeriod);	//var is the average of the squared difference
		}
		for(int j = 2; j < peak.length; j=j+2){
			estimatedPeriod = peak[j] - peak[j-2];
			peakSum += estimatedPeriod;
			peakVar += (detectedPeriod - estimatedPeriod)*(detectedPeriod - estimatedPeriod);
		}
		valleyAvg = valleySum/(valley.length/2 - 1); //get average valley period in samples. (n/2 - 1 because we're getting the differences between adj marks)
		peakAvg = peakSum/(peak.length/2 - 1);
		valleyVar = valleyVar/(valley.length/2 - 1);
		peakVar = peakVar/(peak.length/2 - 1);
		
		System.out.println("freq: " + frequency + " period: " + detectedPeriod);
		System.out.println("valley avg: " + valleyAvg + "  peak avg: " + peakAvg);
		System.out.println("valley var: " + valleyVar + "  peak var: " + peakVar);
		global = GlobalAppData.getInstance();
		if(valleyVar > 0)
			global.incValleyVar(valleyVar, (valley.length/2 - 1));
		if(peakVar > 0)
			global.incPeakVar(peakVar, (peak.length/2 - 1));
		
		float valleyAvgAmp = 0, peakAvgAmp = 0;
		for(int i = 1; i < valley.length; i=i+2){
			valleyAvgAmp += valley[i];
		}
		for(int i = 1; i < peak.length; i=i+2){
			peakAvgAmp += peak[i];
		}
		valleyAvgAmp = (-valleyAvgAmp/(valley.length/2));
		peakAvgAmp = (peakAvgAmp/(peak.length/2));
		System.out.println("valleyAvgAmp = " + valleyAvgAmp + " peak avg amp = " + peakAvgAmp);
		/*
		if(Math.abs(frequency - valleyAvg) < Math.abs(frequency - peakAvg)){
			return valley;	//return valley pitch marks if its pitch contour has less average deviation from frequency estimation
		}
		return peak;
		
		//return valley; //just gonna go with valleys for now
		if(peakAvgAmp > valleyAvgAmp){
			return peak;
		}*/
		return valley;
	}
	
	private static float[] pitchMarkValley(float[] signal, float frequency, int fs){
		if(frequency < 0){
			System.out.println("freq < 0: cant pitch mark");
			return null;
		}
		if(signal == null){
			System.out.println("null signal: cant pitch mark");
			return null;
		}
		float[] minValues = getMin(signal);
		float minVal, minIndex;
		float globalMinIndex = minValues[0];
		float globalMinVal = minValues[1];
		
		float pitchPeriod = (float)fs/frequency; //number of samples in one period
		int maxNumPeriods = (int) (signal.length/(f*pitchPeriod)); //maximum number of periods in input signal
		
		float[] mRight = new float[maxNumPeriods+1]; //add 1 to be safe
		float[] mvRight = new float[maxNumPeriods+1]; //add 1 to be safe
		
		int mRightIndex = 0;
		mRight[mRightIndex] = globalMinIndex; //mRight contains marks on right side of global min (includes that min)
		mvRight[mRightIndex] = globalMinVal;  //mvRight contains amplitudes of marks on right side of global min
		
		//System.out.println("mRight size = " + mRight.length + " pitchPer = " + pitchPeriod + " freq: " + frequency);
		//System.out.println("globalMinIndex= " + globalMinIndex + " globalMinVal: " + globalMinVal);
		
		int lowerBound, upperBound;
		while(mRight[mRightIndex] + Math.ceil((2-f)*pitchPeriod) < signal.length){
			lowerBound = (int)(mRight[mRightIndex]+Math.floor(f*pitchPeriod));
			upperBound = (int) (mRight[mRightIndex] + Math.ceil((2-f)*pitchPeriod));
			minValues = getMin(signal, lowerBound, upperBound);
			//System.out.println("lower bound " + lowerBound + "   upp " + upperBound);
			//System.out.println("minIndex " + minValues[0] + "   minValue " + minValues[1]);
			mRight[mRightIndex+1] = minValues[0];
			mvRight[mRightIndex+1] = minValues[1];
			mRightIndex++;
		}
		
		//check to see if there is one more possible marker on the right side
		if(mRightIndex > 0){
			float rightMostPeriodLength = mRight[mRightIndex] - mRight[mRightIndex-1];
			float rightMostAmplitude = mvRight[mRightIndex];
			
			//potential marker can be found close to one period away from the rightmost mark
			if(mRight[mRightIndex] + Math.ceil(0.95*rightMostPeriodLength) < signal.length){
				lowerBound = (int) (mRight[mRightIndex] + Math.floor(f*pitchPeriod));
				upperBound = signal.length-1;
				minValues = getMin(signal,lowerBound, upperBound);
				minIndex = minValues[0];
				minVal = minValues[1];
				
				//check to see that the potential mark is large enough and is not the last point..and that minIndex isn't 0 (default val)
				if(Math.abs(minVal) > 0.9*Math.abs(rightMostAmplitude) && minIndex < upperBound && minIndex > 0){
					mRight[mRightIndex+1] = minIndex;
					mvRight[mRightIndex+1] = minVal;
					mRightIndex++;
				}
			}
		}
		
		//now compute values on left of global minimum
		float[] mLeft = new float[maxNumPeriods+1]; //add 1 to be safe
		float[] mvLeft = new float[maxNumPeriods+1]; //add 1 to be safe
		
		int mLeftIndex = -1;
		if(globalMinIndex - Math.floor((2-f)*pitchPeriod) >= 1){ //can only go left if there's room for at least one period
			lowerBound = (int)(globalMinIndex - Math.floor((2-f)*pitchPeriod));
			upperBound = (int) (globalMinIndex - Math.ceil(f*pitchPeriod));
			minValues = getMin(signal, lowerBound, upperBound);
			mLeft[0] = minValues[0];
			mvLeft[0] = minValues[1];
			mLeftIndex = 0;
			
			while(mLeft[mLeftIndex] - Math.floor((2-f)*pitchPeriod) >= 1){
				lowerBound = (int)(mLeft[mLeftIndex] - Math.floor((2-f)*pitchPeriod));
				upperBound = (int) (mLeft[mLeftIndex] - Math.ceil(f*pitchPeriod));
				minValues = getMin(signal, lowerBound, upperBound);
				mLeft[mLeftIndex+1] = minValues[0];
				mvLeft[mLeftIndex+1] = minValues[1];
				mLeftIndex++;
			}
			
			//check to see if there is one more possible marker on the left side
			if(mLeftIndex > 0){
				float leftMostPeriodLength = mLeft[mLeftIndex] - mLeft[mLeftIndex-1];
				float leftMostAmplitude = mvLeft[mLeftIndex];
				
				//potential marker can be found close to one period away from the leftmost mark
				if(mLeft[mLeftIndex] - Math.floor(0.95*leftMostPeriodLength) > 0){
					lowerBound = 0;
					upperBound = (int) (mLeft[mLeftIndex] - Math.ceil(f*pitchPeriod));
					minValues = getMin(signal,lowerBound, upperBound);
					minIndex = minValues[0];
					minVal = minValues[1];
					
					//check to see that the potential mark is large enough and is not the first point
					if(Math.abs(minVal) > 0.9*Math.abs(leftMostAmplitude) && minIndex > lowerBound){
						mLeft[mLeftIndex+1] = minIndex;
						mvLeft[mLeftIndex+1] = minVal;
						mLeftIndex++;
					}
				}
			}
		}

		
		float[] pitchMarks = new float[(mRightIndex+mLeftIndex+2)*2]; //result will need 2 * number of pitch marks
		for(int i = 0; i < mLeftIndex+1; i++){
			pitchMarks[i*2] = mLeft[mLeftIndex-i]; //must obtain left values backwards
			pitchMarks[i*2+1] = mvLeft[mLeftIndex-i]; //because the previous algorithm works backwards from global min
		}
		int leftOffset;
		if(mLeftIndex == -1){
			leftOffset = 0;
		}
		else{
			leftOffset = mLeftIndex+1; //
		}
		for(int j = 0; j < mRightIndex+1; j++){
			pitchMarks[leftOffset*2 + j*2] = mRight[j];
			pitchMarks[leftOffset*2 + j*2+1] = mvRight[j];
		}
		/*
		Log.i("mRightIndex", "" + mRightIndex);
		Log.i("mLeftIndex", "" + mLeftIndex);
		Log.i("pitch marks", "" + pitchMarks.length);
		*/
		
		/*
		for(int k = 0; k < pitchMarks.length; k++){
			System.out.println(pitchMarks[k]);
		}*/
		return pitchMarks;
	}
	
	
	private static float[] pitchMarkPeak(float[] signal, float frequency, int fs){
		if(frequency < 0){
			System.out.println("freq < 0: cant pitch mark");
			return null;
		}
		if(signal == null){
			System.out.println("null signal: cant pitch mark");
			return null;
		}
		float[] maxValues = getMax(signal);
		float maxVal, maxIndex;
		float globalMaxIndex = maxValues[0];
		float globalMaxVal = maxValues[1];
		
		float pitchPeriod = (float)fs/frequency; //number of samples in one period
		int maxNumPeriods = (int) (signal.length/(f*pitchPeriod)); //maximum number of periods in input signal
		
		float[] mRight = new float[maxNumPeriods+1]; //add 1 to be safe
		float[] mvRight = new float[maxNumPeriods+1]; //add 1 to be safe
		
		int mRightIndex = 0;
		mRight[mRightIndex] = globalMaxIndex; //mRight contains marks on right side of global max (includes that max)
		mvRight[mRightIndex] = globalMaxVal;  //mvRight contains amplitudes of marks on right side of global max
		
		//System.out.println("mRight size = " + mRight.length + " pitchPer = " + pitchPeriod + " freq: " + frequency);
		//System.out.println("globalMaxIndex= " + globalMaxIndex + " globalMaxVal: " + globalMaxVal);
		
		int lowerBound, upperBound;
		while(mRight[mRightIndex] + Math.ceil((2-f)*pitchPeriod) < signal.length){
			lowerBound = (int)(mRight[mRightIndex]+Math.floor(f*pitchPeriod));
			upperBound = (int) (mRight[mRightIndex] + Math.ceil((2-f)*pitchPeriod));
			maxValues = getMax(signal, lowerBound, upperBound);
			//System.out.println("lower bound " + lowerBound + "   upp " + upperBound);
			//System.out.println("maxIndex " + maxValues[0] + "   maxValue " + maxValues[1] + " mRightIndex = " + mRightIndex + " size = " + mRight.length);
			mRight[mRightIndex+1] = maxValues[0];
			mvRight[mRightIndex+1] = maxValues[1];
			mRightIndex++;
		}
		
		//check to see if there is one more possible marker on the right side
		if(mRightIndex > 0){
			float rightMostPeriodLength = mRight[mRightIndex] - mRight[mRightIndex-1];
			float rightMostAmplitude = mvRight[mRightIndex];
			
			//potential marker can be found close to one period away from the rightmost mark
			if(mRight[mRightIndex] + Math.ceil(0.95*rightMostPeriodLength) < signal.length){
				lowerBound = (int) (mRight[mRightIndex] + Math.floor(f*pitchPeriod));
				upperBound = signal.length-1;
				maxValues = getMax(signal,lowerBound, upperBound);
				maxIndex = maxValues[0];
				maxVal = maxValues[1];
				
				//check to see that the potential mark is large enough and is not the last point
				if(Math.abs(maxVal) > 0.9*Math.abs(rightMostAmplitude) && maxIndex < upperBound && maxIndex > 0){
					mRight[mRightIndex+1] = maxIndex;
					mvRight[mRightIndex+1] = maxVal;
					mRightIndex++;
				}
			}
		}
		
		//now compute values on left of global minimum
		float[] mLeft = new float[maxNumPeriods+1]; //add 1 to be safe
		float[] mvLeft = new float[maxNumPeriods+1]; //add 1 to be safe
		
		int mLeftIndex = -1;
		if(globalMaxIndex - Math.floor((2-f)*pitchPeriod) >= 1){ //can only go left if there's room for at least one period
			lowerBound = (int)(globalMaxIndex - Math.floor((2-f)*pitchPeriod));
			upperBound = (int) (globalMaxIndex - Math.ceil(f*pitchPeriod));
			maxValues = getMax(signal, lowerBound, upperBound);
			mLeft[0] = maxValues[0];
			mvLeft[0] = maxValues[1];
			mLeftIndex = 0;
			
			while(mLeft[mLeftIndex] - Math.floor((2-f)*pitchPeriod) >= 1){
				lowerBound = (int)(mLeft[mLeftIndex] - Math.floor((2-f)*pitchPeriod));
				upperBound = (int) (mLeft[mLeftIndex] - Math.ceil(f*pitchPeriod));
				maxValues = getMax(signal, lowerBound, upperBound);
				//System.out.println("lower bound " + lowerBound + "   upp " + upperBound);
				//System.out.println("maxIndex " + maxValues[0] + "   maxValue " + maxValues[1] + " mLeftIndex = " + mLeftIndex + " size = " + mLeft.length);
				
				mLeft[mLeftIndex+1] = maxValues[0];
				mvLeft[mLeftIndex+1] = maxValues[1];
				mLeftIndex++;
			}
			
			//check to see if there is one more possible marker on the left side
			if(mLeftIndex > 0){
				float leftMostPeriodLength = mLeft[mLeftIndex] - mLeft[mLeftIndex-1];
				float leftMostAmplitude = mvLeft[mLeftIndex];
				
				//potential marker can be found close to one period away from the leftmost mark
				if(mLeft[mLeftIndex] - Math.floor(0.95*leftMostPeriodLength) > 0){
					lowerBound = 0;
					upperBound = (int) (mLeft[mLeftIndex] - Math.ceil(f*pitchPeriod));
					maxValues = getMax(signal,lowerBound, upperBound);
					maxIndex = maxValues[0];
					maxVal = maxValues[1];
					
					//check to see that the potential mark is large enough and is not the first point
					if(Math.abs(maxVal) > 0.9*Math.abs(leftMostAmplitude) && maxIndex > lowerBound){
						mLeft[mLeftIndex+1] = maxIndex;
						mvLeft[mLeftIndex+1] = maxVal;
						mLeftIndex++;
					}
				}
			}
		}

		
		float[] pitchMarks = new float[(mRightIndex+mLeftIndex+2)*2]; //result will need 2 * number of pitch marks
		for(int i = 0; i < mLeftIndex+1; i++){
			pitchMarks[i*2] = mLeft[mLeftIndex-i]; //must obtain left values backwards
			pitchMarks[i*2+1] = mvLeft[mLeftIndex-i]; //because the previous algorithm works backwards from global min
		}
		int leftOffset;
		if(mLeftIndex == -1){
			leftOffset = 0;
		}
		else{
			leftOffset = mLeftIndex+1; //
		}
		for(int j = 0; j < mRightIndex+1; j++){
			pitchMarks[leftOffset*2 + j*2] = mRight[j];
			pitchMarks[leftOffset*2 + j*2+1] = mvRight[j];
		}
		
		/*
		for(int k = 0; k < pitchMarks.length; k++){
			System.out.println(pitchMarks[k]);
		}*/
		return pitchMarks;
	}
	
	
	/*
	 * returns a 2 element float array:
	 * float[0] = minimum index
	 * float[1] = minimum value
	 */
	private static float[] getMin(float[] buffer){
		float[] result = new float[2];
		result[0] = 0;
		result[1] = Float.MAX_VALUE;
		if(buffer == null){
			System.out.println("getMin buffer null");
			return null;
		}
		for(int i = 0; i < buffer.length; i++){
			if(buffer[i] < result[1]){
				result[1] = buffer[i];
				result[0] = i;
			}
		}
		return result;
	}
	
	/*
	 * different from matlab version:
	 * returns index based on whole array, not starting from subarray
	 */
	private static float[] getMin(float[] buffer, int start, int end){
		float[] result = new float[2];
		result[0] = 0;
		result[1] = Float.MAX_VALUE;
		if(buffer == null){
			System.out.println("getMin buffer null");
			return null;
		}
		for(int i = start; i <= end; i++){ //can include end point
			if(buffer[i] < result[1]){
				result[1] = buffer[i];
				result[0] = i;
			}
		}
		return result;
	}
	
	private static float[] getMax(float[] buffer){
		float[] result = new float[2];
		result[0] = 0;
		result[1] = -Float.MAX_VALUE;
		if(buffer == null){
			System.out.println("getMax buffer null");
			return null;
		}
		for(int i = 0; i < buffer.length; i++){
			if(buffer[i] > result[1]){
				result[1] = buffer[i];
				result[0] = i;
			}
		}
		return result;
	}
	
	/*
	 * different from matlab version:
	 * returns index based on whole array, not starting from subarray
	 */
	private static float[] getMax(float[] buffer, int start, int end){
		float[] result = new float[2];
		result[0] = 0;
		result[1] = -Float.MAX_VALUE;
		if(buffer == null){
			System.out.println("getMax buffer null");
			return null;
		}
		for(int i = start; i <= end; i++){ //can include end point
			if(buffer[i] > result[1]){
				result[1] = buffer[i];
				result[0] = i;
			}
		}
		return result;
	}
}
