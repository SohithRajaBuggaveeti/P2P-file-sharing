public class RemotePeerInfo {
    public String peerId;
    public String peerAddress;
    public String peerPort;

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public String getPeerAddress() {
        return peerAddress;
    }

    public void setPeerAddress(String peerAddress) {
        this.peerAddress = peerAddress;
    }

    public String getPeerPort() {
        return peerPort;
    }

    public void setPeerPort(String peerPort) {
        this.peerPort = peerPort;
    }

    public boolean isHasFile() {
        return hasFile;
    }

    public void setHasFile(boolean hasFile) {
        this.hasFile = hasFile;
    }

    public int getPeerPos() {
        return peerPos;
    }

    public void setPeerPos(int peerPos) {
        this.peerPos = peerPos;
    }

    public boolean isFirst() {
        return isFirst;
    }

    public void setFirst(boolean first) {
        isFirst = first;
    }

    public boolean hasFile;
    public int peerPos;
    public boolean isFirst;
    public RemotePeerInfo(String pId, String pAddress, String pPort,boolean hFile,int i) {
        peerId = pId;
        peerAddress = pAddress;
        peerPort = pPort;
        hasFile=hFile;
        peerPos=i;
        if(hasFile)
        {
            isFirst=true;
        }
    }
}