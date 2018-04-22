package main.peer;

import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerCurrentStatus {
    private AtomicBoolean isInterestedInMe;
    private AtomicBoolean amIInterestedInHim;
    private AtomicBoolean isHeChokingMe;
    private AtomicBoolean amIChokingHim;
    private AtomicBoolean amIDownloadingFromHim;
    private AtomicBoolean amIUploadingToHim;
    private BitSet piecesStatus;
    private Flux<PeerCurrentStatusType> peerCurrentStatusTypeFlux;
    private FluxSink<PeerCurrentStatusType> peerCurrentStatusTypeFluxSink;

    private Flux<Boolean> amIDownloadingFromHimFlux;
    private Flux<Boolean> amIUploadingToHimFlux;

    public PeerCurrentStatus(int piecesAmount) {
        this.isInterestedInMe = new AtomicBoolean(false);
        this.amIInterestedInHim = new AtomicBoolean(false);
        this.isHeChokingMe = new AtomicBoolean(false);
        this.amIChokingHim = new AtomicBoolean(false);
        this.amIDownloadingFromHim = new AtomicBoolean(false);
        this.amIUploadingToHim = new AtomicBoolean(false);
        this.piecesStatus = new BitSet(piecesAmount);

        this.peerCurrentStatusTypeFlux = Flux.<PeerCurrentStatusType>create(sink -> {
            this.peerCurrentStatusTypeFluxSink = sink;
        })
                .publish();

        ((ConnectableFlux<PeerCurrentStatusType>) this.peerCurrentStatusTypeFlux).connect();

        this.amIDownloadingFromHimFlux = this.peerCurrentStatusTypeFlux
                .filter(peerCurrentStatusType ->
                        peerCurrentStatusType.equals(PeerCurrentStatusType.I_AM_NOT_DOWNLOAD_FROM_HIM) ||
                                peerCurrentStatusType.equals(PeerCurrentStatusType.I_AM_DOWNLOAD_FROM_HIM))
                .map(peerCurrentStatusType -> peerCurrentStatusType.equals(PeerCurrentStatusType.I_AM_DOWNLOAD_FROM_HIM))
                .replay(1);

        ((ConnectableFlux<Boolean>) this.amIDownloadingFromHimFlux).connect();

        setAmIDownloadingFromHim(false);

        this.amIUploadingToHimFlux = this.peerCurrentStatusTypeFlux
                .filter(peerCurrentStatusType ->
                        peerCurrentStatusType.equals(PeerCurrentStatusType.I_AM_NOT_UPLOAD_TO_HIM) ||
                                peerCurrentStatusType.equals(PeerCurrentStatusType.I_UPLOAD_TO_HIM))
                .map(peerCurrentStatusType -> peerCurrentStatusType.equals(PeerCurrentStatusType.I_UPLOAD_TO_HIM))
                .replay(1);

        ((ConnectableFlux<Boolean>) this.amIUploadingToHimFlux).connect();

        setAmIUploadingToHim(false);
    }

    public boolean getIsInterestedInMe() {
        return isInterestedInMe.get();
    }

    public void setIsHeInterestedInMe(boolean isHeInterestedInMe) {
        this.isInterestedInMe.set(isHeInterestedInMe);
        if (isHeInterestedInMe)
            this.peerCurrentStatusTypeFluxSink.next(PeerCurrentStatusType.HE_INTERESTED_IN_ME);
        else
            this.peerCurrentStatusTypeFluxSink.next(PeerCurrentStatusType.HE_IS_NOT_INTERESTED_IN_ME);
    }

    public boolean getAmIInterestedInHim() {
        return amIInterestedInHim.get();
    }

    public void setAmIInterestedInHim(boolean amIInterestedInHim) {
        this.amIInterestedInHim.set(amIInterestedInHim);
        if (amIInterestedInHim)
            this.peerCurrentStatusTypeFluxSink.next(PeerCurrentStatusType.I_INTERESTED_IN_HIM);
        else
            this.peerCurrentStatusTypeFluxSink.next(PeerCurrentStatusType.I_AM_NOT_INTERESTED_IN_HIM);
    }

    public boolean getIsHeChokingMe() {
        return isHeChokingMe.get();
    }

    public void setIsHeChokingMe(boolean isHeChokingMe) {
        this.isHeChokingMe.set(isHeChokingMe);
        if (isHeChokingMe)
            this.peerCurrentStatusTypeFluxSink.next(PeerCurrentStatusType.HE_CHOKE_ME);
        else
            this.peerCurrentStatusTypeFluxSink.next(PeerCurrentStatusType.HE_IS_NOT_CHOKE_ME);
    }

    public boolean getAmIChokingHim() {
        return amIChokingHim.get();
    }

    public void setAmIChokingHim(boolean amIChokingHim) {
        this.amIChokingHim.set(amIChokingHim);
        if (amIChokingHim)
            this.peerCurrentStatusTypeFluxSink.next(PeerCurrentStatusType.I_CHOKE_HIM);
        else
            this.peerCurrentStatusTypeFluxSink.next(PeerCurrentStatusType.I_AM_NOT_CHOKE_HIM);
    }

    public boolean getAmIDownloadingFromHim() {
        return amIDownloadingFromHim.get();
    }

    public void setAmIDownloadingFromHim(boolean amIDownloadingFromHim) {
        this.amIDownloadingFromHim.set(amIDownloadingFromHim);
        if (amIDownloadingFromHim)
            this.peerCurrentStatusTypeFluxSink.next(PeerCurrentStatusType.I_AM_DOWNLOAD_FROM_HIM);
        else
            this.peerCurrentStatusTypeFluxSink.next(PeerCurrentStatusType.I_AM_NOT_DOWNLOAD_FROM_HIM);
    }

    public boolean getAmIUploadingToHim() {
        return amIUploadingToHim.get();
    }

    public void setAmIUploadingToHim(boolean amIUploadingToHim) {
        this.amIUploadingToHim.set(amIUploadingToHim);
        if (amIUploadingToHim)
            this.peerCurrentStatusTypeFluxSink.next(PeerCurrentStatusType.I_UPLOAD_TO_HIM);
        else
            this.peerCurrentStatusTypeFluxSink.next(PeerCurrentStatusType.I_AM_NOT_UPLOAD_TO_HIM);
    }

    public BitSet getPiecesStatus() {
        return piecesStatus;
    }

    public synchronized void updatePiecesStatus(BitSet piecesStatus) {
        this.piecesStatus.or(piecesStatus);
        this.peerCurrentStatusTypeFluxSink.next(PeerCurrentStatusType.PIECES_STATUS_CHANGE);
    }

    public synchronized  void updatePiecesStatus(int pieceIndex) {
        this.piecesStatus.set(pieceIndex);
        this.peerCurrentStatusTypeFluxSink.next(PeerCurrentStatusType.PIECES_STATUS_CHANGE);
    }

    public Flux<PeerCurrentStatusType> getPeerCurrentStatusTypeFlux() {
        return peerCurrentStatusTypeFlux;
    }

    public Flux<Boolean> getAmIDownloadingFromHimFlux() {
        return amIDownloadingFromHimFlux;
    }

    public Flux<Boolean> getAmIUploadingToHimFlux() {
        return amIUploadingToHimFlux;
    }
}
