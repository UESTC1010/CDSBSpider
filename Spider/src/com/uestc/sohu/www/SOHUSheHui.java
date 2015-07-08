package com.uestc.sohu.www;

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
 * 获取搜狐社会新闻
 * url:http://news.sohu.com/shehuixinwen.shtml
 * 获取每日最新新闻
 * */
public class SOHUSheHui implements SOHU {

	private String DBName ;   //sql name
	private String DBTable ;  // collections name
	private String ENCODE ;   //html encode gb2312	
	//新闻主题links的正则表达式
	private String newsThemeLinksReg ; 
			
	//新闻内容links的正则表达式
	private String newsContentLinksReg ; 
		
	//新闻主题link
	private String theme ;
	//downloadTime
	private String downloadTime;
	Calendar today = Calendar.getInstance();
	private int year = today.get(Calendar.YEAR);
	private int month = today.get(Calendar.MONTH)+1;
	private int date = today.get(Calendar.DATE);	
	//图片计数
	private int imageNumber = 1 ;
	
	public void getSOHUSheHuiNews(){
		System.out.println("shehui start...");
		DBName = "SOHU";
		DBTable = "SH";
		ENCODE = "gb2312";
		String[] newsTitleLabel = new String[]{"title",""};     //新闻标题标签 t
		String[] newsContentLabel = new String[]{"id" ,"contentText"};  //新闻内容标签 "id","endText"
		String[] newsTimeLabel = new String[]{"class","time"};   //新闻时间"class","ep-time-soure cDGray"  
		String[] newsSourceLabel =new String[]{"class","source","搜狐新闻-社会新闻"}; //（3个参数）新闻来源 同新闻时间
		String[] newsCategroyLabel = new String[]{"class","navigation"} ; // 属性
		
		//计算获取新闻的时间
		if( month < 10)
			downloadTime = year+"0"+month;
		else 
			downloadTime = year+""+month;
		if(date < 10)
			downloadTime += "0" + date;
		else 
			downloadTime += date ;
		CRUT crut = new CRUT(DBName ,DBTable);
		//社会新闻 首页链接
		theme = "http://news.sohu.com/shehuixinwen.shtml";
		
		//新闻主题links的正则表达式（待定）
		newsThemeLinksReg = "";
		
		//新闻内容links的正则表达式 
		newsContentLinksReg = "http://news.sohu.com/"+downloadTime+"/n[0-9]{9}.shtml";
		//保存社会新闻主题links
		Queue<String> sheHuiNewsTheme = new LinkedList<String>();
		sheHuiNewsTheme = findThemeLinks(theme,newsThemeLinksReg);
		
		//获取社会新闻内容links
		Queue<String>sheHuiNewsContent = new LinkedList<String>();
		sheHuiNewsContent = findContentLinks(sheHuiNewsTheme,newsContentLinksReg);
		if(sheHuiNewsContent==null){
			crut.destory();
			return ;
		}
		//获取每个新闻网页的html

		while(!sheHuiNewsContent.isEmpty()){
			String url = sheHuiNewsContent.poll();
			if(!crut.query("Url", url)){
				Date date = new Date();
				String html = findContentHtml(url);  //获取新闻的html
				if(html!=null)
					crut.add(findNewsTitle(html,newsTitleLabel,"-搜狐新闻"), findNewsOriginalTitle(html,newsTitleLabel,"-搜狐新闻"),findNewsOriginalTitle(html,newsTitleLabel,"-搜狐新闻"), findNewsTime(html,newsTimeLabel),findNewsContent(html,newsContentLabel), findNewsSource(html,newsSourceLabel),
							findNewsOriginalSource(html,newsSourceLabel), findNewsCategroy(html,newsCategroyLabel), findNewsOriginalCategroy(html,newsCategroyLabel), url, findNewsImages(html,newsTimeLabel),downloadTime,date);
			}
		}
//		System.out.println(i);
		crut.destory();
		System.out.println("shehui over ...");
	
	
	}
	
