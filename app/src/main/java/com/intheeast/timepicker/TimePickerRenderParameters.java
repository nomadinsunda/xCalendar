package com.intheeast.timepicker;

import java.util.ArrayList;




import com.intheeast.etc.LockableScrollView;

import android.graphics.Bitmap;

public class TimePickerRenderParameters {
	float TimePickerTextWdit;
	float TimePickerNumeralWidth;	
	
	float TimePickerDateFieldCircleDiameterPixels;
	float Ppi;
	
	float RightDateFieldOfTimePicker;	
	float LeftMeridiemFieldOfTimePicker;	
	float RightMeridiemFieldOfTimePicker;	
	float LeftHourFieldOfTimePicker;	
	float RightHourFieldOfTimePicker;
	
	ArrayList<Bitmap> TimePickerNumeralTextBitmapsList;
	ArrayList<Bitmap> TimePickerTodayTextBitmapsList;
	ArrayList<Bitmap> TimePickerMeridiemTextBitmapsList;
	ArrayList<Bitmap> TimePickerDayOfWeekTextBitmapsList;
	ArrayList<Bitmap> TimePickerCenterDayOfWeekTextBitmapsList;
	ArrayList<Bitmap> TimePickerCenterTodayTextBitmapsList;
	ArrayList<Bitmap> TimePickerCenterMeridiemTextBitmapsList;
	ArrayList<Bitmap> TimePickerCenterNumeralBitmapsList;
	
	
	Bitmap TimePickerMonthTextBitmap;
	Bitmap TimePickerDateTextBitmap;	
	Bitmap TimePickerCenterMonthTextBitmap;
	Bitmap TimePickerCenterDateTextBitmap;
	Bitmap EraseBackgroundBitmap;
	
	LockableScrollView MainPageScrollView;
	
	public TimePickerRenderParameters() {
		
	}
	
	public void setTimePickerTextWdith(float width) {
		this.TimePickerTextWdit = width;
	}
	
	public void setTimePickerNumeralWidth(float width) {
		this.TimePickerNumeralWidth = width;
	}
	
	public void setTimePickerDateFieldCircleDiameterPixels(float width) {
		this.TimePickerDateFieldCircleDiameterPixels = width;
	}
	
	public void setPpi(float width) {
		this.Ppi = width;
	}
	
	public void setMainPageScrollView(LockableScrollView scrollView) {
		this.MainPageScrollView = scrollView;
	}
	
	public void setRightDateFieldOfTimePicker(float value) {
		this.RightDateFieldOfTimePicker = value;
	}
	
	public void setLeftMeridiemFieldOfTimePicker(float value) {
		this.LeftMeridiemFieldOfTimePicker = value;
	}
	
	public void setRightMeridiemFieldOfTimePicker(float value) {
		this.RightMeridiemFieldOfTimePicker = value;
	}
	
	public void setLeftHourFieldOfTimePicker(float value) {
		this.LeftHourFieldOfTimePicker = value;
	}
	
	public void setRightHourFieldOfTimePicker(float value) {
		this.RightHourFieldOfTimePicker = value;
	}
	
	public void setTimePickerNumeralTextBitmapsList(ArrayList<Bitmap> List) {
		this.TimePickerNumeralTextBitmapsList = List;
	}
	
	public void setTimePickerTodayTextBitmapsList(ArrayList<Bitmap> List) {
		this.TimePickerTodayTextBitmapsList = List;
	}
	
	public void setTimePickerMeridiemTextBitmapsList(ArrayList<Bitmap> List) {
		this.TimePickerMeridiemTextBitmapsList = List;
	}
	
	public void setTimePickerDayOfWeekTextBitmapsList(ArrayList<Bitmap> List) {
		this.TimePickerDayOfWeekTextBitmapsList = List;
	}
	
	public void setTimePickerCenterDayOfWeekTextBitmapsList(ArrayList<Bitmap> List) {
		this.TimePickerCenterDayOfWeekTextBitmapsList = List;
	}
	
	public void setTimePickerCenterTodayTextBitmapsList(ArrayList<Bitmap> List) {
		this.TimePickerCenterTodayTextBitmapsList = List;
	}
	
