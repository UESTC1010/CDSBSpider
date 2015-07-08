package com.uestc.NETEASE.www;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.uestc.spider.www.CRUT;

public class NETEASETimeTask extends TimerTask{
	@Override
	public void run() {
		System.out.println("网易新闻运行开始...");
		NETEASEGuoNei test = new NETEASEGuoNei();
		test.getNETEASEGuoNeiNews();
		NETEASEGuoJi test1 = new NETEASEGuoJi();
		test1.getNETEASEGuoJiNews();
		NETEASESheHui test2 = new NETEASESheHui();
		test2.getNETEASESheHuiNews();
		NETEASEView test3 = new NETEASEView();
		test3.getNETEASEViewNews();
		NETEASEWar test4 = new NETEASEWar();
		test4.getNETEASEWarNews();
		NETEASEFocus test5 = new NETEASEFocus();
		test5.getNETEASEFocusNews();
		System.out.println("网易新闻程序运行完成...");
		System.out.println("现在时间为："+new Date());

		
	}
	public static void main(String[] args){

		Timer timer = new Timer();
		timer.schedule(new NETEASETimeTask(), 0,60*1000);

	}
	
}