	@Override
	public Queue<String> findThemeLinks(String themeLink ,String themeLinkReg) {
		
		Queue<String> themelinks = new LinkedList<String>();
		String html = findContentHtml(themeLink);
		if(html!=null){
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
		}
		return themelinks ;
	}

	public Queue<String> findContentLinks(Queue<String> themeLink ,String contentLinkReg) {
		// TODO Auto-generated method stub
		Queue<String> contentlinks = new LinkedList<String>(); // 临时征用
		Exception bufException = null ;
		Pattern newsContent = Pattern.compile(contentLinkReg);
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
				bufException = e ;
			}catch(Exception e){
				bufException = e ;
			}finally{
				if(bufException != null )
					return null;
			}		
		}
//		System.out.println(contentlinks);
		return contentlinks;
	}
	
	@Override
	public String findContentHtml(String url) {
		// TODO Auto-generated method stub
		String html = null;                 //网页html
		Exception bufException = null ;
		HttpURLConnection httpUrlConnection = null ;
	    InputStream inputStream;
	    BufferedReader bufferedReader;
	    
		int state = 0;
		//判断url是否为有效连接
		try{
			httpUrlConnection = (HttpURLConnection) new URL(url).openConnection(); //创建连接
			state = httpUrlConnection.getResponseCode();
			httpUrlConnection.disconnect();
		}catch (MalformedURLException e) {
//          e.printStackTrace();
			System.out.println("该连接"+url+"网络有故障，已经无法正常链接，无法获取新闻");
			bufException = null ;
		} catch (IOException e) {
          // TODO Auto-generated catch block
//          e.printStackTrace();
			System.out.println("该连接"+url+"网络超级慢，已经无法正常链接，无法获取新闻");
			bufException = null ;
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
        	httpUrlConnection.setConnectTimeout(3000);
			httpUrlConnection.setReadTimeout(1000);
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
	
	@Override
	public String HandleHtml(String html, String one, String two) {
		if(html == null)
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
		if(titleBuf!= null && titleBuf.contains(buf))
			titleBuf = titleBuf.substring(0, titleBuf.indexOf(buf))	;
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
		if(titleBuf != null && titleBuf.contains(buf))
			titleBuf = titleBuf.substring(0, titleBuf.indexOf(buf)+buf.length())	;
		return titleBuf;
	}
	@Override
	public String findNewsContent(String html , String[] label) {
		// TODO Auto-generated method stub
		String contentBuf;
		if(label[1].equals("")){
			contentBuf = HandleHtml(html,label[0]);
		}else{
			contentBuf = HandleHtml(html,label[0],label[1]);
		}
		if(contentBuf!= null ){
			if(contentBuf.contains("_S_b.jpg\";")){
				contentBuf = contentBuf.substring(contentBuf.indexOf("_S_b.jpg\";")+10, contentBuf.length());
			}
			if(contentBuf!=null&&contentBuf.contains("// <![CDATA["))
				contentBuf = contentBuf.substring(0,contentBuf.indexOf("// <![CDATA["));
			if(contentBuf!=null&&contentBuf.contains("media_span_url"))
				contentBuf = contentBuf.substring(0, contentBuf.indexOf("media_span_url"));
			if(contentBuf!=null&&contentBuf.contains("http://"))
				contentBuf = contentBuf.substring(0,contentBuf.indexOf("http://"));
			if(contentBuf!=null){
				contentBuf = contentBuf.replaceAll("&#160;", "");
				contentBuf = contentBuf.replaceFirst("\\s+", "");
				contentBuf = contentBuf.replaceAll("　　", "");
			}
		}
		return contentBuf;
	}
	@Override
	public String findNewsImages(String html , String[] label) {
		String bufHtml = "";        //辅助
		String imageNameTime  = "";
//		Queue<String> imageUrl = new LinkedList<String>();  //保存获取的图片链接
		if(html == null)
			return null;
		if(html.contains("<!-- 正文 -->")&&html.contains("<!-- 分享 -->"))
			bufHtml = html.substring(html.indexOf("<!-- 正文 -->"), html.indexOf("<!-- 分享 -->"));
		else 
			return null;
		//获取图片时间，为命名服务
		imageNameTime = findNewsTime(html,label) ;
		if(imageNameTime == null )
			return null;
		//处理存放条图片的文件夹
	    File f = new File("SOHUSheHui");
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
		String imageReg = "http://photocdn.sohu.com/[0-9]{4}[0-9]{2}[0-9]{2}/Img[0-9]{9}.jpg";
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
					fileBuf = new File("SOHUSheHui",imageNameTime+photohour+photominute+photosecond+"000"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf); 
					imageLocation.offer(fileBuf.getPath());
				}else if(imageNumber < 100){
					fileBuf = new File("SOHUSheHui",imageNameTime+photohour+photominute+photosecond+"00"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
	            
				}else if(imageNumber < 1000){
					fileBuf = new File("SOHUGuoNei",imageNameTime+photohour+photominute+photosecond+"0"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
	  
				}else{
					fileBuf = new File("SOHUGuoNei",imageNameTime+photohour+photominute+photosecond+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
				}
	           
				byte[] buf = new byte[1024];  
				int length = 0;  
//	          	 System.out.println("开始下载:" + url);  
				while ((length = in.read(buf, 0, buf.length)) != -1) {  
					fo.write(buf, 0, length);  
				}  
				in.close();  
				fo.close();  
//	            System.out.println(imageName + "下载完成"); 
			}catch(Exception e){
				System.out.println("亲，图片下载失败！！");
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
	@Override
	public String findNewsTime(String html , String[] label) {
		// TODO Auto-generated method stub
		String timeBuf ="";
		if(label[1].equals("")){
			timeBuf = HandleHtml(html , label[0]);
		}else{
			timeBuf = HandleHtml(html , label[0],label[1]);
		}
		if(timeBuf != null){
			timeBuf = timeBuf.replaceAll("[^0-9]", "");
			if(timeBuf.length() >= 8)
				timeBuf = timeBuf.substring(0, 8);
		}
		return timeBuf;
	}
	@Override
	public String findNewsSource(String html ,String[] label) {
		// TODO Auto-generated method stub
		if(label.length == 3 && (!label[2].equals("")))
			return label[2];
		else
			return null;
	}
	@Override
	public String findNewsOriginalSource(String html ,String[] label) {
		// TODO Auto-generated method stub
		String sourceBuf;
		if(label[1].equals("")){
			sourceBuf = HandleHtml(html , label[0]);
		}else{
			sourceBuf = HandleHtml(html , label[0],label[1]);
		}
		if(sourceBuf!=null){
			sourceBuf = sourceBuf.replaceAll("\\s+", "");
			sourceBuf = label[2] +" - "+ sourceBuf;
		}
		return sourceBuf;
	}
	@Override
	public String findNewsCategroy(String html , String[] label) {
		// TODO Auto-generated method stub
		String categroyBuf ="";
		if(label[1].equals("")){
			categroyBuf = HandleHtml(html , label[0]);
		}else{
			categroyBuf = HandleHtml(html , label[0],label[1]);
		}
		if(categroyBuf!=null && categroyBuf.contains("&gt;")){
			categroyBuf = categroyBuf.replaceAll("&gt;", "");
		}
		return categroyBuf;
	}
	@Override
	public String findNewsOriginalCategroy(String html , String[] label) {
		// TODO Auto-generated method stub
		String categroyBuf ="";
		if(label[1].equals("")){
			categroyBuf = HandleHtml(html , label[0]);
		}else{
			categroyBuf = HandleHtml(html , label[0],label[1]);
		}
		if(categroyBuf!=null && categroyBuf.contains("&gt;")){
			categroyBuf = categroyBuf.replaceAll("&gt;", "");
		}
		return categroyBuf;
	}
	
	public static void main(String []args){
		SOHUSheHui test = new SOHUSheHui();
		test.getSOHUSheHuiNews();
	}
}
