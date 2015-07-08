package com.uestc.NETEASE.www;

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

public class NETEASEWar implements NETEASE{

	
	private String DBName ;   //sql name
	private String DBTable ;  // collections name
	private String ENCODE ;   //html encode gb2312
	
	//��������links���������ʽ
	private String newsThemeLinksReg ; //= "http://news.163.com/special/0001124J/guoneinews_[0-9]{1,2}.html#headList";
			
	//��������links���������ʽ
	private String newsContentLinksReg ; //= "http://news.163.com/[0-9]{2}/[0-9]{4}/[0-9]{2}/(.*?).html#f=dlist";
		
	//��������link
	private String theme ;
	//downloadTime
	private String downloadTime;
	Calendar today = Calendar.getInstance();
	private int year = today.get(Calendar.YEAR);
	private int month = today.get(Calendar.MONTH)+1;
	private int date = today.get(Calendar.DATE);	
	//ͼƬ����
	private int imageNumber = 1 ;
	public NETEASEWar(){
	}
	
	public void getNETEASEWarNews(){
		System.out.println("�������ž��¿�ʼ����...");
		/*��ʼ��������ǩ�� ���� gb2312
		 * 
		 * */
		ENCODE = "GB2312";
		DBName = "NETEASE";   //���ݿ�����
		DBTable = "war";   //����
		String[] newsTitleLabel = new String[]{"title",""};     //���ű����ǩ title or id=h1title
		String[] newsContentLabel = new String[]{"id" ,"endText"};  //�������ݱ�ǩ "id","endText"
		String[] newsTimeLabel = new String[]{"class","ep-time-soure cDGray"};   //����ʱ��"class","ep-time-soure cDGray"  
		String[] newsSourceLabel =new String[]{"class","ep-time-soure cDGray","��������-��������"}; //��3��������������Դ ͬ����ʱ��"class","ep-time-soure cDGray" �ټ���һ��"��������-��������"
		String[] newsCategroyLabel = new String[]{"class","ep-crumb JS_NTES_LOG_FE"} ; // "����" "��������-��������-http://news.163.com/domestic/"
		CRUT crut = new CRUT(DBName,DBTable);
		//�����ȡ���ŵ�ʱ��
		if( month < 10)
			downloadTime = year+"0"+month;
		else 
			downloadTime = year+""+month;
		if(date < 10)
			downloadTime += "0" + date;
		else 
			downloadTime += date ;
		/*���ģ���������ץȡ
		 * 1����ҳ"http://war.163.com/index.html"
		 * 2.��ϸ����һ��10Ҷ��http://war.163.com/special/millatestnews/ http://war.163.com/special/millatestnews_06/
		 * 3.������ʷ��http://war.163.com/special/historyread/
		 * */
		//����
		Queue<String> warNewsThemeLinks = new LinkedList<String>();
		//����
		Queue<String> warNewsContentLinks = new LinkedList<String>();
		
		//������ҳ
		theme = "http://war.163.com/index.html";
		//�������ݵ��������ʽhttp://war.163.com/14/1124/09/ABQAT7EM00011MTO.html
		newsContentLinksReg = "http://war.163.com/[0-9]{2}/[0-9]{4}/[0-9]{2}/(.*?).html";
		warNewsThemeLinks.offer(theme);
		warNewsContentLinks = findContentLinks(warNewsThemeLinks,newsContentLinksReg);
		if(warNewsContentLinks != null){
			while(!warNewsContentLinks.isEmpty()){
				String url = warNewsContentLinks.poll();
				if(!crut.query("Url", url)){
					Date date = new Date();
					String html = findContentHtml(url);  //��ȡ���ŵ�html
//				System.out.println(url);
//				System.out.println("download:"+downloadTime);
//				System.out.println(findNewsTime(html,newsTimeLabel));
					if(html!=null)
						crut.add(findNewsTitle(html,newsTitleLabel,"_������������"), findNewsOriginalTitle(html,newsTitleLabel,"_������������"),findNewsOriginalTitle(html,newsTitleLabel,"_������������"), findNewsTime(html,newsTimeLabel),findNewsContent(html,newsContentLabel), findNewsSource(html,newsSourceLabel),
							findNewsOriginalSource(html,newsSourceLabel), findNewsCategroy(html,newsCategroyLabel), findNewsOriginalCategroy(html,newsCategroyLabel), url, findNewsImages(html,newsTimeLabel),downloadTime,date);
				
				}
			}
		}
		
		//��ϸ����ģ��
		theme  = "http://war.163.com/special/millatestnews/";
//		newsThemeLinksReg = "http://war.163.com/special/millatestnews(_[0-9]{2})*/";  //�����������ʽ
		newsContentLinksReg = "http://war.163.com/[0-9]{2}/[0-9]{4}/[0-9]{2}/(.*?).html"; //�����������ʽ
//		warNewsThemeLinks = findThemeLinks(theme,newsThemeLinksReg);
		warNewsThemeLinks.offer(theme);
		warNewsContentLinks = findContentLinks(warNewsThemeLinks,newsContentLinksReg);
//		int k = 1;
		if(warNewsContentLinks != null){
			while(!warNewsContentLinks.isEmpty()){
				String url = warNewsContentLinks.poll();
				if(!crut.query("Url", url)){
					Date date = new Date();
					String html = findContentHtml(url);  //��ȡ���ŵ�html
//					System.out.println(url);
//					System.out.println("download:"+downloadTime);
//					System.out.println(findNewsTime(html,newsTimeLabel));
//					System.out.println(findNewsComment(url,html,newsCategroyLabel));
				
					crut.add(findNewsTitle(html,newsTitleLabel,"_���׾���"), findNewsOriginalTitle(html,newsTitleLabel,"_���׾���"),findNewsOriginalTitle(html,newsTitleLabel,"_���׾���"), findNewsTime(html,newsTimeLabel),findNewsContent(html,newsContentLabel), findNewsSource(html,newsSourceLabel),
							findNewsOriginalSource(html,newsSourceLabel), findNewsCategroy(html,newsCategroyLabel), findNewsOriginalCategroy(html,newsCategroyLabel), url, findNewsImages(html,newsTimeLabel),downloadTime,date);
				
				}
			}
		}
//		System.out.println(k);
		
//		//������ʷ
//		theme = "http://war.163.com/special/historyread/";
////		newsThemeLinksReg = "http://war.163.com/special/historyread(_[0-9]{2})*/";  //�����������ʽ
//		newsContentLinksReg = "http://war.163.com/[0-9]{2}/[0-9]{4}/[0-9]{2}/(.*?).html"; //�����������ʽ
//		warNewsThemeLinks.offer(theme);
//		warNewsContentLinks = findContentLinks(warNewsThemeLinks,newsContentLinksReg);
////		int j = 1 ;
//		if(warNewsContentLinks == null)
//			return ;
//		while(!warNewsContentLinks.isEmpty()){
//			String url = warNewsContentLinks.poll();
//			
//			if(!crut.query("Url", url)){
//				Date date = new Date();
//				String html = findContentHtml(url);  //��ȡ���ŵ�html
//				System.out.println(url);
//				System.out.println("download:"+downloadTime);
//				System.out.println(findNewsTime(html,newsTimeLabel));
//				if(findNewsTime(html,newsTimeLabel)!= null && findNewsTime(html,newsTimeLabel) == downloadTime){
//					crut.add(findNewsTitle(html,newsTitleLabel,"_���׾���"), findNewsOriginalTitle(html,newsTitleLabel,"_���׾���"),findNewsOriginalTitle(html,newsTitleLabel,"_���׾���"), findNewsTime(html,newsTimeLabel),findNewsContent(html,newsContentLabel), findNewsSource(html,newsSourceLabel),
//							findNewsOriginalSource(html,newsSourceLabel), findNewsCategroy(html,newsCategroyLabel), findNewsOriginalCategroy(html,newsCategroyLabel), url, findNewsImages(html,newsTimeLabel),downloadTime,date);
//				}
//			}
//		}
		crut.destory();
		imageNumber = 1 ;
		System.out.println("�������ž��½���...");
	}
	@Override
	public Queue<String> findThemeLinks(String themeLink ,String themeLinkReg) {
		
		// TODO Auto-generated method stub
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
						if (node instanceof LinkTag)// ���
							return true;
						return false;
					}});
				
				for (int i = 0; i < nodeList.size(); i++)
				{
				
					LinkTag n = (LinkTag) nodeList.elementAt(i);
//		        	System.out.print(n.getStringText() + "==>> ");
//		       	 	System.out.println(n.extractLink());
					//��������
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
				if(bufException!=null){
					return null;
				}
			}
		return themelinks ;
	}

	public Queue<String> findContentLinks(Queue<String> themeLink ,String contentLinkReg) {
		// TODO Auto-generated method stub
		Queue<String> contentlinks = new LinkedList<String>(); // ��ʱ����
		Exception bufeException = null;
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
						if (node instanceof LinkTag)// ���
							return true;
						return false;
					}
		
				});
			
				for (int i = 0; i < nodeList.size(); i++)
				{
			
					LinkTag n = (LinkTag) nodeList.elementAt(i);
//	        	System.out.print(n.getStringText() + "==>> ");
//	       	 	System.out.println(n.extractLink());
					//��������
					Matcher themeMatcher = newsContent.matcher(n.extractLink());
					if(themeMatcher.find()){
					
						if(!contentlinks.contains(n.extractLink()))
							contentlinks.offer(n.extractLink());
					}
				}
			}catch(ParserException e){
				bufeException = e ;
			}catch(Exception e){
				bufeException = e ;
			}finally{
				if(bufeException!= null){
					return null;
				}
			}		
		}
