/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;


/**
 * Simple server to use with telnet
 * @author Michelle Khuu
 */
public class ChatServer implements Runnable{
    private static final int VALID_NAME = 0;
    private static final int DUPLICATE_NAME = 1;
    private static final int INVALID_NAME = 2;
    
    private final Socket _socket;
    private final int _num;
    private final ChatUser _user;   //keep a reference of the current user on this thread
    
    //Track all the connected user threads
    private static final ArrayList<ChatServer> _threads = new ArrayList<ChatServer>();
    //private final ArrayList<ChatUser> _users; 
    
    ChatServer(Socket socket, int num )
    {
        _socket = socket;
        _num = num;
        _user = new ChatUser(num);
        Thread handler = new Thread( this, "handler-" + _num );
        handler.start();
    }
    
    public static void main(String[] args) throws Exception
    {
        int port = 9000;
        if ( args.length > 0 )
        {
            port = Integer.parseInt( args[0] );
        }
        serverLog( "Accepting connections on port: " + port );
        int nextNum = 1;
        ServerSocket serverSocket = new ServerSocket( port );
        while ( true )
        {
            Socket socket = serverSocket.accept();
            ChatServer server = new ChatServer( socket, nextNum++ );
            addServerThread(server);
        }
    }
    
    public static void addServerThread(ChatServer server)
    {
        _threads.add(server);
        serverLog("Thread size: " + _threads.size());
    }
    //Connect the user
    public void run()
    {
        try
        {
            try
            {
                serverLog( _num + " Connected." );

                OutputStreamWriter out = new OutputStreamWriter( _socket.getOutputStream() );
                out.write( "Please enter your name: " );
                out.flush();

                BufferedReader in = new BufferedReader( new InputStreamReader( _socket.getInputStream() ) );
                String name = in.readLine();
                //Exits loop once you enter a valid name
                while(true)
                {
                    int response = checkValidName(name);

                    if(response == INVALID_NAME)
                    {
                        out.write( "That name is invalid. Please try again: \n\r" );
                        out.write("=>> ");
                        out.flush();
                        name = in.readLine();
                    }
                    else if (response == DUPLICATE_NAME)
                    {
                        out.write( "This name is already in use. Please try again: \n\r" );
                        out.write("=>> ");
                        out.flush();
                        name = in.readLine();
                    }
                    else
                    {
                        break;
                    }
                }
                _user.setName(name);
                //_users.add(user);
                joinChat(_user);
            }
            //If connection gets closed, remove it 
            finally
            {
                serverLog("Close connection for user " + _num);
                _socket.close();
            }
        
        }
        catch ( IOException e )
        {
            serverLog( _num + " Error: " + e.toString() );
        }
    }
    
    public void joinChat(ChatUser user)
    {
        try
        {
            BufferedReader in = new BufferedReader( new InputStreamReader( _socket.getInputStream() ) );
            OutputStreamWriter out = new OutputStreamWriter( _socket.getOutputStream() );
            out.write( "Welcome " + user.getName()+ "!!! You have entered the chat room. "
                    + "for commands, use '/help'! \n\r" );
            out.write("=>> ");
            out.flush();

            while ( true )
            {
                String textInput = in.readLine();
             
                serverLog( user.getName()+ ": " + textInput );
                if ( textInput.equals( "/exit" ) )
                {
                    System.out.println( user.getNum() + " Closing Connection." );
                    _threads.remove(this);
                    return;
                }
                else if(String.valueOf(textInput.charAt(0)).equals("/"))
                {
                    //Special character cases
                    checkCommand(textInput, out);
                }
                else
                {
                    sendGlobalMessage(user, textInput);
                    //out.write( user.getName() + ": " + textInput + "\n\r" );
                }
            }
        }catch ( IOException e )
        {
            serverLog( user.getNum() + " Error: " + e.toString() );
        }
    }
    
    //Add message to your screen
    public void addMessage(String msg)
    {
        try
        {
            OutputStreamWriter out = new OutputStreamWriter( _socket.getOutputStream() );
            out.write(msg + "\n\r");
            out.write("=>> ");
            out.flush();
        }catch( IOException e )
        {
            serverLog( _user.getNum() + " Error: " + e.toString() );
        }
    }
    
    public void sendGlobalMessage(ChatUser user, String msg)
    {
        for(ChatServer thread:_threads)
        {
            thread.addMessage(user.getName() + ": " + msg);
            //Loop through and add the chat to everyones screen
        }
    }
    
    public void checkCommand(String textInput, OutputStreamWriter out)
    {
        try
        {
            switch(textInput)
            {
                case "/help":
                    out.write("    /users    - List of chat users\n\r");
                    out.write("    /exit     - Exit chat room\n\r");
                    out.flush();
                    break;
                case "/users":
                    //for (ChatUser user:_users)
                    //{
                    //    out.write(user.getName() + "\n\r");
                    //}
                    out.flush();
                    break;     
                default:
                    out.write("Invalid command. See /help.\n\r");
                    out.flush();
                    break;
            }
            out.flush();
        }catch ( IOException e )
        {
            System.out.println(" Error: " + e.toString() );
        }
    }
    public int checkValidName(String name)
    {
        if ( name == null || name == "" || name.length() == 0 )
        {
            return INVALID_NAME;
        }      
        else
        {
            //Check names to see if there are dupes
            /**
            for(int i=0; i < _users.size(); i++)
            {
                //If name already exists
                if(_users.get(i).getName().equals(name))
                {
                    return DUPLICATE_NAME;
                }
            }**/
            return VALID_NAME;
        }
    }
    
    public static void serverLog(String text)
    {
        System.out.println( text );
    }
}
