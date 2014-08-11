package edu.bc.kimahc.soundProcessing;

import android.util.Log;

public class PitchShifter {
	private int lastSynthesisMark;
	public PitchShifter(){
		lastSynthesisMark = 0;
	}
	
	public int getLastSynthesisMark(){
		return lastSynthesisMark;
	}
	public static float[] pitchShift(float[] signal, float[] pitchMarks, float alpha, float shiftAmount, float gamma, 
			int combine, int SR, int lowFreqCutoff, int highFreqCutoff){
		// gamma newFormantFreq/oldFormantFreq
		
		/*
		if(shiftAmount == 0){
			return signal;
		}*/

		float[] shiftedSignal = new float[signal.length];
		float shiftMultiplier = (float) Math.pow(2, (shiftAmount/12));
		int numPitchMarks = pitchMarks.length/2;	//pitchMarks contains pairs (amplitude and position)
		int signalLength = signal.length;
		if(numPitchMarks < 2){
			Log.i("pitch shifter", numPitchMarks + " is not enough pitch marks");
			return null;
		}
		
		int[] origMarks = new int[numPitchMarks];
		for(int i = 0; i < numPitchMarks; i++){
			origMarks[i] = (int)pitchMarks[i*2];
			//Log.i("start", ""+origMarks[i]);
		}
		
		int maxPeriod = SR/lowFreqCutoff;
		int minPeriod = SR/highFreqCutoff;
		
		
		
		/*
		//obtain the synthesis pitch marks that are on the left of the first pitch mark
		float currentPeriod = origMarks[1] - origMarks[0];
		float currentShiftedPeriod = currentPeriod/shiftMultiplier;
		int maxMarksOnLeft = (int)(currentPeriod / currentShiftedPeriod);	//the largest number of synthesis pitch marks needed on the left of the first pitch mark
		float[] leftSideMarks = null;
		if(maxMarksOnLeft > 0){
			leftSideMarks = new float[maxMarksOnLeft];
			for(int i = 0; i < maxMarksOnLeft; i++){
				leftSideMarks[i] = origMarks[0] - (maxMarksOnLeft-i)*currentShiftedPeriod;
			}
		}
		while(origMarks[0] - count*currentShiftedPeriod >= 0){//must not be negative
			
			Math.round
		}*/


		//float[] shiftedMarks = new float
		
		
		int leftMark = origMarks[0];
		int rightMark = origMarks[0];
		int closestPitchMark = origMarks[0];
		int synthesisPitchMark = closestPitchMark;	//start the synthesis marks at the first pitch mark
		int currentPeriod = origMarks[1] - origMarks[0];
		int endOfUnmarkedSection = origMarks[0] - currentPeriod;
		
		/* if start of pitch marking is more than a period away from the start of the signal
		 * copy the beginning of the signal up to a period away from the first mark
		 */
		if(endOfUnmarkedSection > 0){
			System.arraycopy(signal, 0, shiftedSignal, 0, endOfUnmarkedSection);
			shiftedSignal = olaRightHalfHanning(endOfUnmarkedSection, signal, shiftedSignal, leftMark, currentPeriod);
		}
		
		shiftedSignal = overlapAndAddSegment(signal, shiftedSignal, leftMark, rightMark, closestPitchMark, synthesisPitchMark, currentPeriod);
		

		int currentShiftedPeriod = Math.round(currentPeriod/shiftMultiplier);

		/*
		Log.i("left side", "current period " + currentPeriod);
		Log.i("left side", "first pitch mark " + closestPitchMark);
		Log.i("left side", "current shifted period " + currentShiftedPeriod);
		Log.i("left side", "synth mark " + synthesisPitchMark);
		*/
		/*
		while(synthesisPitchMark-currentShiftedPeriod >= 0 ){	//loop until all the synthesis marks on the left side of the first pitch mark are taken care of
			synthesisPitchMark -= currentShiftedPeriod;
			Log.i("left side", "synth mark " + synthesisPitchMark);
			shiftedSignal = overlapAndAddSegment(signal, shiftedSignal, leftMark, rightMark, closestPitchMark, synthesisPitchMark, currentPeriod);
		}
		*/ //leaving out left side for now
		
		
		//now search between the right of the first pitch mark and the left of the last pitch mark (inclusive)
		synthesisPitchMark = closestPitchMark;
		for(int rightMarkCount = 1; rightMarkCount < numPitchMarks; rightMarkCount++){
			leftMark = origMarks[rightMarkCount-1]; //always search in between left and right marks (and including right side only)
			rightMark = origMarks[rightMarkCount];
			currentPeriod = rightMark - leftMark;								//difference between regular pitch marks
			
			if(currentPeriod < 0){	//this only happens if there's an error...i think it's gone now though
				Log.i("PitchShifter", "current period is neg: " + currentPeriod);
				FastYin fastYin = new FastYin(SR, signal.length);
				float frequency = fastYin.getPitch(signal);
				float[] newMarks = PitchMarker.pitchMark(signal,frequency,SR);
				for(int i = 0; i < origMarks.length; i++){
					System.out.println(origMarks[i] + "  new: " + newMarks[i*2]);
					System.out.println(pitchMarks[i*2+1] + "  new: " + newMarks[i*2+1]);
				}
				
				//currentPeriod = - currentPeriod;
			}
			
			//if there is a large break between pitch marks, treat this area as unvoiced segment (likely due to inability to find f0)
			if(currentPeriod > maxPeriod ){
				//add first part of unvoiced segment
				int startOfUnmarkedSection;
				int hanningWidth;
				if(rightMarkCount - 2 > 0 && (leftMark - origMarks[rightMarkCount-2] < maxPeriod)){
					hanningWidth = leftMark - origMarks[rightMarkCount-2];
				}
				else{
					hanningWidth = minPeriod;
				}
				
				startOfUnmarkedSection = leftMark + hanningWidth;	
				shiftedSignal = olaLeftHalfHanning(startOfUnmarkedSection, signal, shiftedSignal, leftMark, hanningWidth);
				
				
				
				if(rightMarkCount+1 < numPitchMarks && (origMarks[rightMarkCount+1] - rightMark) < maxPeriod){
					hanningWidth = origMarks[rightMarkCount+1] - rightMark; // subtract next period from right mark
				}
				else{
					hanningWidth = minPeriod;
				}
				
				endOfUnmarkedSection = rightMark - hanningWidth;
				shiftedSignal = olaRightHalfHanning(endOfUnmarkedSection, signal, shiftedSignal, rightMark, hanningWidth);
				
				if(endOfUnmarkedSection < startOfUnmarkedSection){
					int temp = startOfUnmarkedSection;
					startOfUnmarkedSection = endOfUnmarkedSection;
					endOfUnmarkedSection = temp;
				}
				
				System.arraycopy(signal, startOfUnmarkedSection, shiftedSignal, startOfUnmarkedSection, (endOfUnmarkedSection - startOfUnmarkedSection));

				synthesisPitchMark = rightMark;
			}
			currentShiftedPeriod = Math.round(currentPeriod/shiftMultiplier);	//difference between synthesis marks
			
			/*
			Log.i("mid", "current period " + currentPeriod);
			Log.i("mid", "current shifted period " + currentShiftedPeriod);
			Log.i("mid", "closest mark " + closestPitchMark);
			Log.i("mid", "left mark " + leftMark);
			Log.i("mid", "right mark " + rightMark);
			*/
			while(synthesisPitchMark+currentShiftedPeriod <= rightMark){ //loop to compute all synthesis marks between left and right marks(including right)


				synthesisPitchMark += currentShiftedPeriod;
				//Log.i("mid", "synth mark " + synthesisPitchMark);
				closestPitchMark = getClosestPitchMark(leftMark, rightMark, synthesisPitchMark);
				//Log.i("mid", "new closest mark " + closestPitchMark);
				shiftedSignal = overlapAndAddSegment(signal, shiftedSignal, leftMark, rightMark, closestPitchMark, synthesisPitchMark, currentPeriod);
			}
			
		}

		/*
		//now search on the right of the last pitch mark
		closestPitchMark = origMarks[numPitchMarks-1];
		Log.i("right side", "current period " + currentPeriod);
		Log.i("right side", "last pitch mark " + closestPitchMark);
		Log.i("right side", "current shifted period " + currentShiftedPeriod);
		
		while(synthesisPitchMark+currentShiftedPeriod < signal.length){	//loop until all the synthesis marks on the left side of the first pitch mark are taken care of
			synthesisPitchMark += currentShiftedPeriod;
			Log.i("right side", "synth mark " + synthesisPitchMark);
			shiftedSignal = overlapAndAddSegment(signal, shiftedSignal, leftMark, rightMark, closestPitchMark, synthesisPitchMark, currentPeriod);
		}
		
		*/ 

		
		//add one period of left side of hanning from rest of signal after the last pitch mark (if there is room) and then copy the rest of the signal
		int startOfUnmarkedSection = rightMark+currentPeriod;
		
		if(startOfUnmarkedSection < signal.length){

			System.arraycopy(signal, startOfUnmarkedSection, shiftedSignal, startOfUnmarkedSection, (signal.length - startOfUnmarkedSection));
			shiftedSignal = olaLeftHalfHanning(startOfUnmarkedSection, signal, shiftedSignal, rightMark, currentPeriod);
			
		}
		
		
		
		
		
		
		for(int i = 0; i < shiftedSignal.length; i++){
			if(shiftMultiplier > 1)
				shiftedSignal[i] = shiftedSignal[i]/shiftMultiplier;	
			shiftedSignal[i] = shiftedSignal[i]*((100-combine)/100f) + signal[i]*(combine/100f);
		}
		
		return shiftedSignal;
	}
	
