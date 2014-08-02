package com.amplitudevisualizer;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AudioCapture implements Runnable {

	private final static int SAMPLE_RATE = 44100; // CD quality
	private final static int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
	private final static int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private final static short SIZE_BUFFER_AMPLITUDE = 256; // a new amplitude will be computed for each buffer of audio data of size 256

	private Thread thread;
	private AudioRecord mRecordInstance = null;
	private short audioData[];
	private int bufferSize;
	private int secondsToRecord;

	private AudioCaptureListener listener;

	/**
	 * Constructor for the class
	 * @param listener is the AudioListener that will receive the callbacks
	 */
	public AudioCapture(AudioCaptureListener listener) {
		this.listener = listener;
	}

	/**
	 * Starts the capture process
	 */
	public void capture() {
		// start the recording thread
		this.secondsToRecord = 30;
		thread = new Thread(this);
		thread.start(); // call the run method
	}

	/**
	 * stops the capture process if there's one in process
	 */
	public void stop() {
		if (mRecordInstance != null)
			mRecordInstance.stop();
	}
	
	/**
	 * The main thread
	 * Records audio and return audio data to the listener
	 */
	public void run() {
		try {
			// get the minimum buffer size required to create a AudioRecord object
			int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING);

			// and the actual buffer size for the audio to record
			// SAMPLE_RATE * seconds to record * 1 (one channel input)
			bufferSize = Math.max(minBufferSize, SAMPLE_RATE * secondsToRecord);

			audioData = new short[bufferSize];

			// start recorder
			mRecordInstance = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL, ENCODING, minBufferSize);

			willStartListening();
			mRecordInstance.startRecording();
			
			double duration_since_recording = 0.;
			int readBuffer = 0;
			
			// fill audio buffer with mic data for time period = seconds_to_record 
			while (readBuffer < bufferSize && mRecordInstance.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
			{
				int offset_buffer = readBuffer;
				do {
					readBuffer += mRecordInstance.read(audioData, readBuffer, bufferSize - readBuffer);
					if (mRecordInstance.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
						break;
					}
				} while (readBuffer < offset_buffer + minBufferSize && readBuffer < bufferSize);
				if (mRecordInstance.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
					break;
				}
				
				// calculate a new amplitude value for all the buffers of size 256
				
				int end_buffer = offset_buffer + SIZE_BUFFER_AMPLITUDE - 1;
				double sum = 0;
				int nb_vals = 0;
				while (end_buffer < offset_buffer + minBufferSize) {
					for (int i = end_buffer - SIZE_BUFFER_AMPLITUDE + 1; i <= end_buffer && i < bufferSize ; i++)
					{
						// The vibrations in the air cause the microphone surface to vibrate in both directions (up and down)
						// so we take | audioData[i] | in order to only have positive values
						sum += Math.abs(audioData[i]);
						nb_vals++;
					}	
					double amplitude = sum / nb_vals; // we get the mean amplitude for all audio data in the corresponding buffer
					didComputeNewAmplitude(duration_since_recording, amplitude);
					
					duration_since_recording += (double) nb_vals / 44100; // voir calcul_amplitude.txt pour precisions
					end_buffer += SIZE_BUFFER_AMPLITUDE - 1;
					sum = 0; nb_vals = 0;
				}
			}

			if (mRecordInstance.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED)
			{
				mRecordInstance.release();
				mRecordInstance = null;
				didInterrupted();
				return;
			}
			
			if (mRecordInstance != null) {
				mRecordInstance.stop();
				mRecordInstance.release();
				mRecordInstance = null;
			}

			// A FAIRE : ecrire methode qui enregistre le son capturé dans un fichier .wav
			didFinishListening();
		}
		catch (Exception e) {
			e.printStackTrace();
			didFailWithException(e);
		}
	}
	

	private void didFinishListening() {
		if (listener == null)
			return;

		if (listener instanceof Activity) {
			Activity activity = (Activity) listener;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					listener.didFinishListening();
				}
			});
		} else
			listener.didFinishListening();
	}

	private void willStartListening() {
		if (listener == null)
			return;

		if (listener instanceof Activity) {
			Activity activity = (Activity) listener;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					listener.willStartListening();
				}
			});
		} else
			listener.willStartListening();
	}

	private void didComputeNewAmplitude(final double current_time, final double amplitude) {
		if (listener == null)
			return;

		if (listener instanceof Activity) {
			Activity activity = (Activity) listener;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					listener.didComputeNewAmplitude(current_time, amplitude);
				}
			});
		} else
			listener.didComputeNewAmplitude(current_time, amplitude);
	}
	
	private void didFailWithException(final Exception e) {
		if (listener == null)
			return;

		if (listener instanceof Activity) {
			Activity activity = (Activity) listener;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					listener.didFailWithException(e);
				}
			});
		} else
			listener.didFailWithException(e);
	}
	
	private void didInterrupted() {
		if (listener == null)
			return;

		if (listener instanceof Activity) {
			Activity activity = (Activity) listener;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					listener.didInterrupted();
				}
			});
		} else
			listener.didInterrupted();
	}

	/**
	 * Interface for the audio listener
	 * Contains the different delegate methods for the capture process
	 */
	public interface AudioCaptureListener {
		/**
		 * Called when the capture process loop has finished
		 */
		public void didFinishListening();

		/**
		 * Called when the capture process is about to start
		 */
		public void willStartListening();
		

		/**
		 * Called when a new amplitude value has been computed (so the chart will be update in real-time)
		 */
		public void didComputeNewAmplitude(double current_time, double amplitude);

		/**
		 * Called if there is an error / exception in the capture process
		 * 
		 * @param e an exception with the error
		 */
		public void didFailWithException(Exception e);

		/**
		 * Called if the capture process has been interrupted
		 */
		public void didInterrupted();
	}
}