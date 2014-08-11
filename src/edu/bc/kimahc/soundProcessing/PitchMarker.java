package edu.bc.kimahc.soundProcessing;

import java.util.Arrays;

import edu.bc.kimahc.seniorthesis2013.GlobalAppData;
import android.util.Log;

public class PitchMarker {
	static float f = 0.7f; //constant that controls the search range
	static float alpha = 1f; //constant that controls relative importance of state probability
	static float beta = 1f; //constant that fine tunes similarity (between transitions)
	static float gamma = 1f;  //constant that controls relative importance of transition probability
	static float delta = 0.008f; //constant determining minimum difference between adjacent samples to find peaks
	static int numCandidates = 3;
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
			global.incValleyVar(valleyVar, (valley.length/2 - 1), detectedPeriod);
		if(peakVar > 0)
			global.incPeakVar(peakVar, (peak.length/2 - 1), detectedPeriod);
		
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
		
		if(Math.abs(frequency - valleyAvg) < Math.abs(frequency - peakAvg)){
			return valley;	//return valley pitch marks if its pitch contour has less average deviation from frequency estimation
		}
		return peak;
		/*
		//return valley; //just gonna go with valleys for now
		if(peakAvgAmp > valleyAvgAmp){
			return peak;
		}
		return valley;*/
	}
	
	public static float[] pitchMark(float[] signal, float frequency, int fs, float prevFrameMark, float prevFrameAmp){
		if(frequency <= 0){
			System.out.println("freq < 0: cant pitch mark");
			return null;
		}
		if(signal == null){
			System.out.println("null signal: cant pitch mark");
			return null;
		}
		//if(Math.abs(prevFrameMark) > 2*f*fs/frequency || prevFrameAmp == 0){
		if(prevFrameMark < 0 || prevFrameAmp == 0){	
			//if previous mark is too far from beginning of this frame, disregard it and start from beginning
			System.out.println(prevFrameMark + " max search: " + (2*f*fs/frequency));
			return pitchMark(signal, frequency, fs);
		}
		
		// if you want to print var values:
		// pitchMark(signal, frequency, fs);
		
		float marksVar = 0, estimatedPeriod;
		float detectedPeriod = (float)fs/frequency;	//gives the period in number of samples
		float[] marks;
		
		if(prevFrameAmp < 0){
			marks = pitchMarkValleyCont(signal, frequency, fs, prevFrameMark, prevFrameAmp);
		}
		else{
			marks = pitchMarkPeakCont(signal, frequency, fs, prevFrameMark, prevFrameAmp);
		}
		
		
		for(int i = 2; i < marks.length; i=i+2){
			estimatedPeriod = marks[i] - marks[i-2];
			float var = (detectedPeriod - estimatedPeriod)*(detectedPeriod - estimatedPeriod);
			marksVar += (detectedPeriod - estimatedPeriod)*(detectedPeriod - estimatedPeriod);	//var is the average of the squared difference
			//System.out.println("var: " + var + " est per: " + estimatedPeriod + " m1 " + marks[i] + " m2 " + marks[i-2]);
		}

		marksVar = marksVar/(marks.length/2 - 1);

		global = GlobalAppData.getInstance();
		if(marksVar > 0){
			if(prevFrameAmp < 0){
				global.incValleyVar(marksVar, (marks.length/2 - 1), detectedPeriod);
			}
			else{
				global.incPeakVar(marksVar, (marks.length/2 - 1), detectedPeriod);
			}
		}
		return marks;
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
		float globalMinIndex = -signal.length;//-minValues[0];	//make neg so it has a fresh start
		float globalMinVal = minValues[1];
		
		return pitchMarkValleyCont(signal, frequency, fs, globalMinIndex, globalMinVal);
		
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
		float globalMaxIndex = -signal.length;//-maxValues[0];//make this neg to have fresh start
		float globalMaxVal = maxValues[1];
		
		return pitchMarkPeakCont(signal, frequency, fs, globalMaxIndex, globalMaxVal);
	}
	
	
	private static float[] pitchMarkValleyCont(float[] signal, float frequency, int fs, float globalMinIndex, float globalMinVal){
		
		//negate signal array
		float[] negatedSignal = new float[signal.length];
		for(int i = 0; i < signal.length; i++){
			negatedSignal[i] = - signal[i];
		}

		
		//obtain pitch marks for negated signal (with negated global min val)
		float[] pitchMarks = pitchMarkPeakCont(negatedSignal, frequency, fs, globalMinIndex, -globalMinVal);
		
		//negate pitch mark amplitudes
		for(int i = 1; i < pitchMarks.length; i+=2){
			pitchMarks[i] = -pitchMarks[i];
		}
		
		return pitchMarks;
	}
	
	private static float[] pitchMarkPeakCont(float[] signal, float frequency, int fs, float globalMaxIndex, float globalMaxVal){
		
		float maxVal, maxIndex;
		float[] maxValues;
		
		float pitchPeriod = (float)fs/frequency; //number of samples in one period
		int maxNumPeriods = (int) (signal.length/(f*pitchPeriod)); //maximum number of periods in input signal... can add 0.5 isntead of1
		
		float[] mRight = new float[maxNumPeriods+1]; //add 1 to be safe
		float[] mvRight = new float[maxNumPeriods+1]; //add 1 to be safe
		
		int[][] candidates = new int[maxNumPeriods+1][numCandidates];	//candidates[i][j] represents the peak index of candidate j at region i
		float[] candidateAmplitudes = new float[numCandidates];	//temp array for the amplitudes of each candidate in a specific region
		float[][] stateProbs = new float[maxNumPeriods+1][numCandidates];
		float[][] accumProbs = new float[maxNumPeriods+1][numCandidates];
		float[][][] transProbs = new float[maxNumPeriods+1][numCandidates][numCandidates]; //each region has a nxn matrix
		int[][] backTrack = new int[maxNumPeriods+1][numCandidates];
		int regionNum = 0;
		
		int mRightIndex = 0;
		int lowerBound, upperBound;
		float[] max = getMax(signal);
		float[] min = getMin(signal);
		float globalMaxValue = max[1]; //required for state probabilities
		float globalMinValue = min[1]; //required for state probabilities
		boolean newStart = false;
		if(globalMaxIndex < 0 || globalMaxIndex > signal.length-1){	//this is new start

			maxValues = getMax(signal);
			globalMaxIndex = maxValues[0];
			globalMaxVal = maxValues[1];
			newStart = true;
			//candidates[0] = getMaxPeaksIndexes(signal, numCandidates, delta);	//find the best n candidate peaks
		}
		
		for(int i = 0; i < numCandidates; i++){
			candidates[0][i] = (int) globalMaxIndex;//start from the global max 
		}
		mRight[mRightIndex] = globalMaxIndex; //mRight contains marks on right side of global max (includes that max)
		mvRight[mRightIndex] = globalMaxVal;  //mvRight contains amplitudes of marks on right side of global max

		for(int i = 0; i < numCandidates; i++){
			candidateAmplitudes[i] = signal[candidates[0][i]];
			//System.out.println(i + " reg 0: index " + candidates[0][i] + " amp: " + candidateAmplitudes[i]);
		}
		
		stateProbs[0] = getStateProbabilities(candidateAmplitudes, globalMinValue, globalMaxValue);	//find the state probabilities of the candidates in region 0
		accumProbs[0] = stateProbs[0];	//initial accumulated probabilities are just the state probabilities of the initial region
		
		for(int i = 0; i < numCandidates; i++){
			//Log.i("state probs", i+" amp " + candidateAmplitudes[i] + " prob " + stateProbs[0][i]);
		}
		
		
		//System.out.println("mRight size = " + mRight.length + " pitchPer = " + pitchPeriod + " freq: " + frequency);
		//System.out.println("globalMaxIndex= " + globalMaxIndex + " globalMaxVal: " + globalMaxVal);
		
		float maxProb = -1f, combinedProb = 0;
		int maxProbIndex = -1;
		while(mRight[mRightIndex] + Math.ceil((2-f)*pitchPeriod) < signal.length){
			lowerBound = (int)(mRight[mRightIndex]+Math.floor(f*pitchPeriod));
			upperBound = (int) (mRight[mRightIndex] + Math.ceil((2-f)*pitchPeriod));
			
			regionNum++;	//increment search region counter
			if(regionNum+1> candidates.length){
				System.out.println("region: " + regionNum + " cand length = " + candidates.length);
				System.out.println("max per: " + maxNumPeriods + " period: " + pitchPeriod + " sig len: " + signal.length);
				System.out.println("up : " + upperBound + " low: " + lowerBound);
				for(int i = 0; i < regionNum; i++){
					System.out.println("cands: " + candidates[i][0]);
				}
			}
			candidates[regionNum] = getMaxPeaksIndexes(signal, lowerBound, upperBound, numCandidates, delta);
			
			for(int i = 0; i < numCandidates; i++){
				candidateAmplitudes[i] = signal[candidates[regionNum][i]];
				//System.out.println(i + " reg:" + regionNum + " index " + candidates[regionNum][i] + " amp: " + candidateAmplitudes[i]);
			}
			stateProbs[regionNum] = getStateProbabilities(candidateAmplitudes, globalMinValue, globalMaxValue);

			for(int i = 0; i < numCandidates; i++){
				//Log.i("state probs", i+" amp " + candidateAmplitudes[i] + " prob " + stateProbs[regionNum][i]);
			}
			//for each candidate in this search region, find the accumulated probabilities
			for(int j = 0; j < numCandidates; j++){	//j represents candidates from this region
				
				//find the max combination of accumulated and transition probability from the candidates in the previous region to the current region
				for(int k = 0; k < numCandidates; k++){	//k represents candidates from previous region
					transProbs[regionNum][k] = getTransitionProb(frequency, candidates[regionNum-1][k], candidates[regionNum], fs);
					combinedProb = accumProbs[regionNum-1][k] + transProbs[regionNum][k][j];	//transition prob from k to j
					if(combinedProb > maxProb){
						maxProb = combinedProb;
						maxProbIndex = k;
					}
					//Log.i("trans", "reg:" + regionNum+" prev k:" + k + " to j:" + j + " " + transProbs[regionNum][k][j]);
					
				}
				
				//the k index giving the max probability is now found
				accumProbs[regionNum][j] = maxProb + stateProbs[regionNum][j];	//add j's state probability to the best accumulated path to j
				backTrack[regionNum][j] = maxProbIndex;	//store the previous candidate that gave the best combined probability
				
				maxProb = -1f;
				maxProbIndex = -1;
				
				//Log.i("accum", "j:" + j + " " + accumProbs[regionNum][j]);
				
			}
						
			
			/*
			maxValues = getMax(signal, lowerBound, upperBound);
			//System.out.println("lower bound " + lowerBound + "   upp " + upperBound);
			//System.out.println("maxIndex " + maxValues[0] + "   maxValue " + maxValues[1] + " mRightIndex = " + mRightIndex + " size = " + mRight.length);
			mRight[mRightIndex+1] = maxValues[0];
			mvRight[mRightIndex+1] = maxValues[1];
			*/
			
			mRight[mRightIndex+1] = candidates[regionNum][0];	//save index of largest peak in search area
			mvRight[mRightIndex+1] = signal[candidates[regionNum][0]];	//save value of largest peak in search area
			mRightIndex++;
		}
		
		
		//find the highest accumulated probability
		for(int j = 0; j < numCandidates; j++){	//j represents candidates in the last region
			//Log.i("accum", "" + j + ": " + accumProbs[regionNum][j]);
			if(accumProbs[regionNum][j] > maxProb){
				maxProb = accumProbs[regionNum][j];
				maxProbIndex = j;
				
			}
		}	
		
		float[] rightMarks = new float[(regionNum+1)*2];
		int[] indexes = new int[regionNum+1];
		int prevIndex = 0;
		
		for(int i = regionNum; i >= 0; i--){
			indexes[i] = candidates[i][maxProbIndex];		//verify this
			maxProbIndex = backTrack[i][maxProbIndex];
			
		}
		for(int i = 0; i < regionNum+1; i++){
			rightMarks[i*2] = indexes[i];
			rightMarks[i*2+1] = signal[indexes[i]];
		}
		
		
		
		
		/*
		
		
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
				if(Math.abs(maxVal) > 0.8*Math.abs(rightMostAmplitude) && maxIndex < upperBound && maxIndex > 0){
					mRight[mRightIndex+1] = maxIndex;
					mvRight[mRightIndex+1] = maxVal;
					mRightIndex++;
				}
			}
		}
		*/
		


		
		
		
		float[] mLeft = new float[maxNumPeriods+1]; //add 1 to be safe
		float[] mvLeft = new float[maxNumPeriods+1]; //add 1 to be safe
		
		int mLeftIndex = -1;
		regionNum = -1;
		if(globalMaxIndex - Math.floor((2-f)*pitchPeriod) >= 1 && newStart){ //can only go left if there's room for at least one period
			lowerBound = (int)(globalMaxIndex - Math.floor((2-f)*pitchPeriod));	
			upperBound = (int) (globalMaxIndex - Math.ceil(f*pitchPeriod));
			maxValues = getMax(signal, lowerBound, upperBound);
			mLeft[0] = maxValues[0];
			mvLeft[0] = maxValues[1];
			mLeftIndex = 0;
			
			//reinitialize all probabilities and candidates back to 0
			for (int[] row : candidates)
				Arrays.fill(row, 0);
			for (int[] row : backTrack)
				Arrays.fill(row, 0);
			
			Arrays.fill(candidateAmplitudes, 0);
			
			for (float[] row : stateProbs)
				Arrays.fill(row, 0);
			for (float[] row : accumProbs)
				Arrays.fill(row, 0);
			
			for (float[][] cands : transProbs)
				for (float[] row : cands)
					Arrays.fill(row, 0);
			

			regionNum = 0;
			maxProb = -1f;
			combinedProb = 0;
			maxProbIndex = -1;
			for(int i = 0; i < numCandidates; i++){
				candidates[0][i] = (int) globalMaxIndex;//start from the global max 
			}
			mLeft[mLeftIndex] = globalMaxIndex; //mRight contains marks on right side of global max (includes that max)
			mvLeft[mLeftIndex] = globalMaxVal;  //mvRight contains amplitudes of marks on right side of global max

			for(int i = 0; i < numCandidates; i++){
				candidateAmplitudes[i] = signal[candidates[0][i]];
			}
			
			stateProbs[0] = getStateProbabilities(candidateAmplitudes, globalMinValue, globalMaxValue);	//find the state probabilities of the candidates in region 0
			accumProbs[0] = stateProbs[0];	//initial accumulated probabilities are just the state probabilities of the initial region

			while(mLeft[mLeftIndex] - Math.floor((2-f)*pitchPeriod) >= 1){
				lowerBound = (int)(mLeft[mLeftIndex] - Math.floor((2-f)*pitchPeriod));
				upperBound = (int) (mLeft[mLeftIndex] - Math.ceil(f*pitchPeriod));
				regionNum++;	//increment search region counter
				if(regionNum+1 > candidates.length){
					System.out.println("region: " + regionNum + " cand length = " + candidates.length);
					System.out.println("max per: " + maxNumPeriods + " period: " + pitchPeriod + " sig len: " + signal.length);
					System.out.println("up : " + upperBound + " low: " + lowerBound);
					for(int i = 0; i < regionNum; i++){
						System.out.println("cands: " + candidates[i][0]);
					}
				}
				candidates[regionNum] = getMaxPeaksIndexes(signal, lowerBound, upperBound, numCandidates, delta);
				
				for(int i = 0; i < numCandidates; i++){
					candidateAmplitudes[i] = signal[candidates[regionNum][i]];
				}
				stateProbs[regionNum] = getStateProbabilities(candidateAmplitudes, globalMinValue, globalMaxValue);
				

				
				//for each candidate in this search region, find the accumulated probabilities
				for(int j = 0; j < numCandidates; j++){	//j represents candidates from this region
					
					//find the max combination of accumulated and transition probability from the candidates in the previous region to the current region
					for(int k = 0; k < numCandidates; k++){	//k represents candidates from previous region
						transProbs[regionNum][k] = getTransitionProb(frequency, candidates[regionNum-1][k], candidates[regionNum], fs);
						combinedProb = accumProbs[regionNum-1][k] + transProbs[regionNum][k][j];	//transition prob from k to j
						
						
						
						
						if(combinedProb > maxProb){
							maxProb = combinedProb;
							maxProbIndex = k;
						}
					}
					
					//the k index giving the max probability is now found
					accumProbs[regionNum][j] = maxProb + stateProbs[regionNum][j];	//add j's state probability to the best accumulated path to j
					backTrack[regionNum][j] = maxProbIndex;	//store the previous candidate that gave the best combined probability
					
					maxProb = -1f;
					maxProbIndex = -1;
				}

				mLeft[mLeftIndex+1] = candidates[regionNum][0];	//save index of largest peak in search area
				mvLeft[mLeftIndex+1] = signal[candidates[regionNum][0]];	//save value of largest peak in search area
				mLeftIndex++;
			}
		}
		


		if(regionNum >= 0){
			//find the highest accumulated probability
			for(int j = 0; j < numCandidates; j++){	//j represents candidates in the last region
				if(accumProbs[regionNum][j] > maxProb){
					maxProb = accumProbs[regionNum][j];
					maxProbIndex = j;
				}
			}	
			indexes = new int[regionNum+1];
			
			for(int i = regionNum; i >= 0; i--){
				indexes[i] = candidates[i][maxProbIndex];
				maxProbIndex = backTrack[i][maxProbIndex];
				
			}
			for(int i = 0; i < regionNum; i++){	//skip the first value (global max)
				mLeft[i] = indexes[i+1];
				mvLeft[i] = signal[indexes[i+1]];
			}
			mLeftIndex--;
		}
		
		float[] pitchMarks = new float[rightMarks.length + (mLeftIndex+1)*2];
		for(int i = 0; i < mLeftIndex+1; i++){
			//Log.i("left p:", ""+ mLeft[mLeftIndex-i]);
			pitchMarks[i*2] = mLeft[mLeftIndex-i]; //must obtain left values backwards
			pitchMarks[i*2+1] = mvLeft[mLeftIndex-i]; //because the previous algorithm works backwards from global min
		}
		System.arraycopy(rightMarks, 0, pitchMarks, (mLeftIndex+1)*2, rightMarks.length);
		/*
		
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
		
		*/
		/*
		for(int k = 0; k < pitchMarks.length; k++){
			System.out.println("index: " + pitchMarks[k] + " amp: " + signal[(int) pitchMarks[k]]);
		}*/
		return pitchMarks;
	}

	/*
	 * returns a 2 element float array:
	 * float[0] = minimum index
	 * float[1] = minimum value
	 */
	public static float[] getMin(float[] buffer){
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
	public static float[] getMin(float[] buffer, int start, int end){
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
	
	public static float[] getMax(float[] buffer){
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
	public static float[] getMax(float[] buffer, int start, int end){
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
	
	/*
	 * returns n element array of the positions of the max n peaks (n being numPeaks) in the signal from [start, end]
	 * delta = constant determining minimum difference between adjacent samples to find peaks
	 * the first element is of the largest peak index
	 */
	
	public static int[] getMaxPeaksIndexes(float[] buffer, int start, int end, int numPeaks, float delta){
		if(numPeaks == 1){
			float[] max = getMax(buffer, start, end);
			int[] ind = new int[1];
			ind[0] = (int) max[0];
			return ind;
		}
		
		if(buffer == null)
			return null;
		
		float current;
		int[] maxPeakIndex = new int[numPeaks];
		float[] maxPeakVal = new float[numPeaks];
		
		int numPeaksFound = 0;
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		int minPos = -1;
		int maxPos = -1;
		
		int lowestPeakLocation = 0;	//the index of the lowest peak value in the maxPeakVal array
		float lowestPeakVal = Float.MAX_VALUE;

		
		boolean lookForMax = true;

		for(int i = start; i <= end; i++){ //can include end point
			current = buffer[i];
			if(current > max){
				max = current;
				maxPos = i;
			}
			if(current < min){
				min = current;
				minPos = i;
			}
			
			if(lookForMax){
				if(current < max - delta){	//if sample to left is larger than itself by delta, mark it as a peak
					if(numPeaksFound < numPeaks){	//limit of number of peaks hasn't been reached yet

						if(max < lowestPeakVal){	// save lowest peak value and index (used for pruning later, if necessary)
							lowestPeakVal = max;
							lowestPeakLocation = numPeaksFound;
						}
						maxPeakIndex[numPeaksFound] = maxPos;
						maxPeakVal[numPeaksFound] = max;
						numPeaksFound++;
					}
					else{	//limit has been reached
						if(max > lowestPeakVal){	//replace lowest peak found (if it is larger)
							maxPeakIndex[lowestPeakLocation] = maxPos;
							maxPeakVal[lowestPeakLocation] = max;
							
							//update the new lowest peak location/value
							lowestPeakLocation = 0;
							for(int j = 0; j < maxPeakVal.length; j++){
								if(maxPeakVal[j] < maxPeakVal[lowestPeakLocation]){
									lowestPeakLocation = j;
									lowestPeakVal = maxPeakVal[j];
								}
							}
						}
					}
					
					min = current;
					minPos = i;
					lookForMax = false;
				}
			}
			else{
				if(current > min + delta){
					max = current;
					maxPos = i;
					lookForMax = true;
				}
			}
		}


		int highestPeakIndex = 0;	//index of the highest peak
		float highestPeakVal = -Float.MAX_VALUE; //value of the highest peak
		int highestPeakLocation = 0; //location of the highest peak index in the maxPeakIndex array
		for(int j = 0; j < numPeaksFound; j++){

			if(maxPeakVal[j] > highestPeakVal){
				highestPeakVal = maxPeakVal[j];
				highestPeakIndex = maxPeakIndex[j];
				highestPeakLocation = j;
			}
		}
		
		//make sure that the first entry is the index of the highest peak
		if(numPeaks > 1){
			maxPeakIndex[highestPeakLocation] = maxPeakIndex[0];
			maxPeakIndex[0] = highestPeakIndex;
		}
		if(numPeaksFound == 0){	//if no peaks were found, use the global max of the region
			float[] singleMax = getMax(buffer, start, end);
			maxPeakIndex[0] = (int) singleMax[0];
			numPeaksFound++;
		}
		while(numPeaksFound < numPeaks){	//fill up remaining spots with the strongest peak
			maxPeakIndex[numPeaksFound] = maxPeakIndex[0];
			numPeaksFound++;
		}
		/*
		for(int i = 0; i < numPeaks; i++){
			Log.i("peaks", "" + maxPeakIndex[i] + ", " + buffer[maxPeakIndex[i]]);
		}*/
		return maxPeakIndex;

	}
	
	public static int[] getMaxPeaksIndexes(float[] buffer, int numPeaks, float delta){
		return getMaxPeaksIndexes(buffer, 0, buffer.length-1, numPeaks, delta);
	}
	/*
	 * get state probabilities of pitch mark candidates
	 * these are correlated with amplitude height
	 */
	private static float[] getStateProbabilities(float[] candidateAmplitudes, float min, float max){
		if(candidateAmplitudes == null){
			return null;
		}
		int n = candidateAmplitudes.length;
		float[] probabilities = new float[n];
		float sum = 0;
		for(int i = 0; i < n; i++){
			probabilities[i] = (Math.abs(candidateAmplitudes[i]) - min)/(max - min);
			sum += alpha*probabilities[i];
		}
		for(int i = 0; i < n; i++){
			//probabilities[i] = alpha*probabilities[i]/sum;	//can adjust relative importance with alpha
		}
		return probabilities;
	}
	
	private static float getTransitionProb(float freq, int distance, int SR){
		return 1f / (1f + beta*Math.abs(freq - (float) SR / (float) distance));
	}
	
	private static float[] getTransitionProb(float freq, int prevCand, int[] nextCand, int SR){
		
		float sum = 0;
		float[] transitionProbs = new float[nextCand.length];
		for(int i = 0; i < nextCand.length; i++){
			transitionProbs[i] = getTransitionProb(freq, (int) Math.abs(nextCand[i] - prevCand), SR);
			sum += gamma*transitionProbs[i];
		}
		
		//normalize
		for(int i = 0; i < nextCand.length; i++){
			//transitionProbs[i] = gamma*transitionProbs[i]/sum;
		}
		return transitionProbs;
	}
	
}
