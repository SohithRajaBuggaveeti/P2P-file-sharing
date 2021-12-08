import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;


public class MessageProcessor implements Runnable
{
    private static String currentPeerID = null;
    RandomAccessFile randomAccessFile;

    public MessageProcessor(String currentPeerID) {
        MessageProcessor.currentPeerID = currentPeerID;
    }

    public void run() {
        MessageData message;
        DataParams messageAttributes;
        String messageType;
        String runningPeerID;

        while(true)
        {
            messageAttributes  = P2P.removeDataFromQueue();
            while(messageAttributes == null) {
                Thread.currentThread();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                messageAttributes  = P2P.removeDataFromQueue();
            }

            message = messageAttributes.getM();

            messageType = message.getDataType();
            runningPeerID = messageAttributes.getpId();
            int currentState = P2P.remotePeerInfoHashMap.get(runningPeerID).state;
            if(messageType.equals(""+Constants.have) && currentState != 14)
            {
                P2P.l.showLog(P2P.peerId+" got HAVE message from Peer "+ runningPeerID);
                if(compareBitfield(message, runningPeerID)) {
                    sendInterestedPayload(P2P.peerData.get(runningPeerID), runningPeerID);
                    P2P.remotePeerInfoHashMap.get(runningPeerID).state = 9;
                }
                else {
                    sendNotInterestedPayload(P2P.peerData.get(runningPeerID), runningPeerID);
                    P2P.remotePeerInfoHashMap.get(runningPeerID).state = 13;
                }
            }
            else {
                switch (currentState)
                {
                    case 2:
                        if (messageType.equals(""+Constants.bitField)) {
                            P2P.l.showLog(P2P.peerId+" got BITFIELD message from Peer "+ runningPeerID);
                            sendBitFieldPayload(P2P.peerData.get(runningPeerID), runningPeerID);
                            P2P.remotePeerInfoHashMap.get(runningPeerID).state = 3;
                        }
                        break;

                    case 3:
                        if (messageType.equals(""+Constants.notInterested)) {
                            P2P.l.showLog( P2P.peerId+" got NOT INTERESTED message from Peer "+runningPeerID);
                            P2P.remotePeerInfoHashMap.get(runningPeerID).isInterested = 0;
                            P2P.remotePeerInfoHashMap.get(runningPeerID).state = 5;
                            P2P.remotePeerInfoHashMap.get(runningPeerID).isHandShake = 1;
                        }
                        else if (messageType.equals(""+Constants.intersted)) {
                            P2P.l.showLog(P2P.peerId+" got a REQUEST message to Peer "+ runningPeerID);
                            P2P.l.showLog( P2P.peerId+" got INTERESTED message from Peer "+runningPeerID);
                            P2P.remotePeerInfoHashMap.get(runningPeerID).isInterested = 1;
                            P2P.remotePeerInfoHashMap.get(runningPeerID).isHandShake = 1;

                            if(!P2P.preferredNeighboursTable.containsKey(runningPeerID) && !P2P.unchokedNeighboursTable.containsKey(runningPeerID)) {
                                sendChokePayload(P2P.peerData.get(runningPeerID), runningPeerID);
                                P2P.remotePeerInfoHashMap.get(runningPeerID).isChoked = 1;
                                P2P.remotePeerInfoHashMap.get(runningPeerID).state  = 6;
                            }
                            else {
                                P2P.remotePeerInfoHashMap.get(runningPeerID).isChoked = 0;
                                sendUnChokePayload(P2P.peerData.get(runningPeerID), runningPeerID);
                                P2P.remotePeerInfoHashMap.get(runningPeerID).state = 4 ;
                            }
                        }
                        break;

                    case 4:
                        if (messageType.equals(""+Constants.request)) {
                            transferPiece(P2P.peerData.get(runningPeerID), message, runningPeerID);
                            // CHOKE/UNCHOKE
                            if(!P2P.preferredNeighboursTable.containsKey(runningPeerID) && !P2P.unchokedNeighboursTable.containsKey(runningPeerID)) {
                                sendChokePayload(P2P.peerData.get(runningPeerID), runningPeerID);
                                P2P.remotePeerInfoHashMap.get(runningPeerID).isChoked = 1;
                                P2P.remotePeerInfoHashMap.get(runningPeerID).state = 6;
                            }
                        }
                        break;

                    case 8:
                        if (messageType.equals(""+Constants.bitField)) {
                            //INTERESTED/ NOT INTERESTED
                            if(compareBitfield(message,runningPeerID)) {
                                sendInterestedPayload(P2P.peerData.get(runningPeerID), runningPeerID);
                                P2P.remotePeerInfoHashMap.get(runningPeerID).state = 9;
                            }
                            else {
                                sendNotInterestedPayload(P2P.peerData.get(runningPeerID), runningPeerID);
                                P2P.remotePeerInfoHashMap.get(runningPeerID).state = 13;
                            }
                        }
                        break;

                    case 9:
                        if (messageType.equals(""+Constants.choke)) {
                            P2P.l.showLog( P2P.peerId+" got CHOKED by Peer "+runningPeerID);
                            P2P.remotePeerInfoHashMap.get(runningPeerID).state = 14;
                        }
                        else if (messageType.equals(""+Constants.unChoke)) {
                            P2P.l.showLog( P2P.peerId+" got CHOKED by Peer "+runningPeerID);
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            P2P.l.showLog( P2P.peerId+" got UNCHOKED by Peer "+runningPeerID);
                            int initialMismatch = P2P.currentDataPayLoad.fetchFirstBitField(
                                    P2P.remotePeerInfoHashMap.get(runningPeerID).payloadData);
                            if(initialMismatch != -1) {
                                sendRequest(P2P.peerData.get(runningPeerID), initialMismatch);
                                P2P.remotePeerInfoHashMap.get(runningPeerID).state = 11;
                                P2P.remotePeerInfoHashMap.get(runningPeerID).startTime = new Date();
                            }
                            else
                                P2P.remotePeerInfoHashMap.get(runningPeerID).state = 13;
                        }
                        break;

                    case 11:
                        if (messageType.equals(""+Constants.piece)) {
                            byte[] payloadArray = message.getPayLoadArray();
                            P2P.remotePeerInfoHashMap.get(runningPeerID).finishTime = new Date();
                            long timeLapse = P2P.remotePeerInfoHashMap.get(runningPeerID).finishTime.getTime() -
                                    P2P.remotePeerInfoHashMap.get(runningPeerID).startTime.getTime() ;
                            P2P.remotePeerInfoHashMap.get(runningPeerID).streamRate= ((double)(payloadArray.length + Constants.sizeOfMessage + Constants.typeOfMessage) / (double)timeLapse) * 100;
                            Payloadpiece p = Payloadpiece.convertToPiece(payloadArray);
                            P2P.currentDataPayLoad.updatePayLoad(p,""+runningPeerID);
                            int fetchPieceIndex = P2P.currentDataPayLoad.fetchFirstBitField(
                                    P2P.remotePeerInfoHashMap.get(runningPeerID).payloadData);
                            if(fetchPieceIndex != -1) {
                                sendRequest(P2P.peerData.get(runningPeerID), fetchPieceIndex);
                                P2P.remotePeerInfoHashMap.get(runningPeerID).state  = 11;
                                P2P.remotePeerInfoHashMap.get(runningPeerID).startTime = new Date();
                            }
                            else
                                P2P.remotePeerInfoHashMap.get(runningPeerID).state = 13;
                            P2P.nextReadPeerInfo();

                            Enumeration<String> keys = Collections.enumeration(P2P.remotePeerInfoHashMap.keySet());
                            while(keys.hasMoreElements())
                            {
                                String nextElement = keys.nextElement();
                                RemotePeerInfo pref = P2P.remotePeerInfoHashMap.get(nextElement);
                                if(nextElement.equals(P2P.peerId))continue;
                                if (pref.isCompleted == 0 && pref.isChoked == 0 && pref.isHandShake == 1) {
                                    sendHavePayload(P2P.peerData.get(nextElement), nextElement);
                                    P2P.remotePeerInfoHashMap.get(nextElement).state = 3;
                                }
                            }
                        }
                        else if (messageType.equals(""+Constants.choke)) {
                            P2P.l.showLog( P2P.peerId+" got CHOKED by Peer "+runningPeerID);
                            P2P.remotePeerInfoHashMap.get(runningPeerID).state = 14;
                        }
                        break;

                    case 14:
                        if (messageType.equals(""+Constants.have)) {
                            if(compareBitfield(message,runningPeerID)) {
                                sendInterestedPayload(P2P.peerData.get(runningPeerID), runningPeerID);
                                P2P.remotePeerInfoHashMap.get(runningPeerID).state = 9;
                            }
                            else {
                                sendNotInterestedPayload(P2P.peerData.get(runningPeerID), runningPeerID);
                                P2P.remotePeerInfoHashMap.get(runningPeerID).state = 13;
                            }
                        }
                        else if (messageType.equals(""+Constants.unChoke)) {
                            P2P.l.showLog( P2P.peerId+" got CHOKED by Peer "+runningPeerID);
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            P2P.l.showLog( P2P.peerId+" got UNCHOKED by Peer "+runningPeerID);
                            P2P.remotePeerInfoHashMap.get(runningPeerID).state = 14;
                        }
                        break;
                }
            }

        }
    }

