package NetWork;
import java.io.*;
import java.net.Socket;

import static NetWork.ActualMsg.*;
import static NetWork.ConstantMethod.*;
import static NetWork.ManageFile.*;

/**
 * Created by zhupd on 2/18/2017.
 */
public class peerProcess  {

    Config config;
    PeerState peerState=new PeerState();
    ManageFile fileManager;
    HandShakeMsg handshake;
    int myID;
    int guestID;

    final byte[] chockMsg = {0,0,0,1,0};
    final byte[] unchockMsg = {0,0,0,1,1};
    final byte[] interestedMsg = {0,0,0,1,2};
    final byte[] notInterestedMsg = {0,0,0,1,3};

    byte[] haveMsg = {0,0,0,5,4};
    byte[] requstMsg = {0,0,0,5,6};


    /**
     * inputstream, read by fixed byte, first read message length,
     * then message type
     * then payload(defined by length)
     *
     */
    void run() throws IOException{
        config = new Config();
        PeerInfo peer = config.peerInfo.get(guestID);
        //
        fileManager = new ManageFile(config, myID);
        //read input stream
        Socket socket = new Socket(peer.IP, peer.port);

        InputStream in = new BufferedInputStream(socket.getInputStream());
        OutputStream out = new BufferedOutputStream(socket.getOutputStream());

        handshake = new HandShakeMsg(myID);
        handshake.sendMsg(out);

        //send bitfield message
        //TODO

        while (true) {
            byte[] msgLength = new byte[4];
            byte[] msgType=new byte[1];
            in.read(msgLength);
            in.read(msgType);

            //if()
        }


    }

    /**
     * according to different type of the Actual message
     * handle the message
     * @param msg
     * @param out
     * @throws IOException
     */
    void receive(ActualMsg msg, OutputStream out) throws IOException {
        msgType type=msg.getType();
        switch (type) {
            case HAVE: {
                if(peerState.stateMap.get(guestID).isInterested(msg.getIndex())) {
                    //if I don't have this piece
                    //add the index of this piece into the interest list
                    //send interested message
                    peerState.stateMap.get(guestID).addInterest(msg.getIndex());
                    out.write(interestedMsg);
                }
                peerState.stateMap.get(guestID).updateBitField(msg);
                if(!peerState.stateMap.get(myID).compareBitfield(peerState.stateMap.get(guestID).getBitField())) {
                    //if bitfield has been updated, and I still not interested in any piece
                    //send not interested message to ALL not interest peers
                    //TODO
                    out.write(notInterestedMsg);
                }
            }

            break;

            case BITFIELD: {
                //we could get all bitfield info when reading the PeerInfo.cfg
                //so we simply ignore this message
            }

            break;

            case INTERESTED: {
                //set interested to true
                peerState.stateMap.get(guestID).setInterested(true);
            }

            break;

            case NOTINTERESTED: {
                //set interested to false
                peerState.stateMap.get(guestID).setInterested(false);
            }

            break;

            case CHOKE: {
                //set choke to true
                peerState.stateMap.get(guestID).setChoke(true);
            }

            break;

            case UNCHOKE: {
                //set choke to false
                peerState.stateMap.get(guestID).setChoke(false);
                //randomly select an index from interest list
                //send a request Message
                int index = peerState.stateMap.get(guestID).randomSelectIndex();
                out.write(mergeBytes(requstMsg, intToBytes(index)));
            }

            break;

            case REQUEST: {
                //request message
                //fetch the index from message
                //send the piece the peer want
                int index = msg.getIndex();
                out.write(writePieceMsg(intToBytes(index)));
            }

            break;

            case PIECE: {
                //read message
                readPieceMsg(msg);
                //update my bitfield
                peerState.stateMap.get(myID).updateBitField(msg);
                //send HAVE message to ALL peers
                //TODO
                out.write(mergeBytes(haveMsg, intToBytes(msg.getIndex())));

            }

            break;

        }//end switch
    }

}

