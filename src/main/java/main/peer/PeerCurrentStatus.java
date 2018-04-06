package main.peer;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerCurrentStatus {
    private AtomicBoolean amICurrentlyDownloadingFromHim;
    private AtomicBoolean isInterestedInMe;
    private AtomicBoolean amIInterestedInHim;
    private AtomicBoolean isHeChokingMe;
    private AtomicBoolean amIChokingHim;
    private BitSet piecesStatus;

    public PeerCurrentStatus(int piecesAmount) {
        this.amICurrentlyDownloadingFromHim = new AtomicBoolean(false);
        this.isInterestedInMe = new AtomicBoolean(false);
        this.amIInterestedInHim = new AtomicBoolean(false);
        this.isHeChokingMe = new AtomicBoolean(false);
        this.amIChokingHim = new AtomicBoolean(false);
        this.piecesStatus = new BitSet(piecesAmount);
    }

    public boolean getIsInterestedInMe() {
        return isInterestedInMe.get();
    }

    public void setIsInterestedInMe(boolean isInterestedInMe) {
        this.isInterestedInMe.set(isInterestedInMe);
    }

    public boolean getAmIInterestedInHim() {
        return amIInterestedInHim.get();
    }

    public void setAmIInterestedInHim(boolean amIInterestedInHim) {
        this.amIInterestedInHim.set(amIInterestedInHim);
    }

    public boolean getIsHeChokingMe() {
        return isHeChokingMe.get();
    }

    public void setIsHeChokingMe(boolean isHeChokingMe) {
        this.isHeChokingMe.set(isHeChokingMe);
    }

    public boolean getAmIChokingHim() {
        return amIChokingHim.get();
    }

    public void setAmIChokingHim(boolean amIChokingHim) {
        this.amIChokingHim.set(amIChokingHim);
    }

    public boolean getAmICurrentlyDownloadingFromHim() {
        return amICurrentlyDownloadingFromHim.get();
    }

    public void setAmICurrentlyDownloadingFromHim(boolean amICurrentlyDownloadingFromHim) {
        this.amICurrentlyDownloadingFromHim.set(amICurrentlyDownloadingFromHim);
    }

    public BitSet getPiecesStatus() {
        return piecesStatus;
    }

    public void updatePiecesStatus(BitSet piecesStatus) {
        this.piecesStatus.or(piecesStatus);
    }

    public void updatePiecesStatus(int pieceIndex) {
        this.piecesStatus.set(pieceIndex);
    }
}
