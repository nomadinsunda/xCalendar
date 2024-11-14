package com.intheeast.timepicker;

import java.util.ArrayList;

import com.intheeast.etc.LockableScrollView;

import android.graphics.Bitmap;



public class BriefTimePickerRenderParameters {
	float TimePickerTextWdit;
	float TimePickerNumeralWidth;	
	float TimePickerDateFieldCircleDiameterPixels;
	float Ppi;
	
	float LeftMonthFieldOfTimePicker;
	float RightMonthFieldOfTimePicker;
	float RightYearFieldOfTimePicker;
	float LeftDayOfWeekFieldOfTimePicker;
	
	ArrayList<Bitmap> TimePickerNumeralTextBitmapsList;
	ArrayList<Bitmap> TimePickerCenterNumeralBitmapsList;
	
	Bitmap TimePickerYearTextBitmap;
	Bitmap TimePickerMonthTextBitmap;
	Bitmap TimePickerDateTextBitmap;
	Bitmap TimePickerCenterYearTextBitmap;
	Bitmap TimePickerCenterMonthTextBitmap;
	Bitmap TimePickerCenterDateTextBitmap;
	Bitmap EraseBackgroundBitmap;
	
	LockableScrollView MainPageScrollView;
	
	public BriefTimePickerRenderParameters() {
		
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
	
	public void setLeftMonthFieldOfTimePicker(float value) {
		this.LeftMonthFieldOfTimePicker = value;
	}
	
	public void setRightMonthFieldOfTimePicker(float value) {
		this.RightMonthFieldOfTimePicker = value;
	}
	
	public void setRightYearFieldOfTimePicker(float value) {
		this.RightYearFieldOfTimePicker = value;
	}
	
	public void setLeftDayOfWeekFieldOfTimePicker(float value) {
		this.LeftDayOfWeekFieldOfTimePicker = value;
	}	
	
	public void setTimePickerNumeralTextBitmapsList(ArrayList<Bitmap> List) {
		this.TimePickerNumeralTextBitmapsList = List;
	}	
	
	
	public void setTimePickerCenterNumeralBitmapsList(ArrayList<Bitmap> List) {
		this.TimePickerCenterNumeralBitmapsList = List;
	}
	
	public void setTimePickerYearTextBitmap(Bitmap bitmap) {
		this.TimePickerYearTextBitmap = bitmap;
	}
	
	public void setTimePickerMonthTextBitmap(Bitmap bitmap) {
		this.TimePickerMonthTextBitmap = bitmap;
	}
	
	public void setTimePickerDateTextBitmap(Bitmap bitmap) {
		this.TimePickerDateTextBitmap = bitmap;
	}
	
	public void setTimePickerCenterYearTextBitmap(Bitmap bitmap) {
		this.TimePickerCenterYearTextBitmap = bitmap;
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
	
	public float getLeftMonthFieldOfTimePicker() {
		return this.LeftMonthFieldOfTimePicker;
	}
	
	public float getRightMonthFieldOfTimePicker() {
		return this.RightMonthFieldOfTimePicker;
	}
	
	public float getRightYearFieldOfTimePicker() {
		return this.RightYearFieldOfTimePicker;
	}
	
	public float getLeftDayOfWeekFieldOfTimePicker() {
		return this.LeftDayOfWeekFieldOfTimePicker;
	}
	
	public ArrayList<Bitmap> getTimePickerNumeralTextBitmapsList() {
		return this.TimePickerNumeralTextBitmapsList;
	}	
	
	public ArrayList<Bitmap> getTimePickerCenterNumeralBitmapsList() {
		return this.TimePickerCenterNumeralBitmapsList;
	}
	
	public Bitmap getTimePickerYearTextBitmap() {
		return this.TimePickerYearTextBitmap;
	}
	
	public Bitmap getTimePickerMonthTextBitmap() {
		return this.TimePickerMonthTextBitmap;
	}
	
	public Bitmap getTimePickerDateTextBitmap() {
		return this.TimePickerDateTextBitmap;
	}
	
	public Bitmap getTimePickerCenterYearTextBitmap() {
		return this.TimePickerCenterYearTextBitmap;
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
