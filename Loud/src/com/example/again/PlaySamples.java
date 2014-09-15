package com.example.again;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import android.media.AudioTrack;

public class PlaySamples implements Runnable {
	private AudioTrack play;
	private BooleanBox hasAudioFocus;
	private Lock focusLock;
	private BooleanBox canDestroyPlaybackResources;
	private Condition canDestroyCond; 
	
	public PlaySamples(
			AudioTrack play, 
			BooleanBox hasAudioFocus, 
			Lock focusLock,
			BooleanBox canDestroyPlaybackResources, 
			Condition canDestroyCond) {
		this.play = play;
		this.hasAudioFocus = hasAudioFocus;
		this.focusLock = focusLock;
		this.canDestroyPlaybackResources = canDestroyPlaybackResources;
		this.canDestroyCond = canDestroyCond;
	}
	
	public void run() {
		while(true) {
			focusLock.lock();
			if (!hasAudioFocus.get()) {
				while (!canDestroyPlaybackResources.get()) {
					try {
						canDestroyCond.await();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				play.release();
				focusLock.unlock();
				return;
			}
			focusLock.unlock();
			play.play();
			Thread.yield();
		}
	}
}
