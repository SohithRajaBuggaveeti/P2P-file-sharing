import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class P2P
{
    public static HashMap<String,RemotePeerInfo> remotePeerInfoHashMap=new HashMap<>();
    public  static HashMap<String,RemotePeerInfo> preferredNeighboursInfoHashMap=new HashMap<>();
    static  Logger l;
    static int clientPort;
    static int presentPeer;
    public static void main(String args[]) throws Exception
    {
        String peerId=args[0];
       l =new Logger("Peer_"+peerId+".log");
        try
        {
            boolean flag=false;
            l.showLog(peerId+" is started");
            getConfigData();
            getPeerInfoDate();
            setPreferredNeighbours(peerId);
            for(Map.Entry<String,RemotePeerInfo> hm: remotePeerInfoHashMap.entrySet() )
            {
                RemotePeerInfo r=hm.getValue();
                if(r.peerId.equals(peerId))
                {
                    clientPort=Integer.parseInt(r.peerPort);
                    presentPeer=r.peerPos;
                }
                if(r.)
            }

        }
        catch (Exception ex)
        {
            l.showLog(ex.getMessage());
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
        int i=0;
        try
        {
            b=new BufferedReader(new FileReader("PeerInfo.cfg"));
            while(b.readLine()!=null)
            {
                configs=b.readLine();
                String[] line=configs.split(" ");
                remotePeerInfoHashMap.put(line[0],new RemotePeerInfo(line[0],line[1],line[2],line[3].equals("1"),i));
                i++;
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
             b=new BufferedReader(new FileReader("CommonConfig.cfg"));
            while(b.readLine()!=null)
            {
                configs=b.readLine();
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
}
