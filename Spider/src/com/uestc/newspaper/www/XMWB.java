package com.uestc.newspaper.www;

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

public class XMWB implements NEWSPAPER{

	private String DBName ;
	private String DBTable ;
	private String ENCODE ;
	//新闻主题 links
	private String themeLinksReg;
	//内容连接 links
	private String contentLinksReg;
	//新闻入口
	private String themeUrl ;
	
	//downloadTime
	private String downloadTime;
	Calendar today = Calendar.getInstance();
	private int year = today.get(Calendar.YEAR);
	private int month = today.get(Calendar.MONTH)+1;
	private int date = today.get(Calendar.DATE);	
	//图片个数
	private int imageNumber = 1;
	
	public void getXMWB(){
		System.out.println("xmwb start...");
		DBName = "NEWSPAPER";
		DBTable ="XMWB";
		ENCODE = "utf-8";
		
		String[] titleLabel = new String[]{"title",""};     
		String[] contentLabel = new String[]{"id","ozoom"};  
		String[] timeLabel = new String[]{"id","jbdata"};   
		String[] sourceLabel =new String[]{"新民晚报","新民晚报：http://xmwb.xinmin.cn/"}; 
		String[] categroyLabel = new String[]{"style","padding-left:10px;"} ; 
		
		CRUT crut = new CRUT(DBName ,DBTable);
		
		String bufMonthString;
		String bufDateString;
		if(month < 10){
			bufMonthString = "0"+month;
		}else {
			bufMonthString = ""+month ;
		}
		if(date < 10){
			bufDateString = "0"+date;
			
		}else {
			bufDateString = "" + date;
		}
		
		//新闻入口赋值
		themeUrl = "http://xmwb.xinmin.cn/html/"+year+"-"+bufMonthString+"/"+bufDateString+"/node_1.htm";

		//主题连接 赋值
		themeLinksReg = "http://xmwb.xinmin.cn/html/"+year+"-"+bufMonthString+"/"+bufDateString+"/node_[0-9]{1,2}.htm";
		
		//内容连接 赋值
		contentLinksReg = "http://xmwb.xinmin.cn/html/"+year+"-"+bufMonthString+"/"+bufDateString+"/content_[0-9]{1}_[0-9]{1,2}.htm";
		
		IOException bufException = null ;
		int state = 0 ;
		try{
			HttpURLConnection httpUrlConnection = (HttpURLConnection) new URL(themeUrl).openConnection(); //创建连接
			state = httpUrlConnection.getResponseCode();
			httpUrlConnection.disconnect();
		}catch (MalformedURLException e) {
//          e.printStackTrace();
			System.out.println("网络慢，已经无法正常链接，无法获取新闻");
			bufException = e ;
		} catch (IOException e) {
          // TODO Auto-generated catch block
//          e.printStackTrace();
			System.out.println("网络超级慢，已经无法正常链接，无法获取新闻");
			bufException = e ;
		}finally{
			if(bufException!= null)
				return ;
		}
		if(state != 200 && state != 201){
			return;
		}
		
		//保存成都商报新闻主题
		Queue<String> cdsbThemeQueue = new LinkedList<String>();
		cdsbThemeQueue = getThemeLinks(themeUrl, themeLinksReg);
//		System.out.println(cdsbThemeQueue);
		//保存成都商报新闻url
		Queue<String> cdsbContentQueue = new LinkedList<String>();
		cdsbContentQueue = getContentLinks(cdsbThemeQueue, contentLinksReg);
//		System.out.println(cdsbContentQueue);
		//下载时间
		downloadTime = ""+year+bufMonthString+bufDateString;
		
		if(cdsbContentQueue == null ){
			crut.destory();
			return ;
		}
		while(!cdsbContentQueue.isEmpty()){
			String url = cdsbContentQueue.poll();
			if(!crut.query("Url", url)){
				Date date = new Date();
				String html = getContentHtml(url);
				if(html!=null)
					crut.add(getNewsTitle(html,titleLabel,"新民晚报数字报-"), getNewsOriginalTitle(html,titleLabel,"新民晚报数字报-"),getNewsOriginalTitle(html,titleLabel,"新民晚报数字报-"), getNewsTime(html,timeLabel),getNewsContent(html,contentLabel), getNewsSource(html,sourceLabel),
							getNewsOriginalSource(html,sourceLabel), getNewsCategroy(html,categroyLabel), getNewsOriginalCategroy(html,categroyLabel), url, getNewsImages(html,timeLabel),downloadTime,date);
			}
		}
		crut.destory();
		System.out.println("xmwb over...");
	}
	@Override
	public Queue<String> getThemeLinks(String themeLink, String themeLinkReg) {
		
		Queue<String> themelinks = new LinkedList<String>();
		Pattern newsThemeLink = Pattern.compile(themeLinkReg);
		themelinks.offer(themeLink);
		Exception bufException = null;
		try {
				Parser parser = new Parser(themeLink);
				parser.setEncoding(ENCODE);
				@SuppressWarnings("serial")
				NodeList nodeList = parser.extractAllNodesThatMatch(new NodeFilter(){
					public boolean accept(Node node)
					{
						if (node instanceof LinkTag)// 标记
							return true;
						return false;
					}});
				
				for (int i = 0; i < nodeList.size(); i++)
				{
				
					LinkTag n = (LinkTag) nodeList.elementAt(i);
//		        	System.out.print(n.getStringText() + "==>> ");
//		       	 	System.out.println(n.extractLink());
					//新闻主题
					Matcher themeMatcher = newsThemeLink.matcher(n.extractLink());
					if(themeMatcher.find()){
						if(!themelinks.contains(n.extractLink()))
							themelinks.offer(n.extractLink());
		        	}
				}
			}catch(ParserException e){
				bufException = e ;
			}catch(Exception e){
				bufException = e ;
			}finally{
				if(bufException != null){
					return null;
				}
				
			}
		return themelinks ;
	}

