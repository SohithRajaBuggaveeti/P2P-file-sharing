import java.io.RandomAccessFile;

public class DataController implements Runnable
{
    public static String peerId;
    RandomAccessFile randomAccessFile;
    DataController(String peerId)
    {
        DataController.peerId=peerId;
    }
    public void run()
    {
    String currentPeerId;
    String dataType;
    DataParams dp;
    MessageData m;
    while(true)
    {
        dp = P2P.removeDataFromQueue();
        while(dp == null){
            Thread.currentThread();
            try {
                Thread.sleep(600);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
            dp = P2P.removeDataFromQueue();
        }
        m = dp.getM();
        dataType = m.getDataType();
        currentPeerId = dp.getpId();
        int cs = P2P.remotePeerInfoHashMap.get(currentPeerId).state;
        if(dataType.equals(Constants.have) && cs!=14)
        {
            P2P.l.showLog(P2P.peerId+" peer got a have message from peer "+ currentPeerId);
            if(comparePayLoadData(m,currentPeerId))
            {
               // sendInterestedMessage(P2P.pee)
            }
        }

    }
    }

    private boolean comparePayLoadData(MessageData m, String currentPeerId)
    {
        P2P.remotePeerInfoHashMap.get(currentPeerId).payloadData=PayLoadData.decodeData(m.getPayLoadArray());
        return P2P.currentDataPayLoad.comparePayLoadData(PayLoadData.decodeData(m.getPayLoadArray()))   ;
    }

}
