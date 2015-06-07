/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;



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
    private static final ArrayList<ChatRoom> _chatRooms = new ArrayList<ChatRoom>();
    
    ChatServer(Socket socket, int num )
    {
        _socket = socket;
        _num = num;
        _user = new ChatUser(num, this);
        
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
                        out.write(">> ");
                        out.flush();
                        name = in.readLine();
                    }
                    else if (response == DUPLICATE_NAME)
                    {
                        out.write( "This name is already in use. Please try again: \n\r" );
                        out.write(">> ");
                        out.flush();
                        name = in.readLine();
                    }
                    else
                    {
                        break;
                    }
                }
                _user.setName(name);
 
                connect(_user);
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
    
    public void connect(ChatUser user)
    {
        try
        {
            BufferedReader in = new BufferedReader( new InputStreamReader( _socket.getInputStream() ) );
            OutputStreamWriter out = new OutputStreamWriter( _socket.getOutputStream() );
            print( "Welcome " + user.getName()+ "!!! Please join or create a chat room. "
                    + "For available commands, use '/help'! \n\r", out);
            while ( true )
            {
                String textInput = in.readLine();
             
                serverLog( user.getName()+ ": " + textInput );
                if ( textInput.equals( "/quit" ) )
                {
                    System.out.println( user.getName()+ " closed connection." );
                    print("** BYE **\n\r", out);
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
                    if(user.inChatRoom())
                    {
                        sendRoomMessage(user, user.getChatRoom(), textInput, false);
                    }
                    else
                    {
                        print("Invalid command. Please join a room to chat.\n\r", out);
                    }
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
            print(msg + "\n\r", out);
            
        }catch( IOException e )
        {
            serverLog( _user.getNum() + " Error: " + e.toString() );
        }
    }
    
    /**Unused
    public void sendGlobalMessage(ChatUser user, String msg)
    {
        for(ChatServer thread:_threads)
        {
            thread.addMessage(user.getName() + ": " + msg);
            //Loop through and add the chat to everyones screen
        }
    }
    **/
    
    public void sendRoomMessage(ChatUser user, ChatRoom currRoom, String msg, Boolean isStatusMsg)
    {
        ArrayList<ChatUser> roomUsers = currRoom.getUsers();
        //Loop through and add the chat to everyones screen
        for(ChatUser roomUser:roomUsers)
        {
            //Send this message to everyone but yourself
            //if(!roomUser.equals(user))
            //{
                if(isStatusMsg)
                    roomUser.getThread().addMessage(msg);
                else
                {
                    DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);
                    format.setTimeZone(TimeZone.getTimeZone("PST"));
                    Date today = new Date();
                    roomUser.getThread().addMessage("[" + format.format(today) + "] "
                            + user.getName() + ": " + msg);
                }
            //}
        }
    }
   
    public void checkCommand(String textInput, OutputStreamWriter out)
    {
        try
        {
            //Check room joining first because room name is inputted by the user
            //serverLog("substring: " + textInput.substring(1,5));
            String cmd = textInput.substring(1,5);
            if(cmd.matches("join"))
            {
                serverLog("textinput is: " + textInput);
                if(textInput.length() > 5 && textInput.substring(6) != null)
                {
                    //Make sure they're not already in a room
                    if(_user.inChatRoom())
                    {
                        print("Please leave this room first.\n\r", out);
                        return;
                    }
                    String roomName = textInput.substring(6);
                    serverLog("Room " + roomName + " created.");
                    //See if room already exists
                    for(ChatRoom room:_chatRooms)
                    {
                        if(room.getName().equals(roomName))
                        {
                            //join room
                            _user.setChatRoom(room);
                            room.addUser(_user);
                            sendRoomMessage(_user, room, "** " + _user.getName() + " has joined the room.", true);
                            out.write("You have been added to the room: " + roomName +"\n\r");
                            out.write("Current users: \n\r");
                            
                            //Print all users currently in the room
                            ArrayList<ChatUser> roomUsers = room.getUsers();
                            for( ChatUser roomUser : roomUsers)
                            {
                                out.write(" ** " + roomUser.getName() + "\n\r");
                            }
                            print("\n\r", out);
                            return;
                        }
                    }
                    //If it reaches here, no room was found. Time to make one
                    ChatRoom newRoom = new ChatRoom(roomName);
                    newRoom.addUser(_user);
                    _user.setChatRoom(newRoom);
                    _chatRooms.add(newRoom);
                    print("You have created the room: " + roomName +"\n\r", out);
                    return;
                }
                else
                {
                    print("Please enter a valid room name.\n\r", out);
                }
                return;
            }
            
            switch(textInput)
            {
                case "/help":
                    out.write("         /users               - List of chat users\n\r");
                    out.write("         /rooms               - List of active chat rooms\n\r");
                    out.write("         /join [roomname]     - Join specified chat room\n\r");
                    out.write("         /leave               - Leave current chat room\n\r");
                    out.write("         /quit                - Disconnect\n\r");
                    print("\n\r", out);
                    break;
                    
                case "/users":
                    if(_user.inChatRoom())
                    {
                        ChatRoom currRoom = _user.getChatRoom();
                        ArrayList<ChatUser> _roomUsers = currRoom.getUsers();
                        for (ChatUser user:_roomUsers)
                        {
                            out.write("** " + user.getName() + "\n\r");
                        }
                        print("End of list.\n\r", out);
                    }
                    else
                    {
                        print("You must join a room first. \n\r", out);
                    }
                    break; 
                
                case "/rooms":
                    if(_chatRooms.isEmpty())
                    {
                        print("There are no active chatrooms. Please create one using the command"
                                + " '/join [roomname]'\n\r", out);
                    }
                    else
                    {
                        out.write("** Name  |  (Users) **\n\r");
                        for(ChatRoom room : _chatRooms)
                        {
                            out.write("** " + room.getName() + "  |  (" + room.getNumUsers() + ") \n\r");
                        }
                        print("End of list.\n\r", out);
                    }
                    break;
                      
                case "/leave":
                    ChatRoom currRoom = _user.getChatRoom();
                    currRoom.removeUser(_user);
                    sendRoomMessage(_user, currRoom, "** " + _user.getName() + " has left the room.", true);
                            
                    //If the room is empty, remove the room
                    if(currRoom.getUsers().isEmpty())
                    {
                        serverLog("Room " + currRoom.getName() + " deleted.\n\r");
                        _chatRooms.remove(currRoom);
                    }
                    _user.setChatRoom(null);
                    print("You have left the room.\n\r", out);
                    break;
                    
                default:
                    print("Invalid command. See /help.\n\r", out);
                    break;
            }
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
            
            for(ChatServer thread:_threads)
            {
                //If name already exists
                if(thread._user.getName().equals(name))
                {
                    return DUPLICATE_NAME;
                }
            }
            return VALID_NAME;
        }
    }
    
    public static void serverLog(String text)
    {
        System.out.print(text + "\r\n");

        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("log.txt", true)))) 
        {
             out.println(text);
        }
        catch( IOException e )
        {
            System.out.println(" Error: " + e.toString() );
        }
}
    //Print user messages
    public void print(String msg, OutputStreamWriter out)
    {
        try
        {
            out.write(msg);
            out.write(">> ");
            out.flush();
        }
        catch( IOException e )
        {
            System.out.println(" Error: " + e.toString() );
        }
    }
}
