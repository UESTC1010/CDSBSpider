package com.uestc.local.www;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
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

/*
 * 四川新闻网
 * url:http://scnews.newssc.org/2009scxw/
 * http://scnews.newssc.org/2009cdxw/
 * http://scnews.newssc.org/2009bwyc/
 * 
 * */
public class SCNEWS {

	private String DBName ;   //sql name
	private String DBTable ;  // collections name
	private String ENCODE ;   //html encode gb2312
		
	//新闻主题links的正则表达式
	private String newsThemeLinksReg ; //= "http://news.163.com/special/0001124J/guoneinews_[0-9]{1,2}.html#headList";
			
	//新闻内容links的正则表达式
	private String newsContentLinksReg ; //= "http://news.163.com/[0-9]{2}/[0-9]{4}/[0-9]{2}/(.*?).html#f=dlist";
		
	//新闻主题link
	private String theme ;
	//downloadTime
	private String downloadTime;
	Calendar today = Calendar.getInstance();
	private int year = today.get(Calendar.YEAR);
	private int month = today.get(Calendar.MONTH)+1;
	private int date = today.get(Calendar.DATE);	
	//图片个数
	private int imageNumber = 1;
	public void getSCXWNews(){
		System.out.println("scxww start...");
		DBName = "LOCALNEWS";
		DBTable = "SCXWW";
		ENCODE = "gb2312";
		String[] newsTitleLabel = new String[]{"title",""};     //新闻标题标签 t
		String[] newsContentLabel = new String[]{"id" ,"content14"};  //新闻内容标签 
		String[] newsTimeLabel = new String[]{"id","pubtime_baidu"};   //新闻时间 
		String[] newsSourceLabel =new String[]{"id","source_baidu","四川新闻网"}; //来源
		String[] newsCategroyLabel = new String[]{"class","black12"} ; // 属性
//		String monthBuf = null;
//		String dateBuf = null;
		//计算获取新闻的时间
		if( month < 10){
			downloadTime = year+"0"+month;
//			monthBuf = "0" + month;	
		}else 
			downloadTime = year+""+month;
		if(date < 10){
			downloadTime += "0" + date;
//			dateBuf = "0" + date ;
		}else {
			downloadTime += date ;
//			dateBuf = "" +date ;
		}
//		System.out.println("111");
		CRUT crut = new CRUT(DBName ,DBTable);
		//四川新闻 首页链接 
		theme = "http://scnews.newssc.org/2009scxw/";
		//成都新闻
		String theme1 = "http://scnews.newssc.org/2009cdxw/";
		String theme2 = "http://scnews.newssc.org/2009bwyc/";
		
		//新闻内容links的正则表达式 http://scnews.newssc.org/system/20150120/000530755.html
		newsContentLinksReg = "http://scnews.newssc.org/system/"+downloadTime+"/[0-9]{9,10}.html";
//		System.out.println("222");
		//保存国内新闻主题links
		Queue<String> guoNeiNewsTheme = new LinkedList<String>();
		guoNeiNewsTheme.offer(theme);
		guoNeiNewsTheme.offer(theme1);
		guoNeiNewsTheme.offer(theme2);

		//获取国内新闻内容links
		Queue<String>guoNeiNewsContent = new LinkedList<String>();
		guoNeiNewsContent = findContentLinks(guoNeiNewsTheme,newsContentLinksReg);
//		System.out.println(guoNeiNewsContent);
		//获取每个新闻网页的html
		if(guoNeiNewsContent == null || guoNeiNewsContent.isEmpty()){
			crut.destory();
			return ;
		}
//		System.out.println("mmmmmm");
		while(!guoNeiNewsContent.isEmpty()){
			String url = guoNeiNewsContent.poll();
			if(!crut.query("Url", url)){
				Date date = new Date();
				String html = findContentHtml(url);  //获取新闻的html
				if(html!=null){
//					System.out.println("sssssssssss");
					crut.add(findNewsTitle(html,newsTitleLabel,"-四川新闻-四川新闻网"), findNewsOriginalTitle(html,newsTitleLabel,"-四川新闻-四川新闻网"),findNewsOriginalTitle(html,newsTitleLabel,"-四川新闻-四川新闻网"), findNewsTime(html,newsTimeLabel),findNewsContent(html,newsContentLabel), findNewsSource(html,newsSourceLabel),
							findNewsOriginalSource(html,newsSourceLabel), findNewsCategroy(html,newsCategroyLabel), findNewsOriginalCategroy(html,newsCategroyLabel), url, findNewsImages(html,newsTimeLabel),downloadTime,date);
//					System.out.println("ttttt");
				}
		
				
			}
//			System.out.println(i);
		}
		crut.destory();
		System.out.println("scxww over...");
	}
	
