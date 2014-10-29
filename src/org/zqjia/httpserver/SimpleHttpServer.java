package org.zqjia.httpserver;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.net.*;
import java.util.concurrent.*;

public class SimpleHttpServer 
{
    private int port=80;
    private ServerSocketChannel serverSocketChannel = null;
    private ExecutorService executorService;
    private static final int POOL_MULTIPLE = 4;

    public SimpleHttpServer() throws IOException 
    {
	    executorService= Executors.newFixedThreadPool(
		    Runtime.getRuntime().availableProcessors() * POOL_MULTIPLE);
	    serverSocketChannel= ServerSocketChannel.open();
	    serverSocketChannel.socket().setReuseAddress(true);
	    serverSocketChannel.socket().bind(new InetSocketAddress(port));
	    System.out.println("服务器启动");
    }

  public void service() 
  {
      while (true) 
      {
	      SocketChannel socketChannel=null;
	      try 
	      {
	          socketChannel = serverSocketChannel.accept();
	          executorService.execute(new Handler(socketChannel));
	      }
	      catch (IOException e) 
	      {
	          e.printStackTrace();
	      }
     }
  }

  public static void main(String args[])throws IOException 
  {
      new SimpleHttpServer().service();
  }
  
  class Handler implements Runnable
  {
	  private SocketChannel socketChannel;
	  public Handler(SocketChannel socketChannel)
	  {
	      this.socketChannel = socketChannel;
	  }
	  
	  public void run()
	  {
	      handle(socketChannel);
	  }

	  
	  private void handle(SocketChannel socketChannel)
	  {
		  try 
		  {
	          Socket socket=socketChannel.socket();
	          System.out.println("接收到客户连接，来自: " +
	        		  	socket.getInetAddress() + ":" +socket.getPort());
	
	          ByteBuffer buffer = ByteBuffer.allocate(1024);
	          //Reads a sequence of bytes from this channel into the given buffer
	          socketChannel.read(buffer);
	          //flip the buffer
	          //大概意思就是让buffer中只存在读取到的数据
	          buffer.flip();
	          //解码buffer中的数据，网络发送过来的数据按照一定格式编码
	          String request = decode(buffer);
	          System.out.print(request);  //打印HTTP请求
	
	          //输出HTTP响应结果
	          StringBuffer sb=new StringBuffer("HTTP/1.1 200 OK\r\n");
	          //\r\n\r\n说明http头结束
	          sb.append("Content-Type:text/html\r\n\r\n");
	          socketChannel.write(encode(sb.toString()));//输出响应头
	
	          //这里构造http响应的内容
	          FileInputStream in;
	          //获得HTTP请求的第一行
	          String firstLineOfRequest = request.substring(0, request.indexOf("\r\n"));
	          if(firstLineOfRequest.indexOf("login.htm") != -1)
	        	  //此处的root是指与src同级的目录
	              in = new FileInputStream("root/login.htm");
	          else
	              in=new FileInputStream("root/hello.htm");
	
	          FileChannel fileChannel=in.getChannel();
	          //Transfers bytes from this channel's file to the given writable byte channel
	          fileChannel.transferTo(0, fileChannel.size(), socketChannel);
	          fileChannel.close();
	      }
		  catch (Exception e)
		  {
	         e.printStackTrace();
	      }
		  finally 
		  {
	         try
	         {
	             if(socketChannel!=null)
	            	 socketChannel.close();
	         }
	         catch (IOException e) 
	         {
	        	 e.printStackTrace();
	         }
	      }
	  }
	  
	  private Charset charset=Charset.forName("UTF-8");
	  
	  public String decode(ByteBuffer buffer)
	  {  //解码
		  CharBuffer charBuffer= charset.decode(buffer);
		  return charBuffer.toString();
	  }
	  
	  public ByteBuffer encode(String str)
	  {  //编码
		  return charset.encode(str);
	  }
  	}

}