	public float[] pitchShiftContAuto(float[] prevSignal, float[] currentSignal, float[] prevShiftedSignal, float[] pitchMarks, int lastPitchMark, int lastSynthMark, float alpha, float shiftAmount, float gamma, 
			int combine, int SR, int lowFreqCutoff, int highFreqCutoff, float autotuneFreq){
		float[] autoReg = pitchShiftCont(prevSignal, currentSignal, prevShiftedSignal, pitchMarks, lastPitchMark, lastSynthMark, alpha, 0, gamma, 
				0, SR, lowFreqCutoff, highFreqCutoff, autotuneFreq);
		if(shiftAmount == 0 || combine == 100){
			return autoReg;
		}
		float shiftMultiplier = (float) Math.pow(2, (shiftAmount/12));
		float shiftedAutotuneFreq = autotuneFreq * shiftMultiplier;
		float[] shiftedReg = pitchShiftCont(prevSignal, currentSignal, prevShiftedSignal, pitchMarks, lastPitchMark, lastSynthMark, alpha, 0, gamma, 
				0, SR, lowFreqCutoff, highFreqCutoff, shiftedAutotuneFreq);
		for(int i = 0; i < autoReg.length; i++){
			shiftedReg[i] = shiftedReg[i]*((100-combine)/100f) + autoReg[i]*(combine/100f);
		}
		return shiftedReg;
	}
	
	
	/*	is given a portion of the previous signal and the full current signal, along with the portion of the shifted signal 
	 * which has already been shifted up to the 'lastSynthMark' using up to the first pitch mark
	 * 	returns a shifted signal that is shifted up to the last included pitch mark.
	 */
	public float[] pitchShiftCont(float[] prevSignal, float[] currentSignal, float[] prevShiftedSignal, float[] pitchMarks, int lastPitchMark, int lastSynthMark, float alpha, float shiftAmount, float gamma, 
			int combine, int SR, int lowFreqCutoff, int highFreqCutoff, float autotuneFreq){

		float[] signal = new float[prevSignal.length + currentSignal.length];
		System.arraycopy(prevSignal, 0, signal, 0, prevSignal.length);	//copy previous signal into first section
		System.arraycopy(currentSignal, 0, signal, prevSignal.length, currentSignal.length); //copy current signal into second section
		

		
		float[] shiftedSignal = new float[prevShiftedSignal.length + currentSignal.length];
		System.arraycopy(prevShiftedSignal, 0, shiftedSignal, 0, prevShiftedSignal.length); //fill first half
		

		float shiftMultiplier = (float) Math.pow(2, (shiftAmount/12));
		int numPitchMarks = pitchMarks.length/2;	//pitchMarks contains pairs (amplitude and position)
		int signalLength = signal.length;
		if(numPitchMarks < 2){
			Log.i("pitch shifter", numPitchMarks + " is not enough pitch marks");
			return null;
		}
		int[] origMarks;
		
		origMarks = new int[numPitchMarks];
		for(int i = 0; i < numPitchMarks; i++){
			origMarks[i] = (int)pitchMarks[i*2];
		}

		
		int maxPeriod = SR/lowFreqCutoff;
		int minPeriod = SR/highFreqCutoff;
		int leftMark, rightMark, closestPitchMark, synthesisPitchMark, currentPeriod, endOfUnmarkedSection, currentShiftedPeriod, autotunePeriod;
		autotunePeriod = (int) Math.round(SR/autotuneFreq/shiftMultiplier);
		
		if(lastPitchMark <= 0 || lastSynthMark <= 0){	//if first half is unmarked
			leftMark = origMarks[0];
			rightMark = origMarks[0];
			closestPitchMark = origMarks[0];
			synthesisPitchMark = closestPitchMark;	//start the synthesis marks at the first pitch mark
			currentPeriod = origMarks[1] - origMarks[0];
			endOfUnmarkedSection = origMarks[0] - currentPeriod;
			
			/* if start of pitch marking is more than a period away from the start of the signal
			 * copy the beginning of the signal up to a period away from the first mark
			 */
			if(endOfUnmarkedSection > 0){
				System.arraycopy(signal, 0, shiftedSignal, 0, endOfUnmarkedSection);
				shiftedSignal = olaRightHalfHanning(endOfUnmarkedSection, signal, shiftedSignal, leftMark, currentPeriod);
			}
			
			shiftedSignal = overlapAndAddSegment(signal, shiftedSignal, leftMark, rightMark, closestPitchMark, synthesisPitchMark, currentPeriod);
		}
		else{	//first half has been previously marked and previous synth marks havent been set shifted
			leftMark = origMarks[0];
			rightMark = origMarks[1];
			closestPitchMark = origMarks[0];
			synthesisPitchMark = lastSynthMark;	//start the synthesis mark from the lastSynthMark
			currentPeriod = origMarks[1] - origMarks[0];
			//System.out.println("last synth mark: " + lastSynthMark);
		}


		

		if(autotunePeriod > 0){
			currentShiftedPeriod = autotunePeriod;
		}else{
			currentShiftedPeriod = Math.round(currentPeriod/shiftMultiplier);
		}
		

		/*
		Log.i("left side", "current period " + currentPeriod);
		Log.i("left side", "first pitch mark " + closestPitchMark);
		Log.i("left side", "current shifted period " + currentShiftedPeriod);
		Log.i("left side", "synth mark " + synthesisPitchMark);
		*/

		
		//now search between the right of the first pitch mark and the left of the last pitch mark (inclusive)
		for(int rightMarkCount = 1; rightMarkCount < origMarks.length; rightMarkCount++){
			leftMark = origMarks[rightMarkCount-1]; //always search in between left and right marks (and including right side only)
			rightMark = origMarks[rightMarkCount];
			currentPeriod = rightMark - leftMark;								//difference between regular pitch marks
			
			if(currentPeriod < 0){	//this only happens if there's an error...i think it's gone now though
				Log.i("PitchShifter", "current period is neg: " + currentPeriod);
				Log.i("left", "" + leftMark);
				Log.i("right", "" + rightMark);
				FastYin fastYin = new FastYin(SR, currentSignal.length);
				float frequency = fastYin.getPitch(currentSignal);
				float[] newMarks = PitchMarker.pitchMark(signal,frequency,SR);
				for(int i = 0; i < origMarks.length; i++){
					System.out.println(origMarks[i] + "  new: " + newMarks[i*2]);
					System.out.println(pitchMarks[i*2+1] + "  new: " + newMarks[i*2+1]);
				}
				
				//currentPeriod = - currentPeriod;
			}
			
			//if there is a large break between pitch marks, treat this area as unvoiced segment (likely due to inability to find f0)
			if(currentPeriod > maxPeriod ){
				//add first part of unvoiced segment
				int startOfUnmarkedSection;
				int hanningWidth;
				if(rightMarkCount - 2 > 0 && (leftMark - origMarks[rightMarkCount-2] < maxPeriod)){
					hanningWidth = leftMark - origMarks[rightMarkCount-2];
				}
				else{
					hanningWidth = minPeriod;
				}
				
				startOfUnmarkedSection = leftMark + hanningWidth;	
				shiftedSignal = olaLeftHalfHanning(startOfUnmarkedSection, signal, shiftedSignal, leftMark, hanningWidth);
				
				
				
				if(rightMarkCount+1 < numPitchMarks && (origMarks[rightMarkCount+1] - rightMark) < maxPeriod){
					hanningWidth = origMarks[rightMarkCount+1] - rightMark; // subtract next period from right mark
				}
				else{
					hanningWidth = minPeriod;
				}
				
				endOfUnmarkedSection = rightMark - hanningWidth;
				shiftedSignal = olaRightHalfHanning(endOfUnmarkedSection, signal, shiftedSignal, rightMark, hanningWidth);
				
				if(endOfUnmarkedSection < startOfUnmarkedSection){
					int temp = startOfUnmarkedSection;
					startOfUnmarkedSection = endOfUnmarkedSection;
					endOfUnmarkedSection = temp;
				}
				
				System.arraycopy(signal, startOfUnmarkedSection, shiftedSignal, startOfUnmarkedSection, endOfUnmarkedSection - startOfUnmarkedSection);

				synthesisPitchMark = rightMark;
			}
			
			if(autotunePeriod > 0){
				currentShiftedPeriod = autotunePeriod;
			}else{
				currentShiftedPeriod = Math.round(currentPeriod/shiftMultiplier);
			}
			/*
			Log.i("mid", "current period " + currentPeriod);
			Log.i("mid", "current shifted period " + currentShiftedPeriod);
			Log.i("mid", "closest mark " + closestPitchMark);
			Log.i("mid", "left mark " + leftMark);
			Log.i("mid", "right mark " + rightMark);
			*/
			while(synthesisPitchMark+currentShiftedPeriod <= rightMark){// && rightMark+currentPeriod < signal.length){ //loop to compute all synthesis marks between left and right marks(including right)
																			//make a requirement for the mark to have the full hanning window

				synthesisPitchMark += currentShiftedPeriod;
				//Log.i("mid", "synth mark " + synthesisPitchMark);
				closestPitchMark = getClosestPitchMark(leftMark, rightMark, synthesisPitchMark);
				//Log.i("mid", "new closest mark " + closestPitchMark);
				shiftedSignal = overlapAndAddSegment(signal, shiftedSignal, leftMark, rightMark, closestPitchMark, synthesisPitchMark, currentPeriod);
				//System.out.println("shPer: " + currentShiftedPeriod);
				//System.out.println("right mark: " + rightMark);
				
			}
			
		}

		/*
		//now search on the right of the last pitch mark
		closestPitchMark = origMarks[numPitchMarks-1];
		Log.i("right side", "current period " + currentPeriod);
		Log.i("right side", "last pitch mark " + closestPitchMark);
		Log.i("right side", "current shifted period " + currentShiftedPeriod);
		
		while(synthesisPitchMark+currentShiftedPeriod < signal.length){	//loop until all the synthesis marks on the left side of the first pitch mark are taken care of
			synthesisPitchMark += currentShiftedPeriod;
			Log.i("right side", "synth mark " + synthesisPitchMark);
			shiftedSignal = overlapAndAddSegment(signal, shiftedSignal, leftMark, rightMark, closestPitchMark, synthesisPitchMark, currentPeriod);
		}
		
		*/ 

		/*
		//add one period of left side of hanning from rest of signal after the last pitch mark (if there is room) and then copy the rest of the signal
		int startOfUnmarkedSection = rightMark+currentPeriod;
		
		if(startOfUnmarkedSection < signal.length){

			System.arraycopy(signal, startOfUnmarkedSection, shiftedSignal, startOfUnmarkedSection, (signal.length - startOfUnmarkedSection));
			shiftedSignal = olaLeftHalfHanning(startOfUnmarkedSection, signal, shiftedSignal, rightMark, currentPeriod);
			
		}
		*/
		
		
		
		
		
		for(int i = 0; i < currentSignal.length; i++){
			if(shiftMultiplier > 1)
				shiftedSignal[i] = shiftedSignal[i]/shiftMultiplier;
			
			shiftedSignal[i] = shiftedSignal[i]*((100-combine)/100f) + signal[i]*(combine/100f);
		}
		lastSynthesisMark = synthesisPitchMark - currentSignal.length;
		//System.out.println("END last synth: " + lastSynthesisMark + " last synthPitchMark = " + synthesisPitchMark + " cur sig len: " + currentSignal.length);
		return shiftedSignal;
	}
	