	public Queue<String> findThemeLinks(String themeLink ,String themeLinkReg) {
		
		Queue<String> themelinks = new LinkedList<String>();
//		Pattern newsThemeLink = Pattern.compile(themeLinkReg);
//		themelinks.offer(themeLink);
//		
//		try {
//				Parser parser = new Parser(themeLink);
//				parser.setEncoding(ENCODE);
//				@SuppressWarnings("serial")
//				NodeList nodeList = parser.extractAllNodesThatMatch(new NodeFilter(){
//					public boolean accept(Node node)
//					{
//						if (node instanceof LinkTag)// 标记
//							return true;
//						return false;
//					}});
//				
//				for (int i = 0; i < nodeList.size(); i++)
//				{
//				
//					LinkTag n = (LinkTag) nodeList.elementAt(i);
////		        	System.out.print(n.getStringText() + "==>> ");
////		       	 	System.out.println(n.extractLink());
//					//新闻主题
//					Matcher themeMatcher = newsThemeLink.matcher(n.extractLink());
//					if(themeMatcher.find()){
//						if(!themelinks.contains(n.extractLink()))
//							themelinks.offer(n.extractLink());
//		        	}
//				}
//			}catch(ParserException e){
//				return null;
//			}catch(Exception e){
//				return null;
//			}
		return themelinks ;
	}

	public Queue<String> findContentLinks(Queue<String> themeLink ,String contentLinkReg) {
		
		Queue<String> contentlinks = new LinkedList<String>(); // 临时征用
//		Exception bufException = null ;
//		System.out.println("qqq");
//		Pattern newsContent = Pattern.compile(contentLinkReg);
		while(!themeLink.isEmpty()){
			
			String buf = themeLink.poll();
			
			String htmlString = findContentHtml(buf);
			if(htmlString != null){
				Pattern newslinks = Pattern.compile(contentLinkReg);
				Matcher newsMatcher = newslinks.matcher(htmlString);
				while(newsMatcher.find()){
					String bufUrl = newsMatcher.group();
					if(!contentlinks.contains(bufUrl))
						contentlinks.offer(bufUrl);
					
				}
			}
		
//			try {
//				System.out.println("www");
//				Parser parser = new Parser(buf);
//				System.out.println("eeee");
//				parser.setEncoding(ENCODE);
//				@SuppressWarnings("serial")
//				NodeList nodeList = parser.extractAllNodesThatMatch(new NodeFilter(){
//					public boolean accept(Node node)
//					{
//						if (node instanceof LinkTag)// 标记
//							return true;
//						return false;
//					}
//		
//				});
//				System.out.println("rrrrr");
//			
//				for (int i = 0; i < nodeList.size(); i++)
//				{
//			
//					LinkTag n = (LinkTag) nodeList.elementAt(i);
////	        	System.out.print(n.getStringText() + "==>> ");
////	       	 	System.out.println(n.extractLink());
//					//新闻主题
//					Matcher themeMatcher = newsContent.matcher(n.extractLink());
//					if(themeMatcher.find()){
//					
//						if(!contentlinks.contains(n.extractLink()))
//							contentlinks.offer(n.extractLink());
//					}
//				}
//			}catch(ParserException e){
//				bufException = e ;
//			}catch(Exception e){
//				bufException = e ;
//			}finally{
//				if(bufException != null ){
//					System.out.println("ttttt");
//					return null;
//				}
//			}		
		}
//		System.out.println(contentlinks);
//		System.out.println("yyyyy");
		return contentlinks;
	}
	
	public String findContentHtml(String url) {
		Exception bufException = null ;
		String html = null;                 //网页html
		HttpURLConnection httpUrlConnection = null ;
	    InputStream inputStream;
	    BufferedReader bufferedReader;
	    
		int state = 0;
		//判断url是否为有效连接
		try{
//			System.out.println("可能出现的链接暂停---0");
			httpUrlConnection = (HttpURLConnection) new URL(url).openConnection(); //创建连接
			state = httpUrlConnection.getResponseCode();
			httpUrlConnection.disconnect();
//			System.out.println("可能出现的链接暂停未遂---0");
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
//        	System.out.println("可能出现的链接暂停---1");
        	httpUrlConnection = (HttpURLConnection) new URL(url).openConnection(); //创建连接
        	httpUrlConnection.setRequestMethod("GET");
        	httpUrlConnection.setConnectTimeout(3000);
			httpUrlConnection.setReadTimeout(1000);
            httpUrlConnection.setUseCaches(true); //使用缓存
            httpUrlConnection.connect();           //建立连接  链接超时处理
//            System.out.println("可能出现的链接未遂---1");
        } catch (Exception e) {
        	System.out.println(url+"该链接访问超时...");
        	bufException = e ;
        }finally{
        	if(bufException != null)
        		return null ;
        }
  
        try {
//        	System.out.println("可能出现的链接暂停---2");
            inputStream = httpUrlConnection.getInputStream(); //读取输入流
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, ENCODE)); 
            String string;
            StringBuffer sb = new StringBuffer();
            while ((string = bufferedReader.readLine()) != null) {
            	sb.append(string);
            	sb.append("\n");
            }
            html = sb.toString();
//            System.out.println("可能出现的链接暂停未遂---2");
        } catch (IOException e) {
//            e.printStackTrace();
        }finally{
        	httpUrlConnection.disconnect();
        }
