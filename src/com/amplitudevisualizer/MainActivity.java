package com.amplitudevisualizer;

import com.amplitudevisualizer.R;
import com.amplitudevisualizer.AudioCapture.*;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

public class MainActivity extends Activity implements AudioCaptureListener {
	
	private GraphicalView chart_view;
	private XYMultipleSeriesDataset m_dataset;  //Object that can hold multiple TimeSeries objects and plot them
	private XYMultipleSeriesRenderer m_renderer;
	private XYSeriesRenderer renderer;
	private XYSeries amplitude_series; // Object for storing (x,y) pair values
			
	boolean recording = false;
	boolean end_capture = false;
	AudioCapture audio_capture;
	TextView status;
	Button recordButton, cancelButton;
	LinearLayout chartContainer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		recordButton = (Button) findViewById(R.id.b_record);
		recordButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click
				if (recording) {
					audio_capture.stop();
					recordButton.setText(R.string.b_record);
					cancelButton.setEnabled(false);
					end_capture = true;
				} 

				else {
					if (end_capture) {
						chartContainer.removeAllViews();
						initChart();
					}
					if (audio_capture == null) {
						audio_capture = new AudioCapture(MainActivity.this);
					}
					audio_capture.capture();
					recordButton.setText("stop");
					cancelButton.setEnabled(true);
					end_capture = false;
				}
			}
		});
		cancelButton = (Button) findViewById(R.id.b_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click
				audio_capture.stop();
				chartContainer.removeAllViews();
				initChart();
				recordButton.setText(R.string.b_record);
				status.setText(R.string.t_status);
				cancelButton.setEnabled(false);
				
			}
		});
		cancelButton.setEnabled(false);
		status = (TextView) findViewById(R.id.t_status);
		
		// init the "sound amplitude visualizer"
        initChart();
	}
			

	@Override
	public void didFinishListening() {
		recording = false;
		end_capture = true;
		recordButton.setText(R.string.b_record);
		//status.setText(Integer.toString(nb_points));
		status.setText(R.string.t_status);
		cancelButton.setEnabled(false);
		/*
		 * demarrage nouvelle tache pour afficher l'amplitude du son dans un fichier
		 */
		// new SaveSoundTask().execute();

	}

	@Override
	public void willStartListening() {
		status.setText("Listening...");
		recording = true;
	}

	@Override
	public void didFailWithException(Exception e) {
		status.setText("Error: " + e);
	}

	@Override
	public void didInterrupted() {
		status.setText(R.string.t_status);
		recording = false;
	}
	
	@Override
	public void didComputeNewAmplitude(double time, double amplitude) {
		//nb_points++;
		m_dataset.getSeriesAt(0).add(time, amplitude);
		chart_view.repaint();
	}
	
	public void initChart() {
		amplitude_series = new XYSeries("amplitude du signal audio");
		m_dataset = new XYMultipleSeriesDataset();
		m_dataset.addSeries(amplitude_series);
        renderer = new XYSeriesRenderer();
        
        renderer.setPointStyle(PointStyle.POINT);
        renderer.setColor(Color.BLACK);
        renderer.setFillPoints(true);
        renderer.setDisplayChartValues(true);
        
        m_renderer = new XYMultipleSeriesRenderer();
        m_renderer.addSeriesRenderer(renderer);

        m_renderer.setChartTitle("Sound amplitude Visualizer");
        m_renderer.setXTitle("Time (s)");
        m_renderer.setYTitle("amplitude (Pa)");
        m_renderer.setAxesColor(Color.RED);
        m_renderer.setShowGrid(true);
        m_renderer.setFitLegend(true); // Make the legend auto-resizable 
        m_renderer.setXLabels(4);
        m_renderer.setXLabelsAlign(Align.RIGHT);
        m_renderer.setYLabelsAlign(Align.RIGHT);
        //m_renderer.setRange(new double[] {0,30,0,40000});
        
        m_renderer.setApplyBackgroundColor(true);
        m_renderer.setBackgroundColor(Color.WHITE);
        m_renderer.setMarginsColor(Color.WHITE);
        m_renderer.setLabelsColor(Color.DKGRAY);
        m_renderer.setZoomButtonsVisible(true);
        m_renderer.setInScroll(true);
        m_renderer.setXAxisMin(0);
        m_renderer.setXAxisMax(30);        
        m_renderer.setYAxisMin(0);


        chartContainer = (LinearLayout) findViewById(R.id.chart_container);
        chart_view = (GraphicalView) ChartFactory.getLineChartView(getBaseContext(), m_dataset, m_renderer);
        // Adding the Line Chart to the LinearLayout
        chartContainer.addView(chart_view);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