	public static float[] pitchShiftMarks(float[] signal, float[] pitchMarks, float alpha, float shiftAmount, float gamma){
		// gamma newFormantFreq/oldFormantFreq
		
		/*
		if(shiftAmount == 0){
			return null;
		}*/

		float[] shiftedSignal = new float[signal.length];
		
		float shiftMultiplier = (float) Math.pow(2, (shiftAmount/12));
		int numPitchMarks = pitchMarks.length/2;	//pitchMarks contains pairs (amplitude and position)
		int signalLength = signal.length;
		if(numPitchMarks < 2){
			Log.i("pitch shifter", numPitchMarks + " is not enough pitch marks");
			return null;
		}
		
		int[] origMarks = new int[numPitchMarks];
		for(int i = 0; i < numPitchMarks; i++){
			origMarks[i] = (int)pitchMarks[i*2];
		}
		

		int leftMark = origMarks[0];
		int rightMark = origMarks[0];
		int closestPitchMark = origMarks[0];
		int synthesisPitchMark = closestPitchMark;
		int currentPeriod = origMarks[1] - origMarks[0];
		


		int currentShiftedPeriod = Math.round(currentPeriod/shiftMultiplier);
		
		float[] shiftedMarks = new float[500];
		int numSynth = 0;
		shiftedMarks[0] = synthesisPitchMark;
		shiftedMarks[1] = -0.5f;
		numSynth++;
		

		
		while(synthesisPitchMark-currentShiftedPeriod >=0){	//loop until all the synthesis marks on the left side of the first pitch mark are taken care of
			synthesisPitchMark -= currentShiftedPeriod;
			shiftedMarks[numSynth*2] = synthesisPitchMark;
			shiftedMarks[numSynth*2+1] = -0.5f;
			numSynth++;
		}
		
		//now search between the right of the first pitch mark and the left of the last pitch mark (inclusive)
		
		for(int rightMarkCount = 1; rightMarkCount < numPitchMarks; rightMarkCount++){
			leftMark = origMarks[rightMarkCount-1]; //always search in between left and right marks (and including right side only)
			rightMark = origMarks[rightMarkCount];
			currentPeriod = rightMark - leftMark;								//difference between regular pitch marks
			currentShiftedPeriod = Math.round(currentPeriod/shiftMultiplier);	//difference between synthesis marks
			
			while(synthesisPitchMark+currentShiftedPeriod <= rightMark){ //loop to compute all synthesis marks between left and right marks(including right)

				synthesisPitchMark += currentShiftedPeriod;
				shiftedMarks[numSynth*2] = synthesisPitchMark;
				shiftedMarks[numSynth*2+1] = -0.5f;
				numSynth++;
				
				closestPitchMark = getClosestPitchMark(leftMark, rightMark, synthesisPitchMark);
				
			}
			
		}

		//now search on the right of the last pitch mark
		closestPitchMark = origMarks[numPitchMarks-1];

		
		while(synthesisPitchMark+currentShiftedPeriod < signal.length){	//loop until all the synthesis marks on the left side of the first pitch mark are taken care of
			synthesisPitchMark += currentShiftedPeriod;
			shiftedMarks[numSynth*2] = synthesisPitchMark;
			shiftedMarks[numSynth*2+1] = -0.5f;
			numSynth++;
			
		}
		
		float[] returnMarks = new float[numSynth*2];
		System.arraycopy(shiftedMarks,0,returnMarks,0,numSynth*2);
		return returnMarks;
	}
	
