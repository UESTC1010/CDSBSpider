package com.uestc.zaobao.www;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ZAOBAOTimeTask extends TimerTask{
	public void run() {
		System.out.println("联合早报新闻运行开始...");
		ZAOBAOGuoNei testGuoNei = new ZAOBAOGuoNei();
		testGuoNei.getZAOBAOGuoNei();
		ZAOBAOGuoJi testGuoJi = new ZAOBAOGuoJi();
		testGuoJi.getZAOBAOGuoJi();
		System.out.println("联合早报程序运行完成...");
		System.out.println("现在时间为："+new Date());

		
	}
	public static void main(String[] args){

		Timer timer = new Timer();
		timer.schedule(new ZAOBAOTimeTask(), 0,60*1000);
	}
}
