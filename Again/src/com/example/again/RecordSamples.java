package com.example.again;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

public class RecordSamples implements Runnable {
	
	private int SAMPLE_RATE;
	private static int[] mSampleRates = new int[] {8000, 11025, 22050, 44100};
	private short channelConfig;
	private short audioFormat;
	public static final int BUFFER_SIZE = 5000; // in bytes
	private AudioRecord rec;
	private AudioTrack play;
	private static final String TAG = "RecordSamples";
	private BooleanBox hasAudioFocus;
	private Lock focusLock;
	private BooleanBox canDestroyPlaybackResources;
	private Condition canDestroyCond;
	
	public RecordSamples(BooleanBox hasAudioFocus, Lock focusLock, Condition canDestroyCond) {
		
		this.hasAudioFocus = hasAudioFocus;
		this.focusLock = focusLock;
		this.canDestroyCond = canDestroyCond;
		this.canDestroyPlaybackResources = new BooleanBox(false);
		rec = findAudioRecord();

		if (channelConfig == AudioFormat.CHANNEL_IN_MONO) {
		   play = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                audioFormat,
                BUFFER_SIZE,
                AudioTrack.MODE_STREAM);
		} else {
			play = new AudioTrack(AudioManager.STREAM_MUSIC,
	             SAMPLE_RATE,
	             AudioFormat.CHANNEL_OUT_STEREO,
	             audioFormat,
	             BUFFER_SIZE,
	             AudioTrack.MODE_STREAM);
		} 
	}
	
	@Override
	public void run() {
		Thread transferThread = new Thread(new SampleTransfer(
				rec, play, hasAudioFocus, focusLock, canDestroyPlaybackResources, canDestroyCond));
		transferThread.start();
		while (true) {
			focusLock.lock();
			if (!hasAudioFocus.get()) {
				if (!canDestroyPlaybackResources.get())
					try {
						canDestroyCond.await();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				rec.stop();
				rec.release();
				focusLock.unlock();
				return;
			}
			focusLock.unlock();
			
			rec.startRecording();
			Thread.yield();
		}
	}
	
	public AudioRecord findAudioRecord() {
		AudioRecord recorder;
	    for (int rate : mSampleRates) {
	        for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
	            for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
	                try {
	                    Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
	                            + channelConfig);
	                    int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

	                    if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
	                        // check if we can instantiate and have a success
	                    	
	                        recorder = new AudioRecord(AudioSource.MIC, rate, channelConfig, audioFormat, bufferSize);

	                        if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
	                        	SAMPLE_RATE = rate;
	                        	this.audioFormat = audioFormat;
	                        	this.channelConfig = channelConfig;
	                            return recorder;
	                        }
	                    }
	                } catch (Exception e) {
	                    Log.e(TAG, rate + "Exception, keep trying.",e);
	                }
	            }
	        }
	    }
	    return null;
	}
	


}
