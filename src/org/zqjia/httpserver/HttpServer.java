
package org.zqjia.httpserver;

import java.io.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.net.*;
import java.util.*;

public class HttpServer
{
    private Selector selector = null;
    private ServerSocketChannel serverSocketChannel = null;
    private int port = 80;

    public HttpServer()throws IOException
    {
    	//调用静态工厂方法open新建一个selector方法
	    selector = Selector.open();
	    //调用静态工厂方法open方法新建一个serverSocketChannel对象
	    serverSocketChannel= ServerSocketChannel.open();
	    //设置得到的serverSocket地址可重用
	    serverSocketChannel.socket().setReuseAddress(true);
	    //设置通道的阻塞模式，这里设置为非阻塞
	    serverSocketChannel.configureBlocking(false);
	    //设置serverSocket绑定监听端口
	    serverSocketChannel.socket().bind(new InetSocketAddress(port));
	    System.out.println("服务器启动");
    }

	public void service() throws IOException
    {
    	//Registers this channel with the given selector, returning a selection key
		//设置接收连接就绪事件，服务器监听到客户端的连接，就可以接收这个连接了
	    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, new AcceptHandler());
	    for(;;)
	    {
	    	//select采用阻塞的工作方式，返回相关事件已经发生的SelectionKey对象的数目，如果一个没有，则进入阻塞状态
	    	//以下四种情况，select方法将立即返回
	    	//1.至少有一个SelectionKey的相关事件已经发生
	    	//2.其他线程调用了Selector的wakeup方法，导致执行select方法的线程立即从select方法中返回
	    	//3.当前执行select方法的线程被其他线程中断
	    	//4.超出等待时间，此时调用的select(long timeout)这个方法
	        int n = selector.select();
	
	        if(n == 0)
	      	   continue;
	        
	        //返回相关事件已经被Selector捕获的SelectionKey的集合
			Set<SelectionKey> readyKeys = selector.selectedKeys();	
			//得到上述集合的迭代器
	        Iterator<SelectionKey> it = readyKeys.iterator();
	        
	        while (it.hasNext())
	        {
	        	SelectionKey key = null;
	        	try
	        	{
	        		key = (SelectionKey) it.next();
	        		it.remove();
	        		//得到SelectionKey的附件
	        		final Handler handler = (Handler)key.attachment();
	        		//处理相关事件
	        		//注意一个问题，这里Handler只是一个接口，集成该接口的有AcceptHandler和RequestHanlder
	        		//显然上面35行那行程序注册的是AcceptHandler,于是调用AcceptHandler的handle方法处理
	        		//在AcceptHandler的handle方法中，将注册OP_READ方法，当读事件就绪时候，将调用那里注册的那个handler
	        		//来处理，这里handle处理key相关的算是一个多态的概念，它将根据不同的事件触发来选择注册的附件进行相应的处理
	        		//暂时的理解就是这样
	        		handler.handle(key);
	        	}
	        	catch(IOException e)
	        	{
	        		e.printStackTrace();
	        		try{
		        			if(key!=null)
		        			{
		        				key.cancel();
		        				key.channel().close();
		        			}
	        			}		
	        		catch(Exception ex)
	        		{
	        			e.printStackTrace();
	        		}
	        	}
	        }//#while
	    }//#while
  }


    public static void main(String args[])throws Exception
    {
	    final HttpServer server = new HttpServer();
	    server.service();
    }
}

