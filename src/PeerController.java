import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class PeerController implements Runnable
{
    private InputStream ip;
    private OutputStream op;
    public static final int noofActiveSessions=1;
    private Socket socket=null;
    private int sessionType;
    String remotepId;
    String currentpId;
    public void run()
    {
        DataParams dp=new DataParams();
        byte[] messageHandShakeArray=new byte[32];
        byte[] bufferedMessage=new byte[Constants.sizeOfMessage+Constants.typeOfMessage];
        byte[] dataLength;
        byte[] dataType;
        try
        {
            if(this.sessionType !=  noofActiveSessions)
            {
                updatePeersData(messageHandShakeArray);
                if(connectWithPeer())
                {
                    throw  new Exception("Failed to connect with : "+this.currentpId);
                }
                P2P.l.showLog("HandShake has been sent to "+ this.remotepId);
                P2P.remotePeerInfoHashMap.get(remotepId).state=2;

            }
            else
            {
                if(connectWithPeer())
                {
                    throw  new Exception("Failed to connect with : "+this.currentpId);
                }
                P2P.l.showLog("HandShake has been sent to "+ this.remotepId);
                updatePeersData(messageHandShakeArray);
                P2P.remotePeerInfoHashMap.get(remotepId).state=8;
                MessageData md=new MessageData(Constants.bitField,P2P.currentDataPayLoad.encodeData());
                op.write(MessageData.convertDataToByteArray(md));
            }
            x:while (true)
            {
                int hBytes;
                if((hBytes=ip.read(bufferedMessage))==-1)
                {
                    break x;
                }
                dataLength=new byte[Constants.sizeOfMessage];
                dataType=new byte[Constants.typeOfMessage];
                MessageData md=new MessageData();
                md.setDataLength(dataLength);
                md.setDataType(dataType);
                System.arraycopy(bufferedMessage,0,dataLength,0,Constants.sizeOfMessage);
                System.arraycopy(bufferedMessage,Constants.sizeOfMessage,dataType,0,Constants.typeOfMessage);
                String s="0 1 2 3";
                if(s.contains(md.getDataType()))
                {
                  dp.m=md;
                }
                else
                {
                    int readBytes=0;
                    int bytesToRead;
                    byte[] payloadMessage=new byte[md.getLengthOfMessage()-1];
                    while (readBytes<md.getLengthOfMessage()-1)
                    {
                        bytesToRead=ip.read(payloadMessage,readBytes,md.getLengthOfMessage()-1-readBytes);
                        if(bytesToRead==-1)
                        {
                            return ;
                        }
                        readBytes+=bytesToRead;
                    }
                    byte[] messageDataPayLoad=new byte[md.getLengthOfMessage()+Constants.sizeOfMessage];
                    System.arraycopy(bufferedMessage,0,messageDataPayLoad,0,Constants.sizeOfMessage+Constants.typeOfMessage);
                    System.arraycopy(payloadMessage,0,messageDataPayLoad,Constants.sizeOfMessage+Constants.typeOfMessage,payloadMessage.length);
                    dp.m=MessageData.convertByteArrayToData(messageDataPayLoad);
                }
                dp.pId=this.remotepId;
                P2P.addToQueue(dp);
            }
        }
        catch (Exception ex)
        {
            P2P.l.showLog(ex.getMessage());
        }

    }
    public void updatePeersData(byte[] hArray) throws IOException {
        x:while(1==1)
        {
            ip.read(hArray);
            Handshake h=Handshake.byteToHandShake(hArray);
            if(h.getHandShakeHeader().equals(Constants.handshakeHeader))
            {
                remotepId=h.getPeerId()+"";
                P2P.l.showLog(this.currentpId+" makes a connection with peer "+this.remotepId);
                P2P.l.showLog(this.remotepId+" has received a handshake message from "+this.currentpId);
                P2P.peerData.put(this.remotepId,this.socket);
                break x;
            }
        }

    }


    PeerController(String pId,Socket s,int sessionType)
    {
        this.socket=s;
        this.sessionType=sessionType;
        this.currentpId=pId;
        try
        {
            ip=s.getInputStream();
            op=s.getOutputStream();
        }
        catch (IOException e) {
            P2P.l.showLog(this.currentpId+" error occurred when trying to get Data.");
        }
    }
    PeerController(String host,int port,int sessionType,String pId) throws IOException
    {
        this.sessionType=sessionType;
        try
        {
            this.currentpId=pId;
            this.socket=new Socket(host,port);
        }
        catch (Exception ex)
        {
            P2P.l.showLog("Error occurred when trying to open a connection with peer "+pId);
        }
        try
        {
            ip=socket.getInputStream();
            op=socket.getOutputStream();
        }
        catch (IOException e) {
            P2P.l.showLog(this.currentpId+" error occurred when trying to get Data.");
        }
    }
    public  boolean connectWithPeer()
    {
        try
        {
            op.write(Handshake.handShakeToArray(new Handshake(Integer.parseInt(this.currentpId))));
        }
        catch (Exception ex)
        {
            P2P.l.showLog("Sending HandShake Failed.....");
            return true;
        }
        return false;
    }



}
