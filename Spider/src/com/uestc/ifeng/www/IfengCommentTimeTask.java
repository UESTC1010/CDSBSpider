package com.uestc.ifeng.www;

import java.util.Date;
import java.util.TimerTask;
import java.util.Timer;

public class IfengCommentTimeTask extends TimerTask{
	@Override
	public void run(){
		System.out.println("凤凰新闻评论运行开始了...");
		IFENGGuoNeiComment test = new IFENGGuoNeiComment();
		test.getIFENGGuoNeiNewsComment();
		IFENGMilComment test1 = new IFENGMilComment();
		test1.getIFENGMilComment();
		IFENGGuoJiComment test2 = new IFENGGuoJiComment();
		test2.getIFENGGuoJiComment();
		IFENGOpinionComment test3 = new IFENGOpinionComment();
		test3.getIFENGOpinionComment();
		IFENGSheHuiComment test4 = new IFENGSheHuiComment();
		test4.getIFENGSheHuiComment();
		IFENGShenDuComment test5 = new IFENGShenDuComment();
		test5.getIFENGShenDuComment();
		System.out.println("凤凰新闻评论程序运行结束...");
		System.out.println("现在时间是："+ new Date());
		
	}
	public static void main(String[] args){
		Timer timer = new Timer();
		timer.schedule(new IfengCommentTimeTask(), 0,60*1000);
	}
}
