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
import java.util.Vector;
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
 *��ȡ�Ѻ���������
 *url:http://news.sohu.com/guoneixinwen.shtml
 *ÿ�ո����������� 
 * */
public class SOHUGuoNei implements SOHU{

	private String DBName ;   //sql name
	private String DBTable ;  // collections name
	private String ENCODE ;   //html encode gb2312	
	//��������links���������ʽ
	private String newsThemeLinksReg ; 
			
	//��������links���������ʽ
	private String newsContentLinksReg ; 
	
	//�Ѿ����ʹ���url
	Vector<String> visitedUrl = new Vector<String>();
		
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
	
	public void getSOHUGuoNeiNews(){
		System.out.println("guonei start...");
		DBName = "SOHU";
		DBTable = "GN";
		ENCODE = "gb2312";
		String[] newsTitleLabel = new String[]{"title",""};     //���ű����ǩ t
		String[] newsContentLabel = new String[]{"id" ,"contentText"};  //�������ݱ�ǩ "id","endText"
		String[] newsTimeLabel = new String[]{"class","time"};   //����ʱ��"class","ep-time-soure cDGray"  
		String[] newsSourceLabel =new String[]{"class","source","�Ѻ�����-��������"}; //��3��������������Դ ͬ����ʱ��
		String[] newsCategroyLabel = new String[]{"class","navigation"} ; // ����
		CRUT crut = new CRUT(DBName ,DBTable);
		
		//�����ȡ���ŵ�ʱ��
		if( month < 10)
			downloadTime = year+"0"+month;
		else 
			downloadTime = year+""+month;
		if(date < 10)
			downloadTime += "0" + date;
		else 
			downloadTime += date ;
		//�������� ��ҳ����
		theme = "http://news.sohu.com/guoneixinwen.shtml";
		
		//��������links���������ʽ��������
		newsThemeLinksReg = "";
		
		//��������links���������ʽ 
		newsContentLinksReg = "http://news.sohu.com/"+downloadTime+"/n[0-9]{9}.shtml";
		
		//���������������links
		Queue<String> guoNeiNewsTheme = new LinkedList<String>();
		guoNeiNewsTheme = findThemeLinks(theme,newsThemeLinksReg);
//		System.out.println(guoNeiNewsTheme);

		//��ȡ�����������links
		Queue<String>guoNeiNewsContent = new LinkedList<String>();
		guoNeiNewsContent = findContentLinks(guoNeiNewsTheme,newsContentLinksReg);
		if(guoNeiNewsContent==null){
			crut.destory();
			return ;
		}
//		System.out.println(guoNeiNewsContent);
		//��ȡÿ��������ҳ��html

		while(!guoNeiNewsContent.isEmpty()){
			String url = guoNeiNewsContent.poll();
			if(!crut.query("Url", url)){
				Date date = new Date();
				String html = findContentHtml(url);  //��ȡ���ŵ�html
				if(html!=null){
					crut.add(findNewsTitle(html,newsTitleLabel,"-�Ѻ�����"), findNewsOriginalTitle(html,newsTitleLabel,"-�Ѻ�����"),findNewsOriginalTitle(html,newsTitleLabel,"-�Ѻ�����"), findNewsTime(html,newsTimeLabel),findNewsContent(html,newsContentLabel), findNewsSource(html,newsSourceLabel),
						findNewsOriginalSource(html,newsSourceLabel), findNewsCategroy(html,newsCategroyLabel), findNewsOriginalCategroy(html,newsCategroyLabel), url, findNewsImages(html,newsTimeLabel),downloadTime,date);
				}
			}
//			visitedUrl.add(url);
		}
//		visitedUrl = null ;
//		System.out.println(i);
		crut.destory();
		System.out.println("guonei over...");
	
	
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
		
			String s1 = "http://news.sohu.com/guoneixinwen_";
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
		Queue<String> contentlinks = new LinkedList<String>(); // ��ʱ����
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
				bufException = e ;
			}catch(Exception e){
				bufException = e ;
			}finally{
				if(bufException != null && contentlinks != null )
					return contentlinks;
			}		
		}