	/*
	 * returns the pitch mark that is closer from either side.
	 * a tie will return the left one
	 */
	public static int getClosestPitchMark(int left, int right, int synthesisMark){
		if(Math.abs(synthesisMark - left) <= Math.abs(synthesisMark - right))
			return left;
		return right;
	}
	public static float[] generateHanningWindow(int width){
		int length = width*2 + 1;
		float[] hanningWindow = new float[length];
        for (int i = 0; i < length; i++){
        	hanningWindow[i] = (float) (0.5 * (1.0 - Math.cos((2*Math.PI*i)/(length-1))));
        }
		return hanningWindow;
	}
	
	/*
	 * overlaps and adds left half of a pitch mark (typically used to start an unvoiced section)
	 */
	public static float[] olaLeftHalfHanning(int startOfUnmarkedSection, float[] signal, float[] shiftedSignal, int lastMark, int currentPeriod){
		if(startOfUnmarkedSection < signal.length){
			float[] hanning = generateHanningWindow(currentPeriod);
			for(int i = lastMark; i < startOfUnmarkedSection; i++){
				shiftedSignal[i] += hanning[i-lastMark]*signal[i];
			}
		}
		return shiftedSignal;
	}
	
	/*
	 * overlaps and adds right half of a pitch mark (typically used to end an unvoiced section)
	 */
	public static float[] olaRightHalfHanning(int endOfUnmarkedSection, float[] signal, float[] shiftedSignal, int nextMark, int currentPeriod){
		if(endOfUnmarkedSection > 0){
			float[] hanning = generateHanningWindow(currentPeriod);
			for(int i = endOfUnmarkedSection; i < nextMark; i++){
				shiftedSignal[i] += hanning[i-endOfUnmarkedSection+currentPeriod]*signal[i];
			}
		}
		return shiftedSignal;
	}

