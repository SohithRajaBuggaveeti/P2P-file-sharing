import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

@SuppressWarnings("deprecation")
public class P2P
{
    public static HashMap<String,RemotePeerInfo> remotePeerInfoHashMap=new HashMap<>();
    public  static HashMap<String,RemotePeerInfo> preferredNeighboursInfoHashMap=new HashMap<>();
    static  Logger l;
    public static volatile Timer timer;
    public static PayLoadData currentDataPayLoad=null;
    static int clientPort;
    public static ServerSocket socket=null;
    public static Thread thread;
    public static String peerId;
    public static boolean finishedFlag = false;
    public static Queue<DataParams> queue = new LinkedList<>();
    public static Vector<Thread> serverThread=new Vector<>();
    public static Thread messageProcessor;
    public  static Vector<Thread> peerThread=new Vector<>();
    public static HashMap<String,Socket> peerData=new HashMap<>();
    public static volatile Hashtable<String, RemotePeerInfo> preferredNeighboursTable = new Hashtable<>();
    public static volatile Hashtable<String, RemotePeerInfo> unchokedNeighboursTable = new Hashtable<>();
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
            Thread t=new Thread(new MessageProcessor(peerId));
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
                        PeerController p=new PeerController(remotePeerInfo.getPeerAddress(),Integer.parseInt(remotePeerInfo.getPeerPort()),1, peerId);
                        Thread temp=new Thread(p);
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
            initializePreferredNeighbors();
            initializeUnChokedNeighbors();
              Thread clientThread=thread;
            Thread messageProcessor=t;
            while(true) {
                finishedFlag = isCompleted();
                if (finishedFlag) {
                    l.showLog("All peers have completed downloading the file.");

                    stopPreferredNeighbors();
                    abortUnChokedNeighbors();

                    try {
                        Thread.currentThread();
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }

                    if (clientThread.isAlive())
                       clientThread.stop();

                    if (messageProcessor.isAlive())
                        messageProcessor.stop();

                    for (Thread thread : peerThread)
                        if (thread.isAlive())
                            thread.stop();

                    for (Thread thread : serverThread)
                        if (thread.isAlive())
                            thread.stop();

                    break;
                } else {
                    try {
                        Thread.currentThread();
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {}
                }
            }
        }
        catch(Exception exception) {
            l.showLog(String.format("[%s] Exception in ending : [%s]", peerId, exception.getMessage()));
        }
        finally {
            l.showLog(String.format("[%s] Peer process is exiting", peerId));
            l.closeLog();
            System.exit(0);
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
            if (!hm.getKey().equals(pId)) {
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
    public static synchronized void addToQueue(DataParams dp)
    {
        queue.add(dp);
    }
    public static void nextReadPeerInfo() {
        try {
            String peerInfoDetails;
            BufferedReader bufferedReader = new BufferedReader(new FileReader(Constants.PEERS_PATH));
            while ((peerInfoDetails = bufferedReader.readLine()) != null) {
                String[]peerDetails = peerInfoDetails.trim().split("\\s+");
                String peerID = peerDetails[0];
                int finishedIndicator = Integer.parseInt(peerDetails[3]);
                if(finishedIndicator == 1) {
                    remotePeerInfoHashMap.get(peerID).isCompleted = 1;
                    remotePeerInfoHashMap.get(peerID).isInterested = 0;
                    remotePeerInfoHashMap.get(peerID).isChoked = 0;
                }
            }
            bufferedReader.close();
        }
        catch (Exception exception) {
            l.showLog(peerId + "" +exception.toString());
        }
    }
    public static class PreferredNeighbours extends TimerTask {
        public void run() {
            nextReadPeerInfo();
            Enumeration<String> remotePeerInfoKeys = Collections.enumeration(remotePeerInfoHashMap.keySet());
            int interestedPeers = 0;
            StringBuilder prefix = new StringBuilder();
            while(remotePeerInfoKeys.hasMoreElements()) {
                String currentRemotePeerInfo = remotePeerInfoKeys.nextElement();
                RemotePeerInfo preferredPeers = remotePeerInfoHashMap.get(currentRemotePeerInfo);
                if(currentRemotePeerInfo.equals(peerId)) continue;
                if (preferredPeers.isCompleted == 0 && preferredPeers.isHandShake == 1)
                    interestedPeers++;
                else if(preferredPeers.isCompleted == 1) {
                    try {
                        preferredNeighboursInfoHashMap.remove(currentRemotePeerInfo);
                    }
                    catch (Exception ignored) { }
                }
            }
            if(interestedPeers > Constants.piece) {
                boolean preferredNeighboursFlag = preferredNeighboursInfoHashMap.isEmpty();
                if(!preferredNeighboursFlag)
                    preferredNeighboursInfoHashMap.clear();

                List<RemotePeerInfo> remotePeerInfosList = new ArrayList<>(remotePeerInfoHashMap.values());
                remotePeerInfosList.sort(new RemotePeerInfo());
                int neighboursCount = 0;

                for (RemotePeerInfo remotePeerInfo : remotePeerInfosList) {
                    if (neighboursCount > Constants.numberOfPreferredNeighbors - 1) break;

                    if (remotePeerInfo.isHandShake == 1 && !remotePeerInfo.peerId.equals(peerId)
                            && remotePeerInfoHashMap.get(remotePeerInfo.peerId).isCompleted == 0) {
                        remotePeerInfoHashMap.get(remotePeerInfo.peerId).isPreferredNeighbor = 1;
                        preferredNeighboursInfoHashMap.put(remotePeerInfo.peerId, remotePeerInfoHashMap.get(remotePeerInfo.peerId));
                        neighboursCount++;
                        prefix.append(remotePeerInfo.peerId).append(", ");

                        if (remotePeerInfoHashMap.get(remotePeerInfo.peerId).isChoked == 1) {
                            sendUnChokePayload(P2P.peerData.get(remotePeerInfo.peerId), remotePeerInfo.peerId);
                            P2P.remotePeerInfoHashMap.get(remotePeerInfo.peerId).isChoked = 0;
                            sendHavePayload(P2P.peerData.get(remotePeerInfo.peerId), remotePeerInfo.peerId);
                            P2P.remotePeerInfoHashMap.get(remotePeerInfo.peerId).state = 3;
                        }
                    }
                }
            }
            else
            {
                remotePeerInfoKeys = Collections.enumeration(remotePeerInfoHashMap.keySet());
                while(remotePeerInfoKeys.hasMoreElements())
                {
                    String nextElement = remotePeerInfoKeys.nextElement();
                    RemotePeerInfo nextRemotePeerInfo = remotePeerInfoHashMap.get(nextElement);
                    if(nextElement.equals(peerId)) continue;

                    if (nextRemotePeerInfo.isCompleted == 0 && nextRemotePeerInfo.isHandShake == 1) {
                        if(!preferredNeighboursInfoHashMap.containsKey(nextElement)) {
                            prefix.append(nextElement).append(", ");
                            preferredNeighboursInfoHashMap.put(nextElement, remotePeerInfoHashMap.get(nextElement));
                            remotePeerInfoHashMap.get(nextElement).isPreferredNeighbor = 1;
                        }
                        if (nextRemotePeerInfo.isChoked == 1) {
                            sendUnChokePayload(P2P.peerData.get(nextElement), nextElement);
                            P2P.remotePeerInfoHashMap.get(nextElement).isChoked = 0;
                            sendHavePayload(P2P.peerData.get(nextElement), nextElement);
                            P2P.remotePeerInfoHashMap.get(nextElement).state = 3;
                        }
                    }
                }
            }
            if (!prefix.toString().equals(""))
                l.showLog(String.format("[%s] has selected the preferred neighbors [%s]", P2P.peerId, prefix));
        }
    }

    private static void sendUnChokePayload(Socket serverSocket, String remotePeerID) {
        l.showLog(String.format("[%s] is sending 'unchoke' message to Peer [%s]", peerId, remotePeerID));
        MessageData message = new MessageData(Constants.unChoke);
        byte[] messageToByteArray = MessageData.convertDataToByteArray(message);
        sendMessage(serverSocket, messageToByteArray);
    }

    private static void sendHavePayload(Socket socket, String remotePeerID) {
        byte[] encodedBitField = P2P.currentDataPayLoad.encodeData();
        l.showLog(String.format("[%s] is sending 'have' message to Peer [%s]", peerId, remotePeerID));
        MessageData message = new MessageData(Constants.have, encodedBitField);
        sendMessage(socket, MessageData.convertDataToByteArray(message));
    }

    private static void sendMessage(Socket serverSocket, byte[] encodedBitField) {
        try {
            OutputStream outputStream = serverSocket.getOutputStream();
            outputStream.write(encodedBitField);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public static class UnChokedNeighbors extends TimerTask {

        public void run() {
            nextReadPeerInfo();
            if(!unchokedNeighboursTable.isEmpty())
                unchokedNeighboursTable.clear();
            Enumeration<String> remotePeerInfosKeys = Collections.enumeration(remotePeerInfoHashMap.keySet());
            Vector<RemotePeerInfo> remotePeersList = new Vector<>();
            while(remotePeerInfosKeys.hasMoreElements()) {
                String key = remotePeerInfosKeys.nextElement();
                RemotePeerInfo pref = remotePeerInfoHashMap.get(key);
                if (pref.isChoked == 1
                        && !key.equals(peerId)
                        && pref.isCompleted == 0
                        && pref.isHandShake == 1)
                    remotePeersList.add(pref);
            }

            if (remotePeersList.size() > 0) {
                Collections.shuffle(remotePeersList);
                RemotePeerInfo firstRemotePeer = remotePeersList.firstElement();
                remotePeerInfoHashMap.get(firstRemotePeer.peerId).isOptUnchokedNeighbor = 1;
                unchokedNeighboursTable.put(firstRemotePeer.peerId, remotePeerInfoHashMap.get(firstRemotePeer.peerId));
                P2P.l.showLog(String.format("[%s] has the optimistically unchoked neighbor [%s]",
                        P2P.peerId, firstRemotePeer.peerId));

                if (remotePeerInfoHashMap.get(firstRemotePeer.peerId).isChoked == 1) {
                    P2P.remotePeerInfoHashMap.get(firstRemotePeer.peerId).isChoked = 0;
                    sendUnChokePayload(P2P.peerData.get(firstRemotePeer.peerId), firstRemotePeer.peerId);
                    sendHavePayload(P2P.peerData.get(firstRemotePeer.peerId), firstRemotePeer.peerId);
                    P2P.remotePeerInfoHashMap.get(firstRemotePeer.peerId).state = 3;
                }
            }
        }
    }

    public static void initializeUnChokedNeighbors() {
        timer = new Timer();
        timer.schedule(new UnChokedNeighbors(),
                0,Constants.optimisticUnchokingInterval * 1000L);
    }

    public static void abortUnChokedNeighbors() {
        timer.cancel();
    }

    public static void initializePreferredNeighbors() {
        timer = new Timer();
        timer.schedule(new PreferredNeighbours(),
                0,Constants.unchokingInterval * 1000L);
    }

    public static void stopPreferredNeighbors() {
        timer.cancel();
    }
    private static void initializePreferredNeighbours() {
        Enumeration<String> remotePeerInfos =Collections.enumeration(remotePeerInfoHashMap.keySet());
        while(remotePeerInfos.hasMoreElements()) {
            String nextElement = remotePeerInfos.nextElement();
            if(!nextElement.equals(peerId))
                preferredNeighboursTable.put(nextElement, remotePeerInfoHashMap.get(nextElement));
        }
    }

    public static synchronized boolean isCompleted() {
        String peerInfoDetail;
        int hasFileCount = 1;

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(
                    Constants.PEERS_PATH));
            while ((peerInfoDetail = bufferedReader.readLine()) != null) {
                hasFileCount = hasFileCount
                        * Integer.parseInt(peerInfoDetail.trim().split("\\s+")[3]);
            }
            bufferedReader.close();
            return hasFileCount != 0;
        } catch (Exception e) {
            l.showLog(e.toString());
            return false;
        }
    }
  

   

}
