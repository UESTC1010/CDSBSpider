package com.uestc.ifeng.www;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import com.uestc.spider.www.CRUT;

public class IFENGSheHuiComment implements IFENGCOMMENT{
	private String DBName ;   //sql name
	private String DBTable ;  // collections name
	private String ENCODE ;   //html encode gb2312	
	//新闻主题links的正则表达式
	private String newsThemeLinksReg ; 
			
	//新闻内容links的正则表达式
	private String newsContentLinksReg ; 
		
	//新闻主题link
	private String theme ;
	Date dateBufDate = new Date();
	//downloadTime
	private String downloadTime;
	Calendar today = Calendar.getInstance();
	private int year = today.get(Calendar.YEAR);
	private int month = today.get(Calendar.MONTH)+1;
	private int date = today.get(Calendar.DATE);	
	
	public void getIFENGSheHuiComment(){
		System.out.println("shehuicomment start...");
		DBName = "IFENGCOMMENT";
		DBTable = "SH";
		ENCODE = "utf-8";
		String[] label = new String[]{"class","textDet"};     //新闻标题标签 t
		//时间辅助参数
		String yearBuf = ""+year;
		String monthBuf = "";
		String dateBuf = "";
		if(month < 10)  //处理日期问题
			monthBuf = "0"+month;
		else
			monthBuf = "" + month;
		if(date < 10)
			dateBuf = "0" + date;
		else 
			dateBuf = ""+ date;
		CRUT crut = new CRUT(DBName ,DBTable);
		//社会新闻 首页链接http://news.ifeng.com/listpage/7837/20141220/2/rtlist.shtml
		String theme1 = "http://news.ifeng.com/listpage/7837/"+yearBuf+monthBuf+dateBuf+"/"+1+"/rtlist.shtml";
		String theme2 = "http://news.ifeng.com/listpage/7837/"+yearBuf+monthBuf+dateBuf+"/"+2+"/rtlist.shtml";
		//新闻主题links的正则表达式 保留后期使用
//		newsThemeLinksReg ;
		
		//新闻内容links的正则表达式  http://news.ifeng.com/a/20141212/42699636_0.shtml
		newsContentLinksReg = "http://news.ifeng.com/a/"+yearBuf+monthBuf+dateBuf+"/[0-9]{7,9}_0.shtml";
		//保存社会新闻主题links
		Queue<String> sheHuiNewsTheme = new LinkedList<String>();
		sheHuiNewsTheme.offer(theme1);
		sheHuiNewsTheme.offer(theme2);
//		System.out.println(guoNeiNewsTheme);
		
		//获取社会新闻内容links
		Queue<String>sheHuiNewsContent = new LinkedList<String>();
		sheHuiNewsContent = findContentLinks(sheHuiNewsTheme,newsContentLinksReg);
//		System.out.println(guoNeiNewsContent);
		if(sheHuiNewsContent == null ){
			crut.destory();
			return ;
		}
		//获取每个新闻网页的html
		//计算获取新闻的时间
		downloadTime = yearBuf+monthBuf+dateBuf;
		while(!sheHuiNewsContent.isEmpty()){
			String url = sheHuiNewsContent.poll();
//			System.out.println(url);
			if(!crut.query("Url", url)){
				String commentUrl = handleCommentUrl(url);
//				System.out.println(commentUrl);
				String commentHtml = findCommentHtml(commentUrl);
				handleComment(commentHtml,label);
				crut.add(url, commentUrl, handleComment(commentHtml,label),dateBufDate);
//				System.out.println(html);
			}else{
				String commentUrl = handleCommentUrl(url);
				String commentHtml = findCommentHtml(commentUrl);
				handleComment(commentHtml,label);
				crut.update(url, commentUrl, handleComment(commentHtml,label),dateBufDate);
			}
		}
		crut.destory();
		System.out.println("shehuicomment over...");
	
	
	}
	@Override
	public Queue<String> findThemeLinks(String themeLink, String themeLinkReg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Queue<String> findContentLinks(Queue<String> themeLink,String ContentLinkReg) {
		Queue<String> contentlinks = new LinkedList<String>(); // 临时征用
		Exception bufeException = null ;
		Pattern newsContent = Pattern.compile(ContentLinkReg);
		while(!themeLink.isEmpty()){
			
			String buf = themeLink.poll();
		
			try {
				Parser parser = new Parser(buf);
				parser.setEncoding(ENCODE);
				@SuppressWarnings("serial")
				NodeList nodeList = parser.extractAllNodesThatMatch(new NodeFilter(){
					public boolean accept(Node node)
					{
						if (node instanceof LinkTag)// 标记
							return true;
						return false;
					}
		
				});
			
				for (int i = 0; i < nodeList.size(); i++)
				{
			
					LinkTag n = (LinkTag) nodeList.elementAt(i);
//	        	System.out.print(n.getStringText() + "==>> ");
//	       	 	System.out.println(n.extractLink());
					//新闻主题
					Matcher themeMatcher = newsContent.matcher(n.extractLink());
					if(themeMatcher.find()){
					
						if(!contentlinks.contains(n.extractLink()))
							contentlinks.offer(n.extractLink());
					}
				}
			}catch(ParserException e){
				bufeException = null ;
			}catch(Exception e){
				bufeException = null ;
			}finally{
				if(bufeException != null )
					return null;
			}		
		}
//		System.out.println(contentlinks);
		return contentlinks;
	}
	@Override
	public String handleCommentUrl(String url) {

		String s1 = "http://comment.ifeng.com/view.php?docUrl=http%3A%2F%2Fnews.ifeng.com%2Fa%2F";
		String s2 = "%2F";
		String s3 = "_0.shtml"; 
		return s1 + url.substring(url.indexOf("a/")+2, url.lastIndexOf("/"))+s2
				+url.substring(url.lastIndexOf("/")+1, url.lastIndexOf("_0.shtml"))+s3;
	}
	
	@Override
	public String findCommentHtml(String commentUrl) {
		String html = null;                 //网页html
		Exception bufeException = null ;
		HttpURLConnection httpUrlConnection = null;
	    InputStream inputStream;
	    BufferedReader bufferedReader;
	    
		int state = 0;
		//判断url是否为有效连接
		try{
			httpUrlConnection = (HttpURLConnection) new URL(commentUrl).openConnection(); //创建连接
			state = httpUrlConnection.getResponseCode();
			httpUrlConnection.disconnect();
		}catch (MalformedURLException e) {
//          e.printStackTrace();
			System.out.println("该连接"+commentUrl+"网络有故障，已经无法正常链接，无法获取新闻");
			bufeException = e ;
		} catch (IOException e) {
          // TODO Auto-generated catch block
//          e.printStackTrace();
			System.out.println("该连接"+commentUrl+"网络超级慢，已经无法正常链接，无法获取新闻");
			bufeException = e ;
		}finally{
			if(bufeException != null )
				return null ;
		}
		if(state != 200 && state != 201){
			return null;
		}
  
        try {
        	httpUrlConnection = (HttpURLConnection) new URL(commentUrl).openConnection(); //创建连接
        	httpUrlConnection.setRequestMethod("GET");
            httpUrlConnection.setUseCaches(true); //使用缓存
            httpUrlConnection.connect();           //建立连接  链接超时处理
        } catch (IOException e) {
        	System.out.println("该链接访问超时...");
        	bufeException = e ;
        }finally{
        	if(bufeException != null )
        		return null;
        }
  
        try {
            inputStream = httpUrlConnection.getInputStream(); //读取输入流
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, ENCODE)); 
            String string;
            StringBuffer sb = new StringBuffer();
            while ((string = bufferedReader.readLine()) != null) {
            	sb.append(string);
            	sb.append("\n");
            }
            html = sb.toString();
        } catch (IOException e) {
//            e.printStackTrace();
        }
//        System.out.println(html);
		return html;
	}

	@Override
	public String HandleHtml(String html, String one) {
		if(html == null )
			return null;
		NodeFilter filter = new HasAttributeFilter(one);
		String buf = "";
		try{
			Parser parser = Parser.createParser(html, ENCODE);
			NodeList nodes = parser.extractAllNodesThatMatch(filter);
   		
			if(nodes!=null) {
				for (int i = 0; i < nodes.size(); i++) {
					Node textnode1 = (Node) nodes.elementAt(i);
					buf += textnode1.toPlainTextString()+"\n";
					if(buf.contains("&nbsp;"))
						buf = buf.replaceAll("&nbsp;", "\n");
				}
			}
		}catch(Exception e){
		   
		   
		}
		return buf ;
	}
	
	@Override
	public String HandleHtml(String html, String one, String two) {
		if(html == null )
			return null;
		NodeFilter filter = new HasAttributeFilter(one,two);
		String buf = "";
		try{
			Parser parser = Parser.createParser(html, ENCODE);
			NodeList nodes = parser.extractAllNodesThatMatch(filter);
   		
			if(nodes!=null) {
				for (int i = 0; i < nodes.size(); i++) {
					Node textnode1 = (Node) nodes.elementAt(i);
					buf += textnode1.toPlainTextString()+"\n";
					if(buf.contains("&nbsp;"))
						buf = buf.replaceAll("&nbsp;", "\n");
				}
			}
		}catch(Exception e){
 
		}
		return buf ;
	}

	@Override
	public Queue<String> handleComment(String commentHtml , String[] label) {
		Queue<String> result = new LinkedList<String>();
		String comment = "";
		if(label[1] != ""){
			comment = HandleHtml(commentHtml,label[0],label[1]);
		}else{
			comment = HandleHtml(commentHtml,label[0]);
		}
		if(comment!= null ){
			comment = comment.replaceAll("\\t", "");
			comment = comment.replaceAll("(\n)+", "\n");
			comment = comment.replaceAll("推荐(.*?)复制\n", "");
			comment = comment.replaceAll("发表日期(.*?)网友：", "");
			comment = comment.replaceAll("\n手机用户\n","");
			String bufReg = "(.*?)\n";
			Pattern tt = Pattern.compile(bufReg);
			Matcher bufMatcher = tt.matcher(comment);
			while(bufMatcher.find()){
				result.offer(bufMatcher.group()+"--"+dateBufDate);
			}
		}
		return result;
	}
	public static void main(String[] args){
		IFENGSheHuiComment test = new IFENGSheHuiComment();
		test.getIFENGSheHuiComment();
	}
}