	@Override
	public Queue<String> getContentLinks(Queue<String> themeLink , String ContentLinkReg) {
		Queue<String> contentlinks = new LinkedList<String>(); // 临时征用
		Exception bufException = null ;
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
				bufException = e ;
			}catch(Exception e){
				bufException = e ;
			}finally{
				if(bufException != null ){
					return null;
				}
			}		
		}
//		System.out.println(contentlinks);
		return contentlinks;
	}

	@Override
	public String getContentHtml(String url) {
		String html = null;                 //网页html
		HttpURLConnection httpUrlConnection = null;
	    InputStream inputStream;
	    BufferedReader bufferedReader;
	    IOException bufException = null ;
		int state = 0 ;
		//判断url是否为有效连接
		try{
			httpUrlConnection = (HttpURLConnection) new URL(url).openConnection(); //创建连接
			state = httpUrlConnection.getResponseCode();
			httpUrlConnection.disconnect();
		}catch (MalformedURLException e) {
//          e.printStackTrace();
			System.out.println("该连接"+url+"网络有故障，已经无法正常链接，无法获取新闻");
			bufException = e; 
		} catch (IOException e) {
          // TODO Auto-generated catch block
//          e.printStackTrace();
			System.out.println("该连接"+url+"网络超级慢，已经无法正常链接，无法获取新闻");
			bufException = e;
		}finally{
			if(bufException != null){
				return null;
			}
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
        		return null ;
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
	public String getHtml(String html, String one) {
		if(html == null)
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
	public String getHtml(String html, String one, String two) {
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

	@Override
	public String getNewsTitle(String html, String[] label, String buf) {
		String titleBuf ;
		if(label[1].equals("")){
			titleBuf = getHtml(html,label[0]);
		}else{
			titleBuf = getHtml(html,label[0],label[1]);
		}
		if(titleBuf!=null&&titleBuf.contains(buf))
			titleBuf = titleBuf.replaceAll(buf, "");
		return titleBuf;
	}

	@Override
	public String getNewsOriginalTitle(String html, String[] label, String buf) {
		String titleBuf ;
		if(label[1].equals("")){
			titleBuf = getHtml(html,label[0]);
		}else{
			titleBuf = getHtml(html,label[0],label[1]);
		}
//		if(titleBuf!=null&&titleBuf.contains(buf))
//			titleBuf = titleBuf + buf	;
		return titleBuf;
	}

	@Override
	public String getNewsContent(String html, String[] label) {
		String contentBuf;
		if(label[1].equals("")){
			contentBuf = getHtml(html,label[0]);
		}else{
			contentBuf = getHtml(html,label[0],label[1]);
		}
		if(contentBuf!=null){
			contentBuf = contentBuf.replaceFirst("\\s+", "");
			if(contentBuf.contains("\n                                                            \n                                                            \n                                                        "))
				contentBuf = contentBuf.replace("\n                                                            \n                                                            \n                                                        ", "");
		}
		return contentBuf;
	}

	@Override
	public String getNewsImages(String html, String[] label) {
		if(html == null)
			return null;
		String bufHtml = null  ;        //辅助
		if(html.contains("<!--<npm:article-pic>-->")&&html.contains("<!--</npm:article-pic>-->")){
			bufHtml = html.substring(html.indexOf("<!--<npm:article-pic>-->"), html.lastIndexOf("<!--</npm:article-pic>-->"));
		}
		String imageNameTime  = "";

		//获取图片时间，为命名服务
		imageNameTime = getNewsTime(html,label) ;
//		System.out.println(imageNameTime);
		if(imageNameTime == null || imageNameTime.equals("")||imageNameTime.length() < 8)
			return null;
		//处理存放条图片的文件夹
    	File f = new File("XMWB");
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
		String imageReg = "../../../resfile/"+imageNameTime.substring(0,4)+"-"+imageNameTime.substring(4, 6)+"-"+imageNameTime.substring(6, 8)+"/(.*?).jpg";
//		System.out.println(imageReg);
		Pattern newsImage = Pattern.compile(imageReg);
		Matcher imageMatcher = newsImage.matcher(bufHtml);
		//处理图片
		int i = 1 ;      //本条新闻图片的个数
		while(imageMatcher.find()){
			String bufUrl = imageMatcher.group();
			
			bufUrl =bufUrl.replace("../../../", "");
//			System.out.println(bufUrl);
			bufUrl =  "http://xmwb.xinmin.cn/" + bufUrl;
//			System.out.println(bufUrl);
			File fileBuf;
//			imageMatcher.group();
			String imageNameSuffix = bufUrl.substring(bufUrl.lastIndexOf("."), bufUrl.length());  //图片后缀名
			try{
				URL uri = new URL(bufUrl);  
			
				InputStream in = uri.openStream();
				FileOutputStream fo;
				if(imageNumber < 10){
					fileBuf = new File("XMWB",imageNameTime+photohour+photominute+photosecond+"000"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf); 
					imageLocation.offer(fileBuf.getPath());
				}else if(imageNumber < 100){
					fileBuf = new File("XMWB",imageNameTime+photohour+photominute+photosecond+"00"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
            
				}else if(imageNumber < 1000){
					fileBuf = new File("XMWB",imageNameTime+photohour+photominute+photosecond+"0"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
  
				}else{
					fileBuf = new File("XMWB",imageNameTime+photohour+photominute+photosecond+imageNumber+"000"+i+imageNameSuffix);
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

	@Override
	public String getNewsTime(String html, String[] label) {
		String bufMonthString;
		String bufDateString;
		if(month < 10){
			bufMonthString = "0"+month;
		}else {
			bufMonthString = ""+month ;
		}
		if(date < 10){
			bufDateString = "0"+date;
			
		}else {
			bufDateString = "" + date;
		}
		return year+bufMonthString+bufDateString;
	}

	@Override
	public String getNewsSource(String html, String[] label) {
		// TODO Auto-generated method stub
		if(label.length == 2 && (!label[0].equals("")))
			return label[0];
		else
			return null;
	}

	@Override
	public String getNewsOriginalSource(String html, String[] label) {
		
		if(label.length == 2 && (!label[1].equals("")))
			return label[1];
		else
			return null;
	}

	@Override
	public String getNewsCategroy(String html, String[] label) {
		String categroyBuf ="";
		if(label[1].equals("")){
			categroyBuf = getHtml(html , label[0]);
		}else{
			categroyBuf = getHtml(html , label[0],label[1]);
		}
		if(categroyBuf!=null){
			if(categroyBuf.contains("："))
			 categroyBuf = categroyBuf.substring(categroyBuf.indexOf("：")+1, categroyBuf.length());
			 categroyBuf = categroyBuf.replaceAll("\\s+", "");
		}
		return categroyBuf;
	}

	@Override
	public String getNewsOriginalCategroy(String html, String[] label) {
		String categroyBuf ="";
		if(label[1].equals("")){
			categroyBuf = getHtml(html , label[0]);
		}else{
			categroyBuf = getHtml(html , label[0],label[1]);
		}
//		if(categroyBuf!=null){
//			if(categroyBuf.contains("第")&&categroyBuf.contains("»")){
//				categroyBuf = categroyBuf.substring(categroyBuf.indexOf("第"), categroyBuf.indexOf("»")-1);
//			}
//		}
		return categroyBuf;
	}
	public static void main(String[] args){
		XMWB test=new XMWB();
		test.getXMWB();
	}
}
