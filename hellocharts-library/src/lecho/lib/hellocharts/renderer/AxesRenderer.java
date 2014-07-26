package lecho.lib.hellocharts.renderer;

import lecho.lib.hellocharts.Chart;
import lecho.lib.hellocharts.ChartCalculator;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.util.Utils;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetricsInt;
import android.text.TextUtils;

public class AxesRenderer {
	private static final int DEFAULT_AXIS_MARGIN_DP = 4;
	private Paint textPaint;
	private Paint linePaint;
	private Chart chart;
	private Context context;
	private int axisMargin;
	// For now don't draw lines for X axis
	// private float[] axisXDrawBuffer;
	private final AxisStops axisXStopsBuffer = new AxisStops();
	private float[] axisYDrawBuffer = new float[] {};
	private final AxisStops axisYStopsBuffer = new AxisStops();
	private int axesLabelMaxWidth;
	private int axesTextHeight;
	private FontMetricsInt fontMetrics = new FontMetricsInt();
	private char[] labelBuffer = new char[32];
	private String maxLabel = "0000";

	public AxesRenderer(Context context, Chart chart) {
		this.context = context;
		this.chart = chart;
		linePaint = new Paint();
		linePaint.setAntiAlias(true);
		linePaint.setStyle(Paint.Style.STROKE);
		linePaint.setStrokeWidth(1);

		textPaint = new Paint();
		textPaint.setAntiAlias(true);
		textPaint.setStyle(Paint.Style.FILL);
		textPaint.setStrokeWidth(1);

		axisMargin = Utils.dp2px(context, DEFAULT_AXIS_MARGIN_DP);
	}

	public void initRenderer() {
		linePaint.setColor(chart.getData().getAxesColor());
		textPaint.setColor(chart.getData().getAxesColor());
		textPaint.setTextSize(Utils.sp2px(context, chart.getData().getAxesTextSize()));
		textPaint.getFontMetricsInt(fontMetrics);
		int axisXHeight = getAxisXHeight(chart.getData().getAxisX());
		int axisYWidth = getAxisYWidth(chart.getData().getAxisY());
		chart.getChartCalculator().setAxesMargin(axisXHeight, axisYWidth);
	}

	private int getAxisXHeight(Axis axisX) {
		axesTextHeight = Math.abs(fontMetrics.ascent);
		int result = 0;
		if (axisX.isAutoGenerated() || !axisX.getValues().isEmpty()) {
			result += axesTextHeight;
		}
		if (!TextUtils.isEmpty(axisX.getName())) {
			result += axesTextHeight;
		}
		result += axisMargin;
		return result;
	}

	private int getAxisYWidth(Axis axisY) {
		int result = 0;
		if (axisY.isAutoGenerated()) {
			axesLabelMaxWidth = (int) textPaint.measureText(maxLabel);
			result += axesLabelMaxWidth;
		} else if (!axisY.getValues().isEmpty()) {
			final int numChars;
			// to simplify I assume that the widest value will be the first or the last.
			if (Math.abs(axisY.getValues().get(0).getValue()) >= Math.abs(axisY.getValues()
					.get(axisY.getValues().size() - 1).getValue())) {
				numChars = axisY.getFormatter().formatValue(labelBuffer, axisY.getValues().get(0).getValue());
			} else {
				numChars = axisY.getFormatter().formatValue(labelBuffer,
						axisY.getValues().get(axisY.getValues().size() - 1).getValue());
			}
			if (numChars > 0) {
				axesLabelMaxWidth = (int) textPaint.measureText(labelBuffer, labelBuffer.length - numChars, numChars);
				result += axesLabelMaxWidth;
			}
		}
		if (!TextUtils.isEmpty(axisY.getName())) {
			axesTextHeight = Math.abs(fontMetrics.ascent);
			result += axesTextHeight;
		}
		result += axisMargin;
		return result;
	}

	public void draw(Canvas canvas) {
		// Draw axisY first to prevent X axis overdrawing.
		drawAxisY(canvas);
		drawAxisX(canvas);
	}