    private void sendRequest(Socket serverSocket, int pieceNumber) {
        byte[] pieceArray = new byte[Constants.maxPieceLength];
        for (int index = 0; index < Constants.maxPieceLength; index++)
            pieceArray[index] = 0;

        byte[] pieceIndexArray = Constants.convertIntToByte(pieceNumber);
        System.arraycopy(pieceIndexArray, 0, pieceArray, 0,
                pieceIndexArray.length);
        MessageData message = new MessageData(Constants.request, pieceArray);
        byte[] messageArray = MessageData.convertDataToByteArray(message);
        sendMessage(serverSocket, messageArray);
    }

    private void transferPiece(Socket serverSocket, MessageData requestMessage, String remotePeerID)
    {
        byte[] bytePieceIndex = requestMessage.getPayLoadArray();
        int pieceIndex = Constants.convertByteArrayToInt(bytePieceIndex,0);
        byte[] readBytes = new byte[Constants.pieceSize];
        int numberOfBytesRead = 0;
        File currentFile = new File(P2P.peerId, Constants.fileName);

        P2P.l.showLog(P2P.peerId+" is sending PIECE "+pieceIndex+" to Peer "+ remotePeerID);
        try {
            randomAccessFile = new RandomAccessFile(currentFile,"r");
            randomAccessFile.seek((long) pieceIndex *Constants.pieceSize);
            numberOfBytesRead = randomAccessFile.read(readBytes, 0, Constants.pieceSize);
        }
        catch (IOException ioException) {
            P2P.l.showLog(P2P.peerId+" has error in reading the file: "+ioException.toString());
        }
        if( numberOfBytesRead == 0)
            P2P.l.showLog(P2P.peerId+" Zero bytes read from the file");
        else if (numberOfBytesRead < 0)
            P2P.l.showLog(P2P.peerId+" File could not be read properly.");

        byte[] bytesBuffer = new byte[numberOfBytesRead + Constants.maxPieceLength];
        System.arraycopy(bytePieceIndex, 0, bytesBuffer, 0, Constants.maxPieceLength);
        System.arraycopy(readBytes, 0, bytesBuffer, Constants.maxPieceLength, numberOfBytesRead);

        MessageData sendMessage = new MessageData(Constants.piece, bytesBuffer);
        byte[] messageToByteArray =  MessageData.convertDataToByteArray(sendMessage);
        sendMessage(serverSocket, messageToByteArray);
        try{randomAccessFile.close();}
        catch(Exception ignored){}
    }

