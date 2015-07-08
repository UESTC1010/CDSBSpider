package com.uestc.ifeng.www;

import java.util.Queue;

public interface IFENGCOMMENT {
	
	
	public Queue<String> findThemeLinks(String themeLink ,String themeLinkReg) ; //获取主题链接
	
	public Queue<String> findContentLinks(Queue<String> themeLink ,String ContentLinkReg) ;  //获取内容链接
	
	public String handleCommentUrl(String url);          //获取comment url
	
	public String findCommentHtml(String commentUrl);    //获取新闻评论页的html
	
	public String HandleHtml(String html,String one);  //处理一个参数的标签的html
	
	public String HandleHtml(String html ,String one,String two);  //处理两个参数的标签
	
	public Queue<String> handleComment(String commentHtml, String[] label);
}
