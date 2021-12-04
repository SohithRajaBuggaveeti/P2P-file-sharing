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
            ip=this.socket.getInputStream();
            op=this.socket.getOutputStream();
        }
        catch (IOException e) {
            P2P.l.showLog(this.currentpId+" error occurred when trying to get Data.");
        }
    }



}
