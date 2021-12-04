import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class P2P
{
    public static HashMap<String,RemotePeerInfo> remotePeerInfoHashMap=new HashMap<>();
    public  static HashMap<String,RemotePeerInfo> preferredNeighboursInfoHashMap=new HashMap<>();
    static  Logger l;
    public static PayLoadData currentDataPayLoad=null;
    static int clientPort;
    public static ServerSocket socket=null;
    public static Thread thread;
    public static String peerId;
    public static Queue<DataParams> queue = new LinkedList<>();
    public static Vector<Thread> serverThread=new Vector<>();
    public  static Vector<Thread> peerThread=new Vector<>();
    public static void main(String args[]) throws Exception
    {
         peerId=args[0];
       l =new Logger("Peer_"+peerId+".log");
       boolean flag=false;
        try
        {

            l.showLog(peerId+" is started");
            getConfigData();
            getPeerInfoDate();
            setPreferredNeighbours(peerId);
            x:for(Map.Entry<String,RemotePeerInfo> hm: remotePeerInfoHashMap.entrySet() )
            {
                RemotePeerInfo r = hm.getValue();
                if (r.peerId.equals(peerId))
                {
                    clientPort = Integer.parseInt(r.peerPort);


                    if (r.hasFile)
                    {
                      flag=true;
                      break x;
                    }
                }
            }
            currentDataPayLoad=new PayLoadData();
            currentDataPayLoad.initPayLoad(peerId,flag);
            Thread t=new Thread(new DataController(peerId));
            t.start();
            if(flag)
            {
                try
                {
                    P2P.socket = new ServerSocket(clientPort);
                    thread = new Thread(new Server(P2P.socket, peerId));
                    thread.start();
                }
                catch (Exception ex)
                {
                   l.showLog(peerId+ " peer is getting an exception while starting the thread");
                    l.closeLog();
                    System.exit(0);
                }
            }
            else
            {
                generatePeerFile();
                for(Map.Entry<String,RemotePeerInfo> hm: remotePeerInfoHashMap.entrySet() )
                {
                    RemotePeerInfo remotePeerInfo=hm.getValue();
                    if(Integer.parseInt(peerId)>Integer.parseInt(hm.getKey()))
                    {
                        Thread temp=new Thread(new PeerController(remotePeerInfo.getPeerAddress(),Integer.parseInt(remotePeerInfo.getPeerPort()),1,peerId));
                        peerThread.add(temp);
                        temp.start();
                    }

                }
                try
                {
                    P2P.socket = new ServerSocket(clientPort);
                    thread = new Thread(new Server(P2P.socket, peerId));
                    thread.start();
                }
                catch (Exception ex)
                {
                    l.showLog(peerId+ " peer is getting an exception while starting the thread");
                    l.closeLog();
                    System.exit(0);
                }

            }


        }
        catch (Exception ex)
        {
            l.showLog(ex.getMessage());
        }

    }

    private static void generatePeerFile()
    {
        try
        {
            File f=new File(peerId,Constants.fileName);
            OutputStream fop=new FileOutputStream(f,true);
            byte intialByte=0;
            int i=0;
            while(i<Constants.fileSize)
            {
                fop.write(intialByte);
                i++;
            }
            fop.close();

        }
        catch (Exception e)
        {
            l.showLog("Error while creating intial dummy file for peer "+peerId);
        }
    }

    private static void setPreferredNeighbours(String pId)
    {
        for(Map.Entry<String,RemotePeerInfo> hm: remotePeerInfoHashMap.entrySet() )
        {

         if(!hm.getKey().equals(pId))
         {
             preferredNeighboursInfoHashMap.put(hm.getKey(), hm.getValue());
         }
        }
    }

    private static void getPeerInfoDate() throws IOException {
        String configs;
        BufferedReader b = null;

        try
        {
            b=new BufferedReader(new FileReader(Constants.PEERS_PATH));
            while((configs=b.readLine())!=null)
            {
                String[] line=configs.split(" ");
                remotePeerInfoHashMap.put(line[0],new RemotePeerInfo(line[0],line[1],line[2],line[3].equals("1")));

            }
        }
        catch (Exception ex)
        {
            l.showLog(ex.getMessage());
        }
        finally
        {
            b.close();
        }
    }

    public static void getConfigData() throws IOException {
        String configs;
        BufferedReader b = null;
        try
        {
             b=new BufferedReader(new FileReader(Constants.COMMON_CONFIG_PATH));
            while((configs=b.readLine())!=null)
            {

                String[] line=configs.split(" ");
                if(line[0].trim().equals("NumberOfPreferredNeighbors"))
                {
                    Constants.numberOfPreferredNeighbors=Integer.parseInt(line[1]);
                }
                if(line[0].trim().equals("UnchokingInterval"))
                {
                    Constants.unchokingInterval=Integer.parseInt(line[1]);
                }
                if(line[0].trim().equals("OptimisticUnchokingInterval"))
                {
                    Constants.optimisticUnchokingInterval=Integer.parseInt(line[1]);
                }
                if(line[0].trim().equals("FileName"))
                {
                    Constants.fileName=line[1];
                }
                if(line[0].trim().equals("FileSize"))
                {
                    Constants.fileSize=Integer.parseInt(line[1]);
                }
                if(line[0].trim().equals("PieceSize"))
                {
                    Constants.pieceSize=Integer.parseInt(line[1]);
                }
            }
        }
        catch (Exception ex)
        {
           l.showLog(ex.getMessage());
        }
        finally
        {
            b.close();
        }
    }
    public static synchronized DataParams removeDataFromQueue(){
        DataParams dp = null;
        if(queue.isEmpty()){}
        else {
            dp = queue.remove();
        }
        return dp;
    }
}
