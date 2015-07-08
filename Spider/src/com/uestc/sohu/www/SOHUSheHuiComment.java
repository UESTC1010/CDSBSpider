package com.uestc.sohu.www;

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
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.SessionNotFoundException;

import com.uestc.spider.www.CRUT;

public class SOHUSheHuiComment implements SOHUCOMMENT{

	private String DBName ;   //sql name
	private String DBTable ;  // collections name
	private String ENCODE ;   //html encode gb2312	
	//新闻主题links的正则表达式
	private String newsThemeLinksReg ; 
			
	//新闻内容links的正则表达式
	private String newsContentLinksReg ; 
		
	//新闻主题link
	private String theme ;
	//time
	Date bufdDate = new Date();
	//downloadTime
	private String downloadTime;
	Calendar today = Calendar.getInstance();
	private int year = today.get(Calendar.YEAR);
	private int month = today.get(Calendar.MONTH)+1;
	private int date = today.get(Calendar.DATE);	
	//图片计数
	private int imageNumber = 1 ;
	
	public void getSOHUSheHuiComment(){
		System.out.println("shehui start...");
		DBName = "SOHUCOMMENT";
		DBTable = "SH";
		ENCODE = "gb2312";
		
		CRUT crut = new CRUT(DBName ,DBTable);
		//社会新闻 首页链接
		theme = "http://news.sohu.com/shehuixinwen.shtml";
		
		//新闻主题links的正则表达式（待定）
		newsThemeLinksReg = "";
		
		//新闻内容links的正则表达式 
		newsContentLinksReg = "http://news.sohu.com/[0-9]{4}[0-9]{2}[0-9]{2}/n[0-9]{9}.shtml";
		//保存社会新闻主题links
		Queue<String> sheHuiNewsTheme = new LinkedList<String>();
		sheHuiNewsTheme = findThemeLinks(theme,newsThemeLinksReg);
//		System.out.println(guoNeiNewsTheme);
		
		//获取社会新闻内容links
		Queue<String>sheHuiNewsContent = new LinkedList<String>();
		sheHuiNewsContent = findContentLinks(sheHuiNewsTheme,newsContentLinksReg);
		if(sheHuiNewsContent==null){
			crut.destory();
			return ;
		}
		//获取每个新闻网页的html
		//计算获取新闻的时间
		if( month < 10)
			downloadTime = year+"0"+month;
		else 
			downloadTime = year+""+month;
		if(date < 10)
			downloadTime += "0" + date;
		else 
			downloadTime += date ;
		while(!sheHuiNewsContent.isEmpty()){
			String url = sheHuiNewsContent.poll();
			if(!crut.query("Url", url)){
				String commenturl = findNewsCommentUrl(url);
//				System.out.println(commenturl);
//				handleNewsComment(commenturl);
				crut.add(url, commenturl, handleNewsComment(commenturl), bufdDate);
			}else {
				String commenturl = findNewsCommentUrl(url);
//				System.out.println(commenturl);
//				handleNewsComment(commenturl);
				crut.update(url, commenturl, handleNewsComment(commenturl), bufdDate);
			}
		}
		crut.destory();
	}
	@Override
	public Queue<String> findThemeLinks(String themeLink, String themeLinkReg) {
		Queue<String> themelinks = new LinkedList<String>();
		String html = findContentHtml(themeLink);
		html = html.replaceAll("\\s+", "");
		String commentReg = "maxPage=(.*?);var";
		
		Pattern newPage = Pattern.compile(commentReg);
		
		Matcher themeMatcher = newPage.matcher(html);
		String mm = "";
		while(themeMatcher.find()){
			mm = themeMatcher.group();
			mm = mm.substring(8, mm.indexOf(";var"));
		}
		
		String s1 = "http://news.sohu.com/shehuixinwen_";
		String s2 = ".shtml";
		themelinks.offer(themeLink);
		int number = Integer.parseInt(mm) - 1;
		int number1 = number - 2 ;
		for(int i = number ; i > number1 ; i--){
			themelinks.offer(s1+i+s2);
		}
		return themelinks ;
	}

