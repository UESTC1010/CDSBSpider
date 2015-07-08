package com.uestc.sohu.www;

import java.util.Queue;

public interface SOHUCOMMENT {
	
	public Queue<String> findThemeLinks(String themeLink ,String themeLinkReg) ; //获取主题链接
	
	public Queue<String> findContentLinks(Queue<String> themeLink ,String ContentLinkReg) ;  //获取内容链接
	
	public String findContentHtml(String url); //获取news Html
	
	public String findNewsCommentUrl(String url);  //返回commentUrl
	
	public Queue<String> handleNewsComment(String commentUrl);   //获得comments
}
