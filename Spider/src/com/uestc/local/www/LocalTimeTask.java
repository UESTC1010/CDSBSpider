package com.uestc.local.www;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class LocalTimeTask extends TimerTask{
	

	@Override
	public void run() {
		System.out.println("本地新闻程序运行...");
		CDQSS cdqss = new CDQSS();
		cdqss.getCDQSSNews();
		SCNEWS scnews = new SCNEWS();
		scnews.getSCXWNews();
		SCOL scol = new SCOL();
		scol.getSCOLNews();
		System.out.println("本地新闻程序结束...");
		System.out.println("现在时间是："+ new Date() +"\n\n");

	}
	public static void main(String[] args){
		Timer timer = new Timer();
		timer.schedule(new LocalTimeTask(), 0,5*60*1000);
	}

}