	private void drawAxisX(Canvas canvas) {
		final ChartCalculator chartCalculator = chart.getChartCalculator();
		final Axis axisX = chart.getData().getAxisX();
		textPaint.setTextAlign(Align.CENTER);
		if (axisX.isAutoGenerated()) {
			drawAxisXAuto(canvas, chartCalculator, axisX);
		} else {
			drawAxisX(canvas, chartCalculator, axisX);
		}
		// Draw separation line
		canvas.drawLine(chartCalculator.mContentRectWithMargins.left, chartCalculator.mContentRect.bottom,
				chartCalculator.mContentRectWithMargins.right, chartCalculator.mContentRect.bottom, linePaint);
		// Drawing axis name
		if (!TextUtils.isEmpty(axisX.getName())) {
			float baseline = chartCalculator.mContentRectWithMargins.bottom + 2 * axesTextHeight + axisMargin;
			canvas.drawText(axisX.getName(), chartCalculator.mContentRect.centerX(), baseline, textPaint);
		}
	}

	private void drawAxisX(Canvas canvas, ChartCalculator chartCalculator, Axis axisX) {
		if (axisX.getValues().size() > 0) {
			// drawing axis values
			float rawY = chartCalculator.mContentRectWithMargins.bottom + axesTextHeight;
			for (AxisValue axisValue : axisX.getValues()) {
				final float value = axisValue.getValue();
				if (value >= chartCalculator.mCurrentViewport.left && value <= chartCalculator.mCurrentViewport.right) {
					final float rawX = chartCalculator.calculateRawX(axisValue.getValue());
					final int nummChars = axisX.getFormatter().formatValue(labelBuffer, axisValue.getValue());
					canvas.drawText(labelBuffer, labelBuffer.length - nummChars, nummChars, rawX, rawY, textPaint);
				}
			}
		}
	}

	private void drawAxisXAuto(Canvas canvas, ChartCalculator chartCalculator, Axis axisX) {
		computeAxisStops(chartCalculator.mCurrentViewport.left, chartCalculator.mCurrentViewport.right,
				chartCalculator.mContentRect.width() / axesLabelMaxWidth / 2, axisXStopsBuffer);
		textPaint.setTextAlign(Paint.Align.CENTER);
		float rawY = chartCalculator.mContentRectWithMargins.bottom + axesTextHeight;
		int i;
		for (i = 0; i < axisXStopsBuffer.numStops; ++i) {
			// TODO: Should I draw vertical lines for X axis, that doesn't look good but sometimes it is useful.
			float rawX = chartCalculator.calculateRawX(axisXStopsBuffer.stops[i]);
			final int nummChars = axisX.getFormatter().formatValue(labelBuffer, axisXStopsBuffer.stops[i],
					axisXStopsBuffer.decimals);
			canvas.drawText(labelBuffer, labelBuffer.length - nummChars, nummChars, rawX, rawY, textPaint);
		}
	}

	private void drawAxisY(Canvas canvas) {
		final ChartCalculator chartCalculator = chart.getChartCalculator();
		final Axis axisY = chart.getData().getAxisY();
		textPaint.setTextAlign(Align.RIGHT);
		// drawing axis values
		if (axisY.isAutoGenerated()) {
			drawAxisYAuto(canvas, chartCalculator, axisY);
		} else {
			drawAxisY(canvas, chartCalculator, axisY);
		}
		// drawing axis name
		textPaint.setTextAlign(Align.CENTER);
		if (!TextUtils.isEmpty(axisY.getName())) {
			final float rawX = chartCalculator.mContentRectWithMargins.left - axesLabelMaxWidth - axisMargin;
			canvas.save();
			canvas.rotate(-90, chartCalculator.mContentRect.centerY(), chartCalculator.mContentRect.centerY());
			canvas.drawText(axisY.getName(), chartCalculator.mContentRect.centerY(), rawX, textPaint);
			canvas.restore();
		}
	}

	public void drawAxisY(Canvas canvas, ChartCalculator chartCalculator, Axis axisY) {
		if (axisYDrawBuffer.length < axisY.getValues().size() * 4) {
			axisYDrawBuffer = new float[axisY.getValues().size() * 4];
		}
		float rawX = chartCalculator.mContentRectWithMargins.left;
		int i = 0;
		for (AxisValue axisValue : axisY.getValues()) {
			final float value = axisValue.getValue();
			if (value <= chartCalculator.mCurrentViewport.bottom && value >= chartCalculator.mCurrentViewport.top) {
				final float rawY = chartCalculator.calculateRawY(value);
				axisYDrawBuffer[i++] = rawX;
				axisYDrawBuffer[i++] = rawY;
				axisYDrawBuffer[i++] = chartCalculator.mContentRectWithMargins.right;
				axisYDrawBuffer[i++] = rawY;
				final int nummChars = axisY.getFormatter().formatValue(labelBuffer, axisValue.getValue());
				canvas.drawText(labelBuffer, labelBuffer.length - nummChars, nummChars, rawX, rawY, textPaint);
			}
		}
		canvas.drawLines(axisYDrawBuffer, 0, i, linePaint);
	}

