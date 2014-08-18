package com.amplitudevisualizer;

import java.text.NumberFormat;

import com.amplitudevisualizer.R;
import com.amplitudevisualizer.AudioCapture.*;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.view.Menu;
import android.view.MenuItem;
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

public class MainActivity extends Activity implements AudioCaptureListener, OnSharedPreferenceChangeListener {

	private GraphicalView chart_view;
	private XYMultipleSeriesDataset m_dataset;  //Object that can hold multiple TimeSeries objects and plot them
	private XYMultipleSeriesRenderer m_renderer;
	private XYSeriesRenderer renderer;
	private XYSeries amplitude_series; // Object for storing (x,y) pair values
			
	boolean recording;
	boolean end_capture;
	AudioCapture audio_capture;
	TextView status;
	Button recordButton, cancelButton;
	LinearLayout chartContainer;
	int recordTime;
	SharedPreferences sharedPref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		sharedPref.registerOnSharedPreferenceChangeListener(this);
		recordTime = Integer.parseInt(sharedPref.getString("record_time", "15"));
		recording = false; end_capture = false;
		recordButton = (Button) findViewById(R.id.b_record);
		recordButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click
				if (recording) {
					audio_capture.stop();
				} 
				else {
					if (end_capture) {
						clearChartData();
						chart_view.repaint();
					}
					if (audio_capture == null) {
						audio_capture = new AudioCapture(MainActivity.this);
					}
					end_capture = false;
					audio_capture.capture(recordTime);
					recordButton.setText("stop");
					cancelButton.setEnabled(true);
				}
			}
		});
		cancelButton = (Button) findViewById(R.id.b_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click
				audio_capture.stop();
				clearChartData();
				chart_view.repaint();
			}
		});
		cancelButton.setEnabled(false);
		status = (TextView) findViewById(R.id.t_status);
		
		// init the "sound amplitude visualizer"
        initChart();
	}
			

	@Override
	public void didFinishListening() {
		didInterrupted();
	}

	@Override
	public void willStartListening() {
		status.setText("Listening...");
		recording = true;
	}

	@Override
	public void didFailWithException(Exception e) {
		status.setText("Error: " + e);
		recording = false;
		end_capture = true;
	}

	@Override
	public void didInterrupted() {
		status.setText(R.string.t_status);
		recording = false;
		recordButton.setText(R.string.b_record);
		cancelButton.setEnabled(false);
		end_capture = true;
	}
	
	@Override
	public void didComputeNewAmplitude(double time, double amplitude) {
		m_dataset.getSeriesAt(0).add(time, amplitude);
		chart_view.repaint();
	}
	
	public void initChart() {
		amplitude_series = new XYSeries("Sound amplitude");
		m_dataset = new XYMultipleSeriesDataset();
		m_dataset.addSeries(amplitude_series);
        renderer = new XYSeriesRenderer();
        
        renderer.setPointStyle(PointStyle.POINT);
        renderer.setColor(Color.BLACK);
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(0);
        renderer.setChartValuesFormat(format);
        renderer.setFillPoints(true);
        renderer.setDisplayChartValues(true);
        /*FillOutsideLine fill = new FillOutsideLine(FillOutsideLine.Type.BOUNDS_ALL);
        fill.setColor(Color.parseColor("#FFFFA600"));
        renderer.addFillOutsideLine(fill);
        
        renderer.setGradientEnabled(true);
        renderer.setGradientStart(0, Color.rgb(192, 192, 192));
        renderer.setGradientStop(45, Color.rgb(25, 25, 112));
        */
        m_renderer = new XYMultipleSeriesRenderer();
        m_renderer.addSeriesRenderer(renderer);

        m_renderer.setChartTitle("Sound amplitude Visualizer");
        m_renderer.setXTitle("Time (s)");
        m_renderer.setYTitle("amplitude (Pa)");
        m_renderer.setAxesColor(Color.RED);
        m_renderer.setShowGrid(true);
        m_renderer.setFitLegend(true); // Make the legend auto-resizable 
        m_renderer.setXLabels(10);
        m_renderer.setYLabels(5);
        m_renderer.setXLabelsAlign(Align.RIGHT);
        m_renderer.setYLabelsAlign(Align.RIGHT);
        m_renderer.setApplyBackgroundColor(true);
        m_renderer.setBackgroundColor(Color.WHITE);
        m_renderer.setMarginsColor(Color.WHITE);
        m_renderer.setLabelsColor(Color.DKGRAY);
        m_renderer.setZoomButtonsVisible(true);
        m_renderer.setXAxisMin(0);
        m_renderer.setXAxisMax(recordTime);
        m_renderer.setYAxisMin(0);
        m_renderer.setAntialiasing(true);
        chartContainer = (LinearLayout) findViewById(R.id.chart_container);
        chart_view = (GraphicalView) ChartFactory.getCubeLineChartView(getBaseContext(), m_dataset, m_renderer, 0.2f );
        // Adding the Line Chart to the LinearLayout
        chartContainer.addView(chart_view);
	}
	
	public void clearChartData() {
		amplitude_series.clear();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}	
	
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId()) {
		case R.id.action_settings:
			startActivity(new Intent(this, MyPreferenceActivity.class));
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO Auto-generated method stub
		if (key.equals("record_time")) {
			recordTime = Integer.parseInt(sharedPref.getString("record_time", "15"));
			if (recording) {
				audio_capture.stop();
		        clearChartData();
			}
	        m_renderer.setXAxisMax(recordTime);
			chart_view.repaint();
		}
	}

}