//		System.out.println(contentlinks);
		return contentlinks;
	}
	
	@Override
	public String findContentHtml(String url) {
		// TODO Auto-generated method stub
		String html = null;                 //��ҳhtml
		Exception bufException = null ;
		HttpURLConnection httpUrlConnection = null;
	    InputStream inputStream;
	    BufferedReader bufferedReader;
	    
		int state = 0;
		//�ж�url�Ƿ�Ϊ��Ч����
		try{
			httpUrlConnection = (HttpURLConnection) new URL(url).openConnection(); //��������
			state = httpUrlConnection.getResponseCode();
			httpUrlConnection.disconnect();
		}catch (MalformedURLException e) {
//          e.printStackTrace();
			System.out.println("������"+url+"�����й��ϣ��Ѿ��޷��������ӣ��޷���ȡ����");
			bufException = e ;
		} catch (IOException e) {
          // TODO Auto-generated catch block
//          e.printStackTrace();
			System.out.println("������"+url+"���糬�������Ѿ��޷��������ӣ��޷���ȡ����");
			bufException = e ;
		}finally{
			if(bufException != null)
				return null;
		}
		if(state != 200 && state != 201){
			return null;
		}
  
        try {
        	httpUrlConnection = (HttpURLConnection) new URL(url).openConnection(); //��������
        	httpUrlConnection.setRequestMethod("GET");
        	httpUrlConnection.setConnectTimeout(3000);
			httpUrlConnection.setReadTimeout(1000);
            httpUrlConnection.setUseCaches(true); //ʹ�û���
            httpUrlConnection.connect();           //��������  ���ӳ�ʱ����
        } catch (IOException e) {
        	System.out.println(url+"�����ӷ��ʳ�ʱ...");
        	bufException = e ;
        }finally{
        	if(bufException != null)
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
		if(titleBuf!=null&&titleBuf.contains(buf))
			titleBuf = titleBuf.substring(0, titleBuf.indexOf(buf))	;
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
		if(titleBuf!=null&&titleBuf.contains(buf))
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
		if(contentBuf!=null){
			if(!contentBuf.isEmpty()){
				if(contentBuf.contains("_S_b.jpg\";")){
					contentBuf = contentBuf.substring(contentBuf.indexOf("_S_b.jpg\";")+10, contentBuf.length());
				}
				contentBuf = contentBuf.replaceAll("&#160;", "");
			}
			if(contentBuf!=null&&contentBuf.contains("// <![CDATA["))
				contentBuf = contentBuf.substring(0,contentBuf.indexOf("// <![CDATA["));
			if(contentBuf!=null&&contentBuf.contains("media_span_url"))
				contentBuf = contentBuf.substring(0, contentBuf.indexOf("media_span_url"));
			if(contentBuf!=null&&contentBuf.contains("http://"))
				contentBuf = contentBuf.substring(0,contentBuf.indexOf("http://"));
			if(contentBuf!=null){
				contentBuf = contentBuf.replaceFirst("\\s+", "");
				contentBuf = contentBuf.replaceAll("<!--(.*?)-->", "");
				contentBuf = contentBuf.replaceAll("����", "");
			}
			
		}
		return contentBuf;
	}
	@Override
	public String findNewsImages(String html , String[] label) {
		if(html == null)
			return null;
		String bufHtml = "";        //����
		String imageNameTime  = "";
		if(html.contains("<!-- ���� -->")&&html.contains("<!-- ���� -->"))
			bufHtml = html.substring(html.indexOf("<!-- ���� -->"), html.indexOf("<!-- ���� -->"));
		else 
			return null;
		//��ȡͼƬʱ�䣬Ϊ��������
		imageNameTime = findNewsTime(html,label) ;
		if(imageNameTime == null)
			return null;
		//���������ͼƬ���ļ���
	    File f = new File("SOHUGuoNei");
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
		String imageReg = "http://photocdn.sohu.com/[0-9]{4}[0-9]{2}[0-9]{2}/Img[0-9]{9}.jpg";
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
					fileBuf = new File("SOHUGuoNei",imageNameTime+photohour+photominute+photosecond+"000"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf); 
					imageLocation.offer(fileBuf.getPath());
				}else if(imageNumber < 100){
					fileBuf = new File("SOHUGuoNei",imageNameTime+photohour+photominute+photosecond+"00"+imageNumber+"000"+i+imageNameSuffix);
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
//	          	 System.out.println("��ʼ����:" + url);  
				while ((length = in.read(buf, 0, buf.length)) != -1) {  
					fo.write(buf, 0, length);  
				}  
				in.close();  
				fo.close();  
//	            System.out.println(imageName + "�������"); 
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
		if(categroyBuf!=null&&categroyBuf.contains("&gt;")){
			categroyBuf = categroyBuf.replaceAll("&gt;", "");
		}
		return categroyBuf;
	}
	
	public static void main(String[] args){
		SOHUGuoNei test  = new SOHUGuoNei();
		test.getSOHUGuoNeiNews();
	}
}