	public void drawAxisYAuto(Canvas canvas, ChartCalculator chartCalculator, Axis axisY) {
		computeAxisStops(chartCalculator.mCurrentViewport.top, chartCalculator.mCurrentViewport.bottom,
				chartCalculator.mContentRect.height() / axesTextHeight / 2, axisYStopsBuffer);
		if (axisYDrawBuffer.length < axisYStopsBuffer.numStops * 4) {
			axisYDrawBuffer = new float[axisYStopsBuffer.numStops * 4];
		}
		float rawX = chartCalculator.mContentRectWithMargins.left;
		int i;
		for (i = 0; i < axisYStopsBuffer.numStops; i++) {
			final float rawY = chartCalculator.calculateRawY(axisYStopsBuffer.stops[i]);
			axisYDrawBuffer[i * 4 + 0] = rawX;
			axisYDrawBuffer[i * 4 + 1] = rawY;
			axisYDrawBuffer[i * 4 + 2] = chartCalculator.mContentRectWithMargins.right;
			axisYDrawBuffer[i * 4 + 3] = rawY;
			final int nummChars = axisY.getFormatter().formatValue(labelBuffer, axisYStopsBuffer.stops[i],
					axisYStopsBuffer.decimals);
			canvas.drawText(labelBuffer, labelBuffer.length - nummChars, nummChars, rawX, rawY, textPaint);
		}
		canvas.drawLines(axisYDrawBuffer, 0, axisYStopsBuffer.numStops * 4, linePaint);
	}

	/**
	 * Computes the set of axis labels to show given start and stop boundaries and an ideal number of stops between
	 * these boundaries.
	 * 
	 * @param start
	 *            The minimum extreme (e.g. the left edge) for the axis.
	 * @param stop
	 *            The maximum extreme (e.g. the right edge) for the axis.
	 * @param steps
	 *            The ideal number of stops to create. This should be based on available screen space; the more space
	 *            there is, the more stops should be shown.
	 * @param outStops
	 *            The destination {@link AxisStops} object to populate.
	 */
	private static void computeAxisStops(float start, float stop, int steps, AxisStops outStops) {
		double range = stop - start;
		if (steps == 0 || range <= 0) {
			outStops.stops = new float[] {};
			outStops.numStops = 0;
			return;
		}

		double rawInterval = range / steps;
		double interval = Utils.roundToOneSignificantFigure(rawInterval);
		double intervalMagnitude = Math.pow(10, (int) Math.log10(interval));
		int intervalSigDigit = (int) (interval / intervalMagnitude);
		if (intervalSigDigit > 5) {
			// Use one order of magnitude higher, to avoid intervals like 0.9 or 90
			interval = Math.floor(10 * intervalMagnitude);
		}

		double first = Math.ceil(start / interval) * interval;
		double last = Utils.nextUp(Math.floor(stop / interval) * interval);

		double intervalValue;
		int stopIndex;
		int numStops = 0;
		for (intervalValue = first; intervalValue <= last; intervalValue += interval) {
			++numStops;
		}

		outStops.numStops = numStops;

		if (outStops.stops.length < numStops) {
			// Ensure stops contains at least numStops elements.
			outStops.stops = new float[numStops];
		}

		for (intervalValue = first, stopIndex = 0; stopIndex < numStops; intervalValue += interval, ++stopIndex) {
			outStops.stops[stopIndex] = (float) intervalValue;
		}

		if (interval < 1) {
			outStops.decimals = (int) Math.ceil(-Math.log10(interval));
		} else {
			outStops.decimals = 0;
		}
	}

	/**
	 * A simple class representing axis label values used only for auto generated axes.
	 * 
	 */
	private static class AxisStops {
		float[] stops = new float[] {};
		int numStops;
		int decimals;
	}
}