//		System.out.println(contentlinks);
		return contentlinks;
	}
	
	@Override
	public String findContentHtml(String url) {
		// TODO Auto-generated method stub
		String html = null;                 //��ҳhtml
		
		HttpURLConnection httpUrlConnection = null;
	    InputStream inputStream;
	    BufferedReader bufferedReader;
	    Exception bufeException = null;
		int state = 0;
		//�ж�url�Ƿ�Ϊ��Ч����
		try{
			httpUrlConnection = (HttpURLConnection) new URL(url).openConnection(); //��������
			state = httpUrlConnection.getResponseCode();
			httpUrlConnection.disconnect();
		}catch (MalformedURLException e) {
//          e.printStackTrace();
			System.out.println("������"+url+"�����й��ϣ��Ѿ��޷��������ӣ��޷���ȡ����");
			bufeException = e ;
		} catch (IOException e) {
          // TODO Auto-generated catch block
//          e.printStackTrace();
			System.out.println("������"+url+"���糬�������Ѿ��޷��������ӣ��޷���ȡ����");
			bufeException = e ;
      }finally{
    	  if(bufeException!=null)
    		  return null;
      }
		if(state != 200 && state != 201){
			return null;
		}
		Exception bufeException1 = null;
        try {
        	httpUrlConnection = (HttpURLConnection) new URL(url).openConnection(); //��������
        	httpUrlConnection.setRequestMethod("GET");
        	httpUrlConnection.setConnectTimeout(3000);
			httpUrlConnection.setReadTimeout(1000);
            httpUrlConnection.setUseCaches(true); //ʹ�û���
            httpUrlConnection.connect();           //��������  ���ӳ�ʱ����
        } catch (IOException e) {
        	System.out.println("�����ӷ��ʳ�ʱ...");
        	bufeException1 = e ;
        }finally{
        	if(bufeException1 != null)
        		return null;
        }
  
        try {
            inputStream = httpUrlConnection.getInputStream(); //��ȡ������
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
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
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
		if(titleBuf!=null){
			if(titleBuf.contains(buf)){
				titleBuf = titleBuf.substring(0, titleBuf.indexOf(buf))	;
			}else if (titleBuf.contains("_���׾���")){
				titleBuf = titleBuf.substring(0, titleBuf.indexOf("_���׾���"))	;
			}else if(titleBuf.contains("_������")){
				titleBuf = titleBuf.substring(0, titleBuf.indexOf("_������"))	;
			}else;
		}
		return titleBuf;
	}
	//news δ��������
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
			}else if (titleBuf.contains("_���׾���")){
				titleBuf = titleBuf.substring(0, titleBuf.indexOf("_���׾���")+5)	;
			}else if(titleBuf.contains("_������")){
				titleBuf = titleBuf.substring(0, titleBuf.indexOf("_������")+4)	;
			}else;
		}
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
		if(contentBuf==null||contentBuf.equals("")){
			contentBuf = HandleHtml(html ,"class","feed-text");
//			System.out.println(contentBuf);
		}
		if(contentBuf!=null){
			if(contentBuf.contains("(NTES);")){
				contentBuf = contentBuf.substring(contentBuf.indexOf("(NTES);")+7, contentBuf.length());
			}
			contentBuf = contentBuf.replaceAll("\\s+", "\n");
			contentBuf = contentBuf.replaceFirst("\\s+", "");
		}
		return contentBuf;
	}
	@SuppressWarnings("unused")
	@Override
	public String findNewsImages(String html , String[] label) {
		// TODO Auto-generated method stub
		if(html==null)
			return null;
		String bufHtml = "";        //����
		String imageNameTime  = findNewsTime(html,label);
		if(imageNameTime==null)
			return null;
//		Queue<String> imageUrl = new LinkedList<String>();  //�����ȡ��ͼƬ����
		if(html.contains("<div id=\"endText\"")&&html.contains("<!-- ��ҳ -->"))
			bufHtml = html.substring(html.indexOf("<div id=\"endText\""), html.indexOf("<!-- ��ҳ -->"));
		else if(html.contains("�����Ƽ�")&&html.contains("<div id=\"endText\"")){ 
			
			bufHtml = html.substring(html.indexOf("<div id=\"endText\""), html.indexOf("�����Ƽ�"));
			
			
		}else{
//			System.out.println("����Ϊ�գ���");
			return null;
		}
		//��ȡͼƬʱ�䣬Ϊ��������
		if(imageNameTime.length() >= 8){
				imageNameTime = imageNameTime ;
		
		}else{
			Calendar now = Calendar.getInstance();
			int year = now.get(Calendar.YEAR);
			int month = now.get(Calendar.MONTH)+1;
			int date = now.get(Calendar.DATE);
			imageNameTime += "" + year;
			if(month < 10){
				imageNameTime += "0"+month;
			}else
				imageNameTime += month ;
			if(date < 10){
				imageNameTime += "0"+date;
			}else
				imageNameTime += date;
		}
		//���������ͼƬ���ļ���
    	File f = new File("NETEASEWar");
    	if(!f.exists()){
    		f.mkdir();
    	}
    	//�������ʱ�� ʱ���� ��ֹͼƬ�����ظ�
    	Calendar photoTime = Calendar.getInstance();
    	int photohour = photoTime.get(Calendar.HOUR_OF_DAY); 
    	int photominute = photoTime.get(Calendar.MINUTE);
    	int photosecond = photoTime.get(Calendar.SECOND); 
    	//����ͼƬ�ļ���λ����Ϣ
    	Queue<String> imageLocation = new LinkedList<String>();
    	//ͼƬ�������ʽ
		String imageReg = "(http://img[0-9]{1}.cache.netease.com/cnews/[0-9]{4}/[0-9]{1,2}/[0-9]{1,2}/(.*?).((jpg)|(png)|(jpeg)))|(http://img[0-9]{1}.cache.netease.com/catchpic/(.*?)/(.*?)/(.*?).((jpg)|(png)|(jpeg)))";
		Pattern newsImage = Pattern.compile(imageReg);
		Matcher imageMatcher = newsImage.matcher(bufHtml);
		//����ͼƬ
		int i = 1 ;      //��������ͼƬ�ĸ���
		while(imageMatcher.find()){
			String bufUrl = imageMatcher.group();
//			System.out.println(bufUrl);
			File fileBuf;
//			imageMatcher.group();
			String imageNameSuffix = bufUrl.substring(bufUrl.lastIndexOf("."), bufUrl.length());  //ͼƬ��׺��
			try{
				URL uri = new URL(bufUrl);  
			
				InputStream in = uri.openStream();
				FileOutputStream fo;
				if(imageNumber < 10){
					fileBuf = new File("NETEASEWar",imageNameTime+photohour+photominute+photosecond+"000"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf); 
					imageLocation.offer(fileBuf.getPath());
				}else if(imageNumber < 100){
					fileBuf = new File("NETEASEWar",imageNameTime+photohour+photominute+photosecond+"00"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
            
				}else if(imageNumber < 1000){
					fileBuf = new File("NETEASEWar",imageNameTime+photohour+photominute+photosecond+"0"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
  
				}else{
					fileBuf = new File("NETEASEWar",imageNameTime+photohour+photominute+photosecond+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
				}
            
				byte[] buf = new byte[1024];  
				int length = 0;  
//           	 System.out.println("��ʼ����:" + url);  
				while ((length = in.read(buf, 0, buf.length)) != -1) {  
					fo.write(buf, 0, length);  
				}  
				in.close();  
				fo.close();  
//          	  System.out.println(imageName + "�������"); 
			}catch(Exception e){
				System.out.println("�ף�ͼƬ����ʧ�ܣ���");
				System.out.println("���������Ƿ�������");
			}
			i ++;
			
        }  
		//�����������û��ͼƬ��ͼƬ�ı�Ų�������
		if(!imageLocation.isEmpty())
			imageNumber ++;
		return imageLocation.toString();
	}
	//����ʱ��
	@Override
	public String findNewsTime(String html , String[] label) {
		// TODO Auto-generated method stub
		String timeBuf = null;
		if(label[1].equals("")){
			timeBuf = HandleHtml(html , label[0]);
		}else{
			timeBuf = HandleHtml(html , label[0],label[1]);
		}
//		System.out.println(timeBuf+"lllllllllll");
		if(timeBuf == null||timeBuf.equals("")){
			timeBuf = HandleHtml(html,"class","ep-info cDGray");
//			return timeBuf;
		}else if(label[0].equals("style")&&label[1].equals("float:left;")){
			timeBuf = timeBuf.substring(0,19);
		}else
			timeBuf = timeBuf.substring(9, 28);  //���ݲ�ͬ���� ��ͬ����
		if(timeBuf!=null){
			timeBuf = timeBuf.replaceAll("[^0-9]", "");
			if(timeBuf.length() >= 8)
				timeBuf = timeBuf.substring(0, 8);
			else
				timeBuf = null;
		}
		if(timeBuf == null ){
			timeBuf = downloadTime ;
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
		if(sourceBuf==null||sourceBuf.equals("")){
			sourceBuf = HandleHtml(html,"class","ep-info cDGray");
		}
		if(sourceBuf!=null){
			if(sourceBuf.contains("��Դ: ")){
				sourceBuf = sourceBuf.substring(sourceBuf.indexOf("��Դ: ")+4, sourceBuf.length());
			}
			if(sourceBuf.length() >29)
				sourceBuf = sourceBuf.substring(29, sourceBuf.length());  //���ݲ�ͬ���� ��ͬ����
		}
		if(label.length == 3 && (!label[2].equals("")))
			return label[2]+"-"+sourceBuf;
		else
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
		if(categroyBuf!=null&&categroyBuf.contains("&gt;")){
			categroyBuf = categroyBuf.replaceAll("&gt;", "");
			categroyBuf = categroyBuf.replaceAll("\\s+", "");
			if(categroyBuf.contains("��������")){
				categroyBuf = categroyBuf.substring(categroyBuf.indexOf("��������")+4, categroyBuf.indexOf("����"));
			}else if(categroyBuf.contains("����Ƶ��")){
				categroyBuf = categroyBuf.substring(categroyBuf.indexOf("����Ƶ��")+4, categroyBuf.length());
			}else if(categroyBuf.contains("������ҳ")){
				categroyBuf = categroyBuf.substring(categroyBuf.indexOf("������ҳ")+4, categroyBuf.indexOf("����"));
			}else;
			
			
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
		if(categroyBuf!=null&&categroyBuf.contains("&gt;")){
			categroyBuf = categroyBuf.replaceAll("&gt;", "");
		}
		return categroyBuf;
	}
	public static void main(String[] args){
		long start = System.currentTimeMillis();
		NETEASEWar test = new NETEASEWar();
		test.getNETEASEWarNews();
		long end = System.currentTimeMillis();
		System.out.println(end-start);
	}
}