    private boolean compareBitfield(MessageData message, String remotePeerID) {
        PayLoadData payloadData = PayLoadData.decodeData(message.getPayLoadArray());
        P2P.remotePeerInfoHashMap.get(remotePeerID).payloadData = payloadData;
        return P2P.currentDataPayLoad.comparePayLoadData(payloadData);
    }

    private void sendNotInterestedPayload(Socket serverSocket, String remotePeerID) {
        P2P.l.showLog(P2P.peerId+" sent a NOT INTERESTED message to Peer "+ remotePeerID);
        MessageData message =  new MessageData(Constants.notInterested);
        byte[] messageToByteArray = MessageData.convertDataToByteArray(message);
        sendMessage(serverSocket,messageToByteArray);
    }

    private void sendInterestedPayload(Socket serverSocket, String remotePeerID) {
        P2P.l.showLog(P2P.peerId+" sent a REQUEST message to Peer "+ remotePeerID);
        P2P.l.showLog(P2P.peerId+" sent a INTERESTED message to Peer "+ remotePeerID);
        MessageData message =  new MessageData(Constants.intersted);
        byte[] msgByte = MessageData.convertDataToByteArray(message);
        sendMessage(serverSocket,msgByte);
    }

    private void sendUnChokePayload(Socket serverSocket, String remotePeerID) {
        P2P.l.showLog(P2P.peerId+" sent a UNCHOKE message to Peer "+ remotePeerID);
        MessageData message = new MessageData(Constants.unChoke);
        byte[] messageToByteArray = MessageData.convertDataToByteArray(message);
        sendMessage(serverSocket,messageToByteArray);
    }

    private void sendChokePayload(Socket serverSocket, String remotePeerID) {
        P2P.l.showLog(P2P.peerId+" sent a CHOKE message to Peer "+ remotePeerID);
        MessageData message = new MessageData(Constants.choke);
        byte[] messageToByteArray = MessageData.convertDataToByteArray(message);
        sendMessage(serverSocket,messageToByteArray);
    }

    private void sendBitFieldPayload(Socket serverSocket, String remotePeerID) {
        P2P.l.showLog(P2P.peerId+" sent a BITFIELD message to Peer "+ remotePeerID);
        byte[] encodedBitField = P2P.currentDataPayLoad.encodeData();
        MessageData message = new MessageData(+Constants.bitField, encodedBitField);
        sendMessage(serverSocket, MessageData.convertDataToByteArray(message));
    }

    private void sendHavePayload(Socket serverSocket, String remotePeerID) {
        P2P.l.showLog(P2P.peerId+" sent a HAVE message to Peer "+ remotePeerID);
        byte[] encodedBitField = P2P.currentDataPayLoad.encodeData();
        MessageData message = new MessageData(Constants.have, encodedBitField);
        sendMessage(serverSocket, MessageData.convertDataToByteArray(message));
    }

    private void sendMessage(Socket serverSocket, byte[] encodedBitField) {
        try {
            OutputStream outputStream = serverSocket.getOutputStream();
            outputStream.write(encodedBitField);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}