//        System.out.println(html);
		return html;
	}
	
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
					buf += textnode1.toPlainTextString();
					if(buf.contains("&nbsp;"))
						buf = buf.replaceAll("&nbsp;", " ");
				}
			}
		}catch(Exception e){
		   
		   
		}
		return buf ;
	}
	
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
					buf += textnode1.toPlainTextString();
					if(buf.contains("&nbsp;"))
						buf = buf.replaceAll("&nbsp;", " ");
				}
			}
		}catch(Exception e){
 
		}
		return buf ;
	}
	//news title
	public String findNewsTitle(String html , String[] label,String buf) {
		String titleBuf ;
		if(label[1].equals("")){
			titleBuf = HandleHtml(html,label[0]);
		}else{
			titleBuf = HandleHtml(html,label[0],label[1]);
		}
		if(titleBuf!=null){
			if(titleBuf.contains(buf))
				titleBuf = titleBuf.substring(0, titleBuf.indexOf(buf))	;
			else if(titleBuf.contains("-本网原创_四川新闻")){
				titleBuf = titleBuf.substring(0, titleBuf.indexOf("-本网原创_四川新闻")) ;
			}
		}
		return titleBuf;
	}
	//news 未处理标题
	public String findNewsOriginalTitle(String html , String[] label,String buf) {
		// TODO Auto-generated method stub
		String titleBuf ;
		if(label[1].equals("")){
			titleBuf = HandleHtml(html,label[0]);
		}else{
			titleBuf = HandleHtml(html,label[0],label[1]);
		}
		if(titleBuf!=null){
			if(titleBuf.contains(buf)){
				titleBuf = titleBuf.substring(0, titleBuf.indexOf(buf)+buf.length())	;
			}else if(titleBuf.contains("-本网原创_四川新闻")){
				titleBuf = titleBuf.substring(0, titleBuf.indexOf("-本网原创_四川新闻")+10) ;
			}
		}
		return titleBuf;
	}
	public String findNewsContent(String html , String[] label) {
		// TODO Auto-generated method stub
		if(html == null)
			return null;
		String contentBuf;
		if(label[1].equals("")){
			contentBuf = HandleHtml(html,label[0]);
		}else{
			contentBuf = HandleHtml(html,label[0],label[1]);
			if(contentBuf==null||contentBuf.equals("")){
				contentBuf = HandleHtml(html,"class","content");
			}
		}
		
//		System.out.println(html);
		if(contentBuf==null||contentBuf.equals("")){
			
			if(html.contains("<section>")&&html.contains("</section>")){
				contentBuf = html.substring(html.indexOf("<section>")+9, html.indexOf("</section>"));
			}else if(html.contains("class=\"content\">")&&(html.contains("[编辑"))){
				contentBuf = html.substring(html.indexOf("class=\"content\">")+16, html.indexOf("[编辑"));
				
			}
		}
		if(contentBuf!=null){
			contentBuf = contentBuf.replaceAll("<(.*?)>", "");
			contentBuf = contentBuf.replaceFirst("\\s+", "");
			contentBuf = contentBuf.replaceAll("&nbsp;", "");
		}
		return contentBuf;
	}
	//处理图片，使用时间label
	public String findNewsImages(String html , String[] label) {
		if(html == null )
			return null;
		String bufHtml = "";        //辅助
		String imageNameTime  = "";

		if(html.contains("<section>"))
			bufHtml = html.substring(html.indexOf("<section>"), html.indexOf("</section>"));
		else{
			bufHtml = html;
		}
			
		//获取图片时间，为命名服务
		imageNameTime = findNewsTime(html,label) ;
		if(imageNameTime == null ||imageNameTime.length() < 8)
			return null;
		//处理存放条图片的文件夹
    	File f = new File("SCXWW");
    	if(!f.exists()){
    		f.mkdir();
    	}
    	//加入具体时间 时分秒 防止图片命名重复
    	Calendar photoTime = Calendar.getInstance();
    	int photohour = photoTime.get(Calendar.HOUR_OF_DAY); 
    	int photominute = photoTime.get(Calendar.MINUTE);
    	int photosecond = photoTime.get(Calendar.SECOND);
    	//保存图片文件的位置信息
    	Queue<String> imageLocation = new LinkedList<String>();
    	//图片正则表达式
		String imageReg = "http://pic.newssc.org/upload/((news)|(ori))/"+imageNameTime+"/(.*?).jpg";
		Pattern newsImage = Pattern.compile(imageReg);
		Matcher imageMatcher = newsImage.matcher(bufHtml);
		//处理图片
		int i = 1 ;      //本条新闻图片的个数
		while(imageMatcher.find()){
			String bufUrl = imageMatcher.group();
//			System.out.println(bufUrl);
			File fileBuf;
//			imageMatcher.group();
			String imageNameSuffix = bufUrl.substring(bufUrl.lastIndexOf("."), bufUrl.length());  //图片后缀名
			try{
				URL uri = new URL(bufUrl);  
			
				InputStream in = uri.openStream();
				FileOutputStream fo;
				if(imageNumber < 10){
					fileBuf = new File("SCXWW",imageNameTime+photohour+photominute+photosecond+"000"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf); 
					imageLocation.offer(fileBuf.getPath());
				}else if(imageNumber < 100){
					fileBuf = new File("SCXWW",imageNameTime+photohour+photominute+photosecond+"00"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
            
				}else if(imageNumber < 1000){
					fileBuf = new File("SCXWW",imageNameTime+photohour+photominute+photosecond+"0"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
  
				}else{
					fileBuf = new File("SCXWW",imageNameTime+photohour+photominute+photosecond+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
				}
            
				byte[] buf = new byte[1024];  
				int length = 0;  
//           	 System.out.println("开始下载:" + url);  
				while ((length = in.read(buf, 0, buf.length)) != -1) {  
					fo.write(buf, 0, length);  
				}  
				in.close();  
				fo.close();  
//          	  System.out.println(imageName + "下载完成"); 
			}catch(Exception e){
				System.out.println("亲，图片下载失败！！"+e);
				System.out.println("请检查网络是否正常！");
			}
			i ++;
			
        }  
		//如果该条新闻没有图片则图片的编号不再增加
		if(!imageLocation.isEmpty())
			imageNumber ++;
		return imageLocation.toString();
	}
	//新闻时间
	public String findNewsTime(String html , String[] label) {
		// TODO Auto-generated method stub
		String timeBuf ="";
		if(label[1].equals("")){
			timeBuf = HandleHtml(html , label[0]);
		}else{
			timeBuf = HandleHtml(html , label[0],label[1]);
		}
		if(timeBuf!=null){
			timeBuf = timeBuf.replaceAll("[^0-9]", "");
			if(timeBuf.length() >= 8)
				timeBuf = timeBuf.substring(0, 8);
			else
				timeBuf = null;
		}
		return timeBuf;
	}
	public String findNewsSource(String html ,String[] label) {
		// TODO Auto-generated method stub
		if(label.length == 3 && (!label[2].equals("")))
			return label[2];
		else
			return null;
	}
	public String findNewsOriginalSource(String html ,String[] label) {
		// TODO Auto-generated method stub
		String sourceBuf;
		if(label[1].equals("")){
			sourceBuf = HandleHtml(html , label[0]);
		}else{
			sourceBuf = HandleHtml(html , label[0],label[1]);
		}
		if(label.length == 3 && (!label[2].equals(""))&& sourceBuf!=null)
			return label[2]+"-"+sourceBuf;
		else
			return sourceBuf;
	}
	public String findNewsCategroy(String html , String[] label) {
		// TODO Auto-generated method stub
		String categroyBuf ="";
		if(label[1].equals("")){
			categroyBuf = HandleHtml(html , label[0]);
		}else{
			categroyBuf = HandleHtml(html , label[0],label[1]);
		}
		if(categroyBuf!=null){	
			categroyBuf = categroyBuf.replaceAll("\\s+", "");
			if(categroyBuf!=null && categroyBuf.contains("四川新闻"))
				categroyBuf = categroyBuf.substring(categroyBuf.indexOf("四川新闻")+5, categroyBuf.length());
			if(categroyBuf.equals(""))
				return "四川新闻";
		}
		return categroyBuf;
	}
	public String findNewsOriginalCategroy(String html , String[] label) {
		// TODO Auto-generated method stub
		String categroyBuf ="";
		if(label[1].equals("")){
			categroyBuf = HandleHtml(html , label[0]);
		}else{
			categroyBuf = HandleHtml(html , label[0],label[1]);
		}
		if(categroyBuf==null||categroyBuf.equals(""))
			return "四川新闻";
		return categroyBuf;
	}

	public static void main(String[] args){
		while(true){
		SCNEWS test = new SCNEWS();
		test.getSCXWNews();
		try {
			Thread.sleep(5*60*1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
	}
}
