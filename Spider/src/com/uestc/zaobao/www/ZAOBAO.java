package com.uestc.zaobao.www;

import java.util.Queue;

public interface ZAOBAO {

	public Queue<String> getThemeLinks(String themeLink ,String themeLinkReg) ; //获取主题链接
	
	public Queue<String> getContentLinks(Queue<String> themeLink ,String ContentLinkReg) ;  //获取内容链接
	
	public String getContentHtml(String url);    //获取新闻内容页的html
	
	public String getHtml(String html,String one);  //处理一个参数的标签的html
	
	public String getHtml(String html ,String one,String two);  //处理两个参数的标签
	
	public String getNewsTitle(String html ,String[] label,String buf) ; //获取新闻标题
	
	public String getNewsOriginalTitle(String html , String[] label,String buf) ; //获取新闻原始标题
	
	public String getNewsContent(String html , String[] label) ;    //获取新闻内容
	
	public String getNewsImages(String html , String[] label);     //获取新闻图片
	 
	public String getNewsTime(String html , String[] label) ;        //获取新闻发布时间
	
	public String getNewsSource(String html ,String[] label) ;           //新闻来源
	
	public String getNewsOriginalSource(String html ,String[] label) ;     //新闻具体来源
	 
	public String getNewsCategroy(String html , String[] label) ;  //新闻版面属性
	
	public String getNewsOriginalCategroy(String html , String[] label); //新闻具体版面属性
}