	public static float[] overlapAndAddSegment(float[] originalSignal, float[] shiftedSignal, int leftMark, int rightMark, int pitchMark, int synthesisPitchMark, int period){
		int signalLength = shiftedSignal.length;
		int synthesisLeftRemainder = synthesisPitchMark - period;
		int synthesisStart = 0;
		int hanningStart = 0;
		
		if(synthesisLeftRemainder < 0){		//if result is negative, some of the window will be cut off
			hanningStart = -synthesisLeftRemainder;	//start indexing further in the hanning window
			synthesisStart = 0;
		}
		else{								//else no remainder
			synthesisStart = synthesisLeftRemainder;
			synthesisLeftRemainder = 0;
		}
		
		int synthesisRightRemainder = synthesisPitchMark + period;
		int synthesisEnd = 0;
		float[] hanning = generateHanningWindow(period);
		if(synthesisRightRemainder >= signalLength){		//if result is greater than length, then some of the window will be cut off
			synthesisRightRemainder = synthesisRightRemainder - signalLength+1;
			synthesisEnd = signalLength-1;
		}
		else{								//else no remainder
			synthesisEnd = synthesisRightRemainder;
			synthesisRightRemainder = 0;
		}

		/*
		Log.i("ola1", "left mark " + leftMark);
		Log.i("ola", "right mark " + rightMark);		
		Log.i("ola", "closest mark " + pitchMark);
		Log.i("ola", "synth mark " + synthesisPitchMark);	
		Log.i("ola", "pitch mark " + pitchMark);		
		Log.i("ola", "period " + period);

		Log.i("ola", "synth start " + synthesisStart);
		Log.i("ola", "hanningStart " + hanningStart);
		Log.i("ola", "synth end " + synthesisEnd);
		*/
		
		//check left extreme
		int closestMark = pitchMark;
		if(pitchMark+synthesisStart-synthesisPitchMark < 0){	//there is not enough samples on the left of this pitch mark to fill up the left side of the synthesis mark
			closestMark = rightMark;
		}
		
		//add left side of signal * hanning window
		for(int i = synthesisStart; i < synthesisPitchMark; i++){
			//System.out.println("hanning index: " + (i+hanningStart-synthesisStart));
			//System.out.println("orig index: " + (closestMark+i-synthesisPitchMark));
			//System.out.println("i: " + i);
			shiftedSignal[i] += hanning[i+hanningStart-synthesisStart]*originalSignal[closestMark+i-synthesisPitchMark];
		}
		//out of bounds
		
		
		//check right extreme
		closestMark = pitchMark;
		if(pitchMark+synthesisEnd-synthesisPitchMark >= signalLength){	//there is not enough samples on the right of this pitch mark to fill up the right side of the synthesis mark
			closestMark = leftMark;
		}
		
		//add right side of signal * hanning window (includes center pitch mark)
		for(int i = synthesisPitchMark; i <= synthesisEnd; i++){
			//System.out.println("hanning index: " + (i+synthesisRightRemainder-synthesisPitchMark));
			//System.out.println("orig index: " + (closestMark+i-synthesisPitchMark));
			//System.out.println("i: " + i);
			shiftedSignal[i] += hanning[i-synthesisPitchMark+period]*originalSignal[closestMark+i-synthesisPitchMark];//hanning[period]
		}
		return shiftedSignal;
	}
}
