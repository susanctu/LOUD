package com.example.again;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import android.media.AudioRecord;
import android.media.AudioTrack;

public class SampleTransfer implements Runnable {

	private AudioRecord rec;
	private Thread playThread; 
	private AudioTrack play;
	private BooleanBox hasAudioFocus;
	private Lock focusLock;
	private BooleanBox canDestroyPlaybackResources;
	private Condition canDestroyCond;
	
	public SampleTransfer(
			AudioRecord rec, 
			AudioTrack play, 
			BooleanBox hasAudioFocus, 
			Lock focusLock, 
			BooleanBox canDestroyPlaybackResources,
			Condition canDestroyCond) {
		
		this.rec = rec; 
		this.play = play;
		this.hasAudioFocus = hasAudioFocus;
		this.focusLock = focusLock;
		this.canDestroyPlaybackResources = canDestroyPlaybackResources;
		this.canDestroyCond = canDestroyCond;
		
		playThread = new Thread(new PlaySamples(
				play, hasAudioFocus, focusLock, canDestroyPlaybackResources, canDestroyCond));
		playThread.start();
	}
	
	@Override
	public void run() {	
		// This thread must exit before either of RecordSamples or PlaySamples exits
		// Otherwise we may use an AudioRecord or AudioPlayer that has already
		// been destroyed
		int numLoops = 0;
		while (true) {
			
			focusLock.lock();
			if (!hasAudioFocus.get()) {
				canDestroyPlaybackResources.set(true);
				canDestroyCond.signalAll();
				focusLock.unlock();
				return;
			}
			focusLock.unlock();
			
			short[] tempbuf = new short[RecordSamples.BUFFER_SIZE]; // always record into new buffer
			int usableBytes = rec.read(tempbuf,0,RecordSamples.BUFFER_SIZE);
			if ((numLoops++) % 1000 == 0) { //flush it every once in a while so that we don't get behind
				play.flush();
			}
			
			play.write(tempbuf, 0, usableBytes);
			Thread.yield();
		}
	}
}