	@Override
	public Queue<String> findContentLinks(Queue<String> themeLink,String ContentLinkReg) {
		Queue<String> contentlinks = new LinkedList<String>(); // 临时征用
		Exception bufException = null ;
		Pattern newsContent = Pattern.compile(ContentLinkReg);
		while(!themeLink.isEmpty()){
			try {
				Parser parser = new Parser(themeLink.poll());
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
			
				for (int i = 0 ; i < nodeList.size(); i++)
				{
			
					LinkTag n = (LinkTag) nodeList.elementAt(i);
					//新闻主题
					Matcher themeMatcher = newsContent.matcher(n.extractLink());
					if(themeMatcher.find()){
						if(!contentlinks.contains(n.extractLink())){
							
							contentlinks.offer(n.extractLink());
						}
					}
				}
			}catch(ParserException e){
				bufException = e ;
			}catch(Exception e){
				bufException = e ;
			}finally{
				if(bufException!=null )
					return null;
			}		
		}
		return contentlinks;
	}

	public String findContentHtml(String url){
		
		String html = null;                 //网页html
		Exception bufException = null ;
		HttpURLConnection httpUrlConnection = null ;
	    InputStream inputStream;
	    BufferedReader bufferedReader;
	    
		int state = 0 ;
		//判断url是否为有效连接
		try{
			httpUrlConnection = (HttpURLConnection) new URL(url).openConnection(); //创建连接
			state = httpUrlConnection.getResponseCode();
			httpUrlConnection.disconnect();
		}catch (MalformedURLException e) {
//          e.printStackTrace();
			System.out.println("该连接"+url+"网络有故障，已经无法正常链接，无法获取新闻");
			bufException = e ;
		} catch (IOException e) {
          // TODO Auto-generated catch block
//          e.printStackTrace();
			System.out.println("该连接"+url+"网络超级慢，已经无法正常链接，无法获取新闻");
			bufException = e ;
		}finally{
			if(bufException != null )
				return null;
		}
		if(state != 200 && state != 201){
			return null;
		}
  
        try {
        	httpUrlConnection = (HttpURLConnection) new URL(url).openConnection(); //创建连接
        	httpUrlConnection.setRequestMethod("GET");
            httpUrlConnection.setUseCaches(true); //使用缓存
            httpUrlConnection.connect();           //建立连接  链接超时处理
        } catch (IOException e) {
        	System.out.println("该链接访问超时...");
        	bufException = e ;
        }finally{
        	if(bufException != null )
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
	public String findNewsCommentUrl(String url) {
		// http://quan.sohu.com/pinglun/cyqemw6s1/407248469
		String commentUrl = url.substring(url.lastIndexOf("n")+1, url.lastIndexOf("."));
		return "http://quan.sohu.com/pinglun/cyqemw6s1/"+commentUrl;
	}

	@Override
	public Queue<String> handleNewsComment(String commentUrl) {
		Queue<String> comment = new LinkedList<String>();
		Exception bufeException = null ;
		System.setProperty("webdriver.firefox.bin", "E:/Firefox/firefox.exe");
//		System.getProperties().setProperty("webdriver.chrome.driver", "seleniumjar/chromedriver.exe");
//		WebDriver driver = new ChromeDriver();
		WebDriver driver = new FirefoxDriver();
		try {
			driver.get(commentUrl);
			new Thread().sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			bufeException = e ;
		}catch(TimeoutException e){
			bufeException = e ;
			
		}finally{
			if(bufeException != null )
				return null;
		}
		  
        WebElement webElement;
        String test = null ;
        try{
        	webElement = driver.findElement(By.xpath("//div[@class='topic-changyan']"));  
        	test = webElement.getText();
        }catch(NoSuchElementException e){
        	bufeException = e ;
        }catch(SessionNotFoundException e){
        	bufeException = e ;
        }catch(TimeoutException e){
        	bufeException = e ;
        }finally{
    		driver.close();  
            driver.quit();
            if(bufeException!= null)
            	return null;
        } 
        
        test = test.replaceAll("\\s+", "");
        String commentReg = "](.*?)回复分享";
		
		Pattern newPage = Pattern.compile(commentReg);
		
		Matcher themeMatcher = newPage.matcher(test);
		while(themeMatcher.find()){
			String mm = themeMatcher.group();
			mm = mm.replaceAll("]|(回复分享)", "");
			comment.offer(mm+"--"+ bufdDate);
			System.out.println(mm);
		} 
        return comment;
	}

	public static void main(String[] args){
		SOHUSheHuiComment test = new SOHUSheHuiComment();
		test.getSOHUSheHuiComment();
	}

}
