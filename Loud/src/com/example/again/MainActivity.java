package com.example.again;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity {
	private BooleanBox hasAudioFocus;
	private Lock focusLock;
	private Condition audioFocus;
	private AudioManager am; 
	private static int filled_color;
	private static int unfilled_color;
	
	public MainActivity() {
		super();
        hasAudioFocus = new BooleanBox(false);
        focusLock = new ReentrantLock();
        audioFocus = focusLock.newCondition();
	}
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        am = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);  
        filled_color = getApplicationContext().getResources().getColor(R.color.holo_blue_dark);
      	unfilled_color = getApplicationContext().getResources().getColor(android.R.color.darker_gray);
        setContentView(R.layout.activity_main);       
    }
    
    private void recolorVolumeBars() {
    	// color the correct number of volume bars for the current volume
    	View volumeBars = findViewById(R.id.volumeBars);
    	int numToColor = getNumBarsToColor(volumeBars);	  
    	for(int i=0; i<numToColor; ++i) {
    		View nextChild = ((ViewGroup)volumeBars).getChildAt(i);
    	    nextChild.setBackgroundColor(filled_color);
    	}
    	for (int i=numToColor; i < ((ViewGroup)volumeBars).getChildCount(); ++i) {
    		View nextChild = ((ViewGroup)volumeBars).getChildAt(i);
    		nextChild.setBackgroundColor(unfilled_color);
    	}
    	((ViewGroup)volumeBars).invalidate();
    }
    
  
  private synchronized void adjustSeekBarProgressToVolume(SeekBar seekBar) {
	   int progress = seekBar.getMax() 
	   			* am.getStreamVolume(AudioManager.STREAM_MUSIC) 
				/ am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);	
	   seekBar.setProgress(progress);
  }

   private int getNumBarsToColor(View volumeBars) {
	   return (int)(((ViewGroup)volumeBars).getChildCount() 
			   * am.getStreamVolume(AudioManager.STREAM_MUSIC) 
			   / am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
   }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) || (keyCode == KeyEvent.KEYCODE_VOLUME_UP)){
        	SeekBar seekBar = (SeekBar) findViewById(R.id.seek_bar);
        	adjustSeekBarProgressToVolume(seekBar);
    	}
    	return super.onKeyDown(keyCode, event);
    }
    
    private void restartPlayback() {
        // Request audio focus for playback
    	final AudioManager.OnAudioFocusChangeListener afChangeListener = new OnAudioFocusChangeListener() {
    		public void onAudioFocusChange(int focusChange) {
    	       if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
    	    	   Log.d("MainActivity", "permanent audioFocus loss");
    	           focusLock.lock();
    	           hasAudioFocus.set(false);
    	           focusLock.unlock();
    	           am.abandonAudioFocus(this);
    	        }
    		}
    	};
    	
        int result = am.requestAudioFocus(afChangeListener,
                                         // Use the music stream.
                                         AudioManager.STREAM_MUSIC,
                                         // Request permanent focus.
                                         AudioManager.AUDIOFOCUS_GAIN);
           
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        	focusLock.lock();
        	hasAudioFocus.set(true);
        	focusLock.unlock();
        	// Start playback
            (new Thread(new RecordSamples(hasAudioFocus, focusLock, audioFocus))).start();
        }
    }
    
    @Override
    public void onResume() {
    	Log.d("MainActivity", "onResume");
    	super.onResume();
    	// if we've lost audiofocus, we ask for it back when the user resumes this app
    	focusLock.lock();
    	if (!hasAudioFocus.get()) {
    		restartPlayback();
    	}
    	focusLock.unlock();
    	SeekBar seekBar = (SeekBar) findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
        	@Override
        	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        		int volume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * seekBar.getProgress() / seekBar.getMax();
        	    // lets flags be 0, volume toast will still appear due to super.onKeyDown for vol keys
        		am.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        	    recolorVolumeBars();
            }

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// empty
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// empty
			}
        });
        // TODO could we possibly get key presses while this next line is executing? / 
        // does this next method actually need to be marked as synchronized?
    	adjustSeekBarProgressToVolume(seekBar);
    }
}