	public void setTimePickerCenterMeridiemTextBitmapsList(ArrayList<Bitmap> List) {
		this.TimePickerCenterMeridiemTextBitmapsList = List;
	}
	
	public void setTimePickerCenterNumeralBitmapsList(ArrayList<Bitmap> List) {
		this.TimePickerCenterNumeralBitmapsList = List;
	}
	
	
	public void setTimePickerMonthTextBitmap(Bitmap bitmap) {
		this.TimePickerMonthTextBitmap = bitmap;
	}
	
	public void setTimePickerDateTextBitmap(Bitmap bitmap) {
		this.TimePickerDateTextBitmap = bitmap;
	}	
	
	public void setTimePickerCenterMonthTextBitmap(Bitmap bitmap) {
		this.TimePickerCenterMonthTextBitmap = bitmap;
	}
	
	public void setTimePickerCenterDateTextBitmap(Bitmap bitmap) {
		this.TimePickerCenterDateTextBitmap = bitmap;
	}
	
	public void setEraseBackgroundBitmap(Bitmap bitmap) {
		this.EraseBackgroundBitmap = bitmap;
	}
	
	public float getTimePickerTextWdith() {
		return this.TimePickerTextWdit;
	}
	
	public float getTimePickerNumeralWidth() {
		return this.TimePickerNumeralWidth;
	}
	
	public float getTimePickerDateFieldCircleDiameterPixels() {
		return this.TimePickerDateFieldCircleDiameterPixels;
	}
	
	public float getPpi() {
		return this.Ppi;
	}
	
	public LockableScrollView getMainPageScrollView() {
		return this.MainPageScrollView;
	}
	
	public float getRightDateFieldOfTimePicker() {
		return this.RightDateFieldOfTimePicker;
	}
	
	public float getLeftMeridiemFieldOfTimePicker() {
		return this.LeftMeridiemFieldOfTimePicker;
	}
	
	public float getRightMeridiemFieldOfTimePicker() {
		return this.RightMeridiemFieldOfTimePicker;
	}
	
	public float getLeftHourFieldOfTimePicker() {
		return this.LeftHourFieldOfTimePicker;
	}
	
	public float getRightHourFieldOfTimePicker() {
		return this.RightHourFieldOfTimePicker;
	}	
	
	
	public ArrayList<Bitmap> getTimePickerNumeralTextBitmapsList() {
		return this.TimePickerNumeralTextBitmapsList;
	}
	
	public ArrayList<Bitmap> getTimePickerTodayTextBitmapsList() {
		return this.TimePickerTodayTextBitmapsList;
	}
	
	public ArrayList<Bitmap> getTimePickerMeridiemTextBitmapsList() {
		return this.TimePickerMeridiemTextBitmapsList;
	}
	
	public ArrayList<Bitmap> getTimePickerDayOfWeekTextBitmapsList() {
		return this.TimePickerDayOfWeekTextBitmapsList;
	}
	
	public ArrayList<Bitmap> getTimePickerCenterDayOfWeekTextBitmapsList() {
		return this.TimePickerCenterDayOfWeekTextBitmapsList;
	}
	
	public ArrayList<Bitmap> getTimePickerCenterTodayTextBitmapsList() {
		return this.TimePickerCenterTodayTextBitmapsList;
	}
	
	public ArrayList<Bitmap> getTimePickerCenterMeridiemTextBitmapsList() {
		return this.TimePickerCenterMeridiemTextBitmapsList;
	}
	
	public ArrayList<Bitmap> getTimePickerCenterNumeralBitmapsList() {
		return this.TimePickerCenterNumeralBitmapsList;
	}	
		
	public Bitmap getTimePickerMonthTextBitmap() {
		return this.TimePickerMonthTextBitmap;
	}
	
	public Bitmap getTimePickerDateTextBitmap() {
		return this.TimePickerDateTextBitmap;
	}	
	
	public Bitmap getTimePickerCenterMonthTextBitmap() {
		return this.TimePickerCenterMonthTextBitmap;
	}
	
	public Bitmap getTimePickerCenterDateTextBitmap() {
		return this.TimePickerCenterDateTextBitmap;
	}
	
	public Bitmap getEraseBackgroundBitmap() {
		return this.EraseBackgroundBitmap;
	}
}
