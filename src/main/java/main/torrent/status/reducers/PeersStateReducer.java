package main.torrent.status.reducers;

import main.torrent.status.Action;
import main.torrent.status.state.tree.PeersState;
import main.torrent.status.state.tree.TorrentStatusState;

import java.util.function.Supplier;

public class PeersStateReducer {
    public static Supplier<PeersState> defaultPeersStateSupplier = () ->
            PeersState.PeersStateBuilder.builder()
                    .setStartedListeningToIncomingPeersInProgress(false)
                    .setStartedListeningToIncomingPeersWindUp(false)
                    .setPauseListeningToIncomingPeersInProgress(false)
                    .setPauseListeningToIncomingPeersWindUp(false)
                    .setResumeListeningToIncomingPeersInProgress(false)
                    .setResumeListeningToIncomingPeersWindUp(false)
                    .setStartedSearchingPeersInProgress(false)
                    .setStartedSearchingPeersWindUp(false)
                    .setPauseSearchingPeersInProgress(false)
                    .setPauseSearchingPeersWindUp(false)
                    .setResumeSearchingPeersInProgress(false)
                    .setResumeSearchingPeersWindUp(false)
                    .build();

    public PeersState reducer(TorrentStatusState oldState, Action action) {
        return oldState.getPeersState();
    }